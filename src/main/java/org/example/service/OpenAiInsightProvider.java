package org.example.service;

import org.example.entities.ProfilPsychologique;
import org.example.entities.RapportSessionJeu;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiInsightProvider implements AiInsightProvider {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public OpenAiInsightProvider(String apiKey) {
        this(apiKey, readConfig("OPENAI_MODEL", "gpt-4o-mini"));
    }

    public OpenAiInsightProvider(String apiKey, String model) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-4o-mini" : model.trim();
    }

    @Override
    public Optional<String> enrichAnalysis(ProfilPsychologique profilPsychologique, RapportSessionJeu rapportSessionJeu) {
        if (apiKey.isBlank()) {
            return Optional.empty();
        }

        String prompt = buildPrompt(profilPsychologique, rapportSessionJeu);
        String requestBody = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "Tu es un assistant clinique prudent. Donne uniquement une analyse complementaire concise pour un parent, sans diagnostic medical."},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.3
                }
                """.formatted(escapeJson(model), escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(readConfig("OPENAI_API_URL", "https://api.openai.com/v1/chat/completions")))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                System.out.println("OpenAI insight indisponible: HTTP " + response.statusCode());
                return Optional.empty();
            }
            return extractContent(response.body());
        } catch (InterruptedException e) {
            System.out.println("OpenAI insight indisponible: " + e.getMessage());
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException e) {
            System.out.println("OpenAI insight indisponible: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(ProfilPsychologique profilPsychologique, RapportSessionJeu rapportSessionJeu) {
        return "Analyse ce resume d'une session de jeu visuel pour enfant de moins de 5 ans. "
                + "Profil=" + profilPsychologique.getProfil()
                + ", scoreEmotionnel=" + profilPsychologique.getScoreEmotionnel()
                + ", sociabilite=" + profilPsychologique.getSociabilite()
                + ", timidite=" + profilPsychologique.getTimidite()
                + ", stress=" + profilPsychologique.getStress()
                + ", curiosite=" + profilPsychologique.getCuriosite()
                + ", tendance=" + profilPsychologique.getTendance()
                + ", anomalie=" + profilPsychologique.getAnomalie()
                + ", synthese=" + profilPsychologique.getSyntheseClinique()
                + ", resumeSession=" + rapportSessionJeu.getResumeSession()
                + ". Donne 3 phrases maximum: observation, risque potentiel a surveiller, conseil parent.";
    }

    private Optional<String> extractContent(String responseBody) {
        Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String content = matcher.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/");
        return Optional.of(content.trim());
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static String readConfig(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return defaultValue;
    }
}
