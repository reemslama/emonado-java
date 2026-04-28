package org.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Analyses cliniques (journaux) : priorite a <b>Groq</b> (cle {@code GROQ_API_KEY}, compte gratuit sur https://console.groq.com ),
 * puis repli sur <b>Gemini</b> si {@code GEMINI_API_KEY} est defini.
 */
public final class ClinicalLlmService {

    private static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
    /** Modele rapide et genereusement disponible sur le free tier Groq */
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static String readGroqKey() {
        String p = System.getProperty("GROQ_API_KEY");
        if (p != null && !p.isBlank()) {
            return p.trim();
        }
        String e = System.getenv("GROQ_API_KEY");
        return e != null && !e.isBlank() ? e.trim() : "";
    }

    public String completeClinical(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt vide.");
        }

        String groqKey = readGroqKey();
        if (!groqKey.isEmpty()) {
            try {
                return callGroq(groqKey, prompt);
            } catch (RuntimeException ex) {
                if (!hasGeminiKeyConfigured()) {
                    throw new RuntimeException(
                            "Groq a echoue : " + ex.getMessage()
                                    + ". Corrigez GROQ_API_KEY ou configurez GEMINI_API_KEY en secours.",
                            ex);
                }
                try {
                    return new GeminiChatService().generateClinicalViaGemini(prompt);
                } catch (RuntimeException geminiEx) {
                    throw new RuntimeException(
                            "Groq : " + ex.getMessage() + " | Gemini : " + geminiEx.getMessage(),
                            geminiEx);
                }
            }
        }

        return new GeminiChatService().generateClinicalViaGemini(prompt);
    }

    private static boolean hasGeminiKeyConfigured() {
        String p = System.getProperty("GEMINI_API_KEY");
        if (p != null && !p.isBlank()) {
            return true;
        }
        String e = System.getenv("GEMINI_API_KEY");
        return e != null && !e.isBlank();
    }

    private String callGroq(String apiKey, String prompt) {
        String truncated = prompt.length() > 48_000 ? prompt.substring(0, 48_000) + "\n\n[... tronque ...]" : prompt;
        String escaped = escapeJson(truncated);
        String payload = """
                {
                  "model": "%s",
                  "temperature": 0.55,
                  "max_tokens": 8192,
                  "messages": [
                    {"role": "user", "content": "%s"}
                  ]
                }
                """.formatted(GROQ_MODEL, escaped);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_CHAT_URL))
                .timeout(Duration.ofSeconds(55))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            if (response.statusCode() >= 400) {
                String hint = extractGroqErrorMessage(body);
                throw new RuntimeException("HTTP " + response.statusCode()
                        + (hint != null && !hint.isBlank() ? " — " + hint : ""));
            }
            return extractOpenAiAssistantContent(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requete Groq interrompue.", e);
        } catch (IOException e) {
            throw new RuntimeException("Reseau Groq : " + e.getMessage(), e);
        }
    }

    private static String extractGroqErrorMessage(String json) {
        if (json == null) {
            return null;
        }
        int i = json.indexOf("\"message\"");
        if (i < 0) {
            return null;
        }
        int colon = json.indexOf(':', i);
        int q = indexOfNonWhitespace(json, colon + 1);
        if (q < 0 || json.charAt(q) != '"') {
            return null;
        }
        return readJsonStringContent(json, q + 1);
    }

    private static String extractOpenAiAssistantContent(String json) {
        int role = json.indexOf("\"role\":\"assistant\"");
        if (role < 0) {
            role = json.indexOf("\"role\": \"assistant\"");
        }
        if (role < 0) {
            throw new RuntimeException("Reponse Groq inattendue (pas de message assistant).");
        }
        int contentKey = json.indexOf("\"content\"", role);
        if (contentKey < 0) {
            contentKey = json.indexOf("\"content\" ", role);
        }
        if (contentKey < 0) {
            throw new RuntimeException("Reponse Groq sans champ content.");
        }
        int colon = json.indexOf(':', contentKey + 9);
        int openQuote = indexOfNonWhitespace(json, colon + 1);
        if (openQuote < 0 || json.charAt(openQuote) != '"') {
            throw new RuntimeException("Reponse Groq mal formee (content).");
        }
        String text = readJsonStringContent(json, openQuote + 1);
        if (text.isBlank()) {
            throw new RuntimeException("Reponse Groq vide.");
        }
        return text.trim();
    }

    private static int indexOfNonWhitespace(String s, int from) {
        while (from < s.length() && Character.isWhitespace(s.charAt(from))) {
            from++;
        }
        return from < s.length() ? from : -1;
    }

    /**
     * Lit une valeur string JSON a partir du premier caractere apres l'ouverture ".
     */
    private static String readJsonStringContent(String json, int start) {
        StringBuilder sb = new StringBuilder();
        for (int p = start; p < json.length(); p++) {
            char c = json.charAt(p);
            if (c == '\\' && p + 1 < json.length()) {
                char n = json.charAt(p + 1);
                switch (n) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(n);
                }
                p++;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
