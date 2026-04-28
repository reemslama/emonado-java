package org.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiChatService {
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
    /** Cle par defaut vide : definir GEMINI_API_KEY ou utiliser {@link ClinicalLlmService} avec GROQ_API_KEY. */
    private static final String DEFAULT_API_KEY = "";
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");

    private static final List<String> GREETINGS = List.of(
            "hi", "hello", "hey", "bonjour", "bonsoir", "salut", "cc", "coucou"
    );

    private static final List<String> PSYCHOLOGY_KEYWORDS = List.of(
            "stress", "anx", "angoiss", "peur", "triste", "trist", "depress", "deprime", "mal etre",
            "solitude", "seul", "fatigue mentale", "fatigue psych", "epuise", "epuis", "burnout",
            "pleure", "pleurer", "colere", "culpabil", "honte", "trauma", "souvenir", "obsession",
            "panic", "panique", "respirer", "respiration", "emotion", "sentiment", "ressens", "ressenti",
            "je me sens", "je sens", "psycholog", "mental", "crise", "insomnie", "sommeil", "motivation",
            "confiance", "estime", "harcele", "harcelement", "pression", "vide", "detresse", "detresse",
            "inquiet", "inquiete", "nerv", "rumination", "ruminer", "overthink", "pens", "angoisse",
            "therapie", "psy", "psychologue", "je souffre", "je vais mal", "j'ai mal", "je suis perdu",
            "perdue", "je me sens seul", "besoin de parler", "besoin d'aide", "mal dans ma peau",
            "relation toxique", "rupture", "deuil", "famille", "parents", "couple", "rejet", "abandon"
    );

    private static final List<String> OFF_TOPIC_KEYWORDS = List.of(
            "film", "serie", "netflix", "anime", "musique", "restaurant", "recette", "cuisine", "jeu",
            "gaming", "console", "football", "voyage", "hotel", "shopping", "telephone", "smartphone",
            "pc", "ordinateur", "code java", "programmation", "devoir", "math", "examen", "voiture"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String askPsychologyAssistant(String patientName, String message) {
        String normalized = normalize(message);

        if (normalized.isBlank()) {
            return "Je suis la pour parler de votre etat emotionnel. Dites-moi simplement comment vous vous sentez.";
        }

        if (isGreeting(normalized)) {
            return "Bonjour. Je suis la pour vous ecouter. Comment vous sentez-vous aujourd'hui, moralement ou emotionnellement ?";
        }

        if (containsEmergencySignal(normalized)) {
            return "Je ne peux pas gerer une urgence. Contactez immediatement le 15, le 112, les urgences ou une personne de confiance proche.";
        }

        if (!isPsychologyTopic(normalized)) {
            return "Je suis limite aux sujets psychologiques et emotionnels. Je peux vous ecouter si vous voulez parler de stress, tristesse, anxiete, peur, solitude ou de ce que vous ressentez.";
        }

        String apiKey = readApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "Cle API non configuree. Demandez a l'administrateur d'ajouter GEMINI_API_KEY (Google AI Studio) "
                    + "ou GROQ_API_KEY pour l'analyse clinique.";
        }
        String prompt = buildPrompt(patientName, message);
        String payload = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Gemini a retourne une erreur HTTP " + response.statusCode() + ".");
            }
            return postProcess(extractText(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La requete vers Gemini a ete interrompue.", e);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de contacter le chatbot Gemini : " + e.getMessage(), e);
        }
    }

    /**
     * Appel Gemini pour le contexte clinique (journaux). Preferez {@link ClinicalLlmService#completeClinical(String)} (Groq en priorite).
     */
    public String generateClinicalViaGemini(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt vide.");
        }
        String body = prompt.length() > 48_000 ? prompt.substring(0, 48_000) + "\n\n[... texte tronque pour l'API ...]" : prompt;

        String apiKey = readApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "Aucune cle Gemini. Creez une cle sur https://aistudio.google.com/apikey et definissez la variable "
                            + "d'environnement GEMINI_API_KEY, ou utilisez GROQ_API_KEY (gratuit, https://console.groq.com/keys ).");
        }
        String payload = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(escapeJson(body));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Gemini HTTP " + response.statusCode()
                        + ". Essayez GROQ_API_KEY (console.groq.com) ou une autre cle GEMINI_API_KEY.");
            }
            return extractText(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requete Gemini interrompue.", e);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de contacter Gemini : " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String patientName, String message) {
        return """
                Tu es un assistant d'ecoute psychologique pour Emonado.
                Tu dois parler comme un psychologue d'ecoute empathique, simple et humain.
                Tu reponds uniquement en francais.
                Tu aides un patient pendant l'absence temporaire de son psychologue.

                Regles absolues :
                - Tu restes uniquement sur les emotions, les ressentis, le stress, l'anxiete, la tristesse, la peur, les relations et la regulation emotionnelle.
                - Tu n'acceptes pas les sujets de divertissement, films, series, jeux, shopping, cuisine, technologie ou recommandations loisirs.
                - Tu ne proposes jamais de film, serie, musique, activite fun ou distraction generique si le patient ne parle pas d'abord de son ressenti psychologique.
                - Tu ne fais jamais de diagnostic medical ou psychiatrique.
                - Tu ne prescris jamais de medicament.
                - Tu reponds avec empathie, reformulation, validation emotionnelle et 1 a 3 pistes concretes maximum.
                - Tu encourages doucement le patient a exprimer ce qu'il ressent.
                - Si la situation semble grave, tu orientes vers un psychologue, un proche ou les urgences.
                - Si le patient semble juste dire bonjour, tu lui demandes comment il se sent aujourd'hui.
                - Ta reponse doit ressembler a une conversation avec un psy d'ecoute, pas a un moteur de recommandations.

                Structure recommandee :
                1. Validation courte de l'emotion ou du besoin
                2. Une question douce ou une reformulation
                3. Une a trois pistes utiles maximum si c'est pertinent

                Patient : %s
                Message du patient : %s
                """.formatted(patientName == null || patientName.isBlank() ? "Patient" : patientName, message);
    }

    private String postProcess(String response) {
        String normalized = normalize(response);
        if (containsOffTopic(normalized)) {
            return "Je prefere rester centre sur votre etat emotionnel. Si vous voulez, dites-moi plutot ce que vous ressentez en ce moment.";
        }
        return response;
    }

    private boolean isGreeting(String normalized) {
        return GREETINGS.stream().anyMatch(greeting -> normalized.equals(greeting));
    }

    private boolean containsEmergencySignal(String normalized) {
        return normalized.contains("suicide")
                || normalized.contains("me tuer")
                || normalized.contains("m automutil")
                || normalized.contains("self harm")
                || normalized.contains("mettre fin a mes jours");
    }

    private boolean isPsychologyTopic(String normalized) {
        if (containsOffTopic(normalized) && !containsPsychologyKeyword(normalized)) {
            return false;
        }
        return containsPsychologyKeyword(normalized);
    }

    private boolean containsPsychologyKeyword(String normalized) {
        return PSYCHOLOGY_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private boolean containsOffTopic(String normalized) {
        return OFF_TOPIC_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value
                .toLowerCase()
                .replace("'", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractText(String json) {
        Matcher matcher = TEXT_PATTERN.matcher(json);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String text = unescapeJson(matcher.group(1));
            if (!text.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append(text);
            }
        }

        if (builder.isEmpty()) {
            throw new RuntimeException("Reponse Gemini invalide ou vide.");
        }
        return builder.toString().trim();
    }

    private String readApiKey() {
        String property = System.getProperty("GEMINI_API_KEY");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_API_KEY;
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
