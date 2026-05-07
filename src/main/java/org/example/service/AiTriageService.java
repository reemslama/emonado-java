package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entities.AiTriageResult;
import org.example.entities.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AiTriageService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Properties config = IntegrationConfigService.loadProperties("openai.properties");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey = IntegrationConfigService.read(config, "OPENAI_API_KEY", "");
    private final String model = IntegrationConfigService.read(config, "OPENAI_TRIAGE_MODEL", "gpt-4.1");
    private final String apiBaseUrl = IntegrationConfigService.read(config, "OPENAI_API_BASE_URL", "https://api.openai.com/v1");
    private final boolean enabled = IntegrationConfigService.readBoolean(config, "OPENAI_TRIAGE_ENABLED", true);

    public boolean isConfigured() {
        return enabled && !apiKey.isBlank();
    }

    public AiTriageResult analyzeAppointmentRequest(String patientNotes, List<User> psychiatrists) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI n'est pas configure.");
        }
        if (patientNotes == null || patientNotes.isBlank()) {
            throw new IllegalArgumentException("Les notes patient sont obligatoires pour le triage IA.");
        }

        String psychiatristSummary = psychiatrists == null || psychiatrists.isEmpty()
                ? "Aucun psychologue disponible."
                : psychiatrists.stream()
                .map(user -> "- " + user.getPrenom() + " " + user.getNom() + " | specialite: " + fallback(user.getSpecialite(), "generaliste"))
                .collect(Collectors.joining("\n"));

        String instructions = """
                Tu es un assistant de triage pour une application de prise de rendez-vous psychologique.
                Analyse la demande patient et retourne UNIQUEMENT un JSON valide avec cette structure:
                {
                  "recommendedType": "consultation|suivi",
                  "recommendedSpeciality": "string",
                  "priorityLevel": "normal|sensible|urgent",
                  "rationale": "string",
                  "emergency": true,
                  "suggestedPsychologueName": "string",
                  "followUpQuestions": ["q1", "q2"],
                  "synthesizedNotes": "string"
                }
                Regles:
                - recommendedType vaut uniquement consultation ou suivi.
                - emergency=true si le texte suggere crise, violence, idee suicidaire, danger immediat ou urgence tres elevee.
                - suggestedPsychologueName doit correspondre a un nom de la liste fournie ou etre vide.
                - synthesizedNotes doit etre un resume clinique propre et exploitable dans un rendez-vous.
                """;

        String input = "Liste des psychologues disponibles:\n" + psychiatristSummary
                + "\n\nDemande patient:\n" + patientNotes;

        try {
            String payload = OBJECT_MAPPER.createObjectNode()
                    .put("model", model)
                    .put("instructions", instructions)
                    .set("input", OBJECT_MAPPER.createArrayNode().add(
                            OBJECT_MAPPER.createObjectNode()
                                    .put("role", "user")
                                    .put("type", "message")
                                    .set("content", OBJECT_MAPPER.createArrayNode().add(
                                            OBJECT_MAPPER.createObjectNode().put("type", "input_text").put("text", input)
                                    ))
                    ))
                    .toPrettyString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI HTTP " + response.statusCode() + " : " + response.body());
            }
            return parseTriageResult(response.body());
        } catch (IOException e) {
            throw new RuntimeException("Impossible de contacter OpenAI : " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Appel OpenAI interrompu.", e);
        }
    }

    private AiTriageResult parseTriageResult(String body) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        String jsonText = root.path("output_text").asText("");
        if (jsonText.isBlank()) {
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.path("content");
                    if (content.isArray()) {
                        for (JsonNode contentItem : content) {
                            if (contentItem.hasNonNull("text")) {
                                jsonText = contentItem.get("text").asText();
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (jsonText.isBlank()) {
            throw new IOException("Reponse OpenAI vide ou non exploitable.");
        }
        JsonNode resultNode = OBJECT_MAPPER.readTree(extractJsonObject(jsonText));
        AiTriageResult result = new AiTriageResult();
        result.setRecommendedType(resultNode.path("recommendedType").asText(""));
        result.setRecommendedSpeciality(resultNode.path("recommendedSpeciality").asText(""));
        result.setPriorityLevel(resultNode.path("priorityLevel").asText(""));
        result.setRationale(resultNode.path("rationale").asText(""));
        result.setEmergency(resultNode.path("emergency").asBoolean(false));
        result.setSuggestedPsychologueName(resultNode.path("suggestedPsychologueName").asText(""));
        List<String> questions = new ArrayList<>();
        if (resultNode.path("followUpQuestions").isArray()) {
            for (JsonNode node : resultNode.path("followUpQuestions")) {
                questions.add(node.asText());
            }
        }
        result.setFollowUpQuestions(questions);
        result.setSynthesizedNotes(resultNode.path("synthesizedNotes").asText(""));
        return result;
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
