package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class AiJournalAnalysisService {
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.1";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RiskDetectionService riskDetectionService = new RiskDetectionService();
    private final Properties properties = IntegrationConfigService.loadProperties("ai.properties");

    public AiResult generateAdvice(User patient, JournalAnalyseRow row) {
        String prompt = """
                Analyse ce journal patient et donne un conseil professionnel, empathique et actionnable.
                Reponds en francais avec:
                1. Synthese emotionnelle courte
                2. Conseil immediat
                3. Plan concret en 3 etapes
                4. Message de prudence si le contenu indique un risque

                Patient: %s
                Humeur declaree: %s
                Journal: %s
                """.formatted(patientLabel(patient), safe(row.getHumeur()), safe(row.getContenuComplet()));
        return askWithFallback("Conseil clinique bref pour journal patient", prompt, localAdvice(row));
    }

    public AiResult detectRisk(User patient, JournalAnalyseRow row) {
        RiskDetectionService.RiskAssessment localRisk = riskDetectionService.assess(row.getContenuComplet());
        String prompt = """
                Fais une detection de risque clinique sur ce journal. Reponds en francais avec:
                - Niveau: Aucun, Modere, Eleve ou Critique
                - Indices detectes
                - Risques possibles
                - Actions recommandees pour le psychologue
                - Urgence: faible, moyenne, haute

                Ne pose pas de diagnostic. Si idees suicidaires ou danger immediat, recommande une prise en charge urgente.

                Patient: %s
                Humeur declaree: %s
                Detection locale preliminaire: %s, score %d, %s
                Journal: %s
                """.formatted(
                patientLabel(patient),
                safe(row.getHumeur()),
                localRisk.level(),
                localRisk.score(),
                localRisk.summary(),
                safe(row.getContenuComplet())
        );
        return askWithFallback("Detection de risque clinique", prompt, localDetection(row, localRisk));
    }

    private AiResult askWithFallback(String systemPrompt, String userPrompt, String localFallback) {
        String groqKey = read("GROQ_API_KEY", read("GROK_API_KEY", ""));
        if (!groqKey.isBlank()) {
            try {
                return new AiResult(callGroq(groqKey, systemPrompt, userPrompt), "Groq");
            } catch (Exception e) {
                System.err.println("[AI] Groq indisponible, fallback Ollama: " + e.getMessage());
            }
        }

        try {
            return new AiResult(callOllama(systemPrompt + "\n\n" + userPrompt), "Ollama");
        } catch (Exception e) {
            System.err.println("[AI] Ollama indisponible, fallback local: " + e.getMessage());
            return new AiResult(localFallback, "Analyse locale");
        }
    }

    private String callGroq(String apiKey, String systemPrompt, String userPrompt) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", read("GROQ_MODEL", DEFAULT_GROQ_MODEL));
        root.put("temperature", 0.35);
        root.put("max_tokens", 900);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("Reponse Groq vide");
        }
        return content.asText().trim();
    }

    private String callOllama(String prompt) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", read("OLLAMA_MODEL", DEFAULT_OLLAMA_MODEL));
        root.put("prompt", prompt);
        root.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(read("OLLAMA_URL", DEFAULT_OLLAMA_URL)))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode text = mapper.readTree(response.body()).path("response");
        if (text.isMissingNode() || text.asText().isBlank()) {
            throw new IllegalStateException("Reponse Ollama vide");
        }
        return text.asText().trim();
    }

    private String localAdvice(JournalAnalyseRow row) {
        RiskDetectionService.RiskAssessment risk = riskDetectionService.assess(row.getContenuComplet());
        if (risk.isCritical()) {
            return """
                    Synthese: le journal contient des signaux de detresse importants.
                    Conseil immediat: ne pas rester seul et contacter rapidement un professionnel ou un service d'urgence local.
                    Plan:
                    1. Prevenir une personne de confiance maintenant.
                    2. Eloigner les moyens de passage a l'acte.
                    3. Programmer une prise en charge clinique urgente.
                    Prudence: cette analyse locale ne remplace pas une evaluation clinique.
                    """;
        }
        return """
                Synthese: le journal indique un besoin d'accompagnement emotionnel.
                Conseil immediat: identifier l'emotion dominante et noter l'evenement declencheur.
                Plan:
                1. Respirer lentement pendant deux minutes.
                2. Ecrire une action simple et realisable aujourd'hui.
                3. Partager le ressenti avec un professionnel si la detresse persiste.
                """;
    }

    private String localDetection(JournalAnalyseRow row, RiskDetectionService.RiskAssessment risk) {
        return """
                Niveau: %s
                Score local: %d
                Indices detectes: %s
                Actions recommandees: verifier le contexte avec le patient, evaluer l'urgence, documenter la decision clinique.
                Urgence: %s
                """.formatted(
                risk.level(),
                risk.score(),
                risk.summary(),
                risk.isCritical() ? "haute" : risk.requiresAlert() ? "moyenne" : "faible"
        );
    }

    private String read(String key, String defaultValue) {
        return IntegrationConfigService.read(properties, key, defaultValue);
    }

    private String patientLabel(User user) {
        if (user == null) {
            return "Patient non renseigne";
        }
        return (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    public record AiResult(String text, String provider) {
    }
}
