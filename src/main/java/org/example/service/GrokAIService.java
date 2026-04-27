package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.entities.Question;
import org.example.entities.Reponse;
import org.example.entities.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

public class GrokAIService {

    // =========================================================================
    // CONSTANTES
    // =========================================================================

    private static final String GROQ_API_URL  = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final String API_KEY = System.getenv("GROQ_API_KEY");

    /**
     * Clé utilisée dans les Maps de l'historique pour stocker la valeur de la réponse.
     * DOIT être identique à PasserTestController.CLE_VALEUR
     * et à la clé écrite dans TestAdaptatif.ajouterQuestionReponse().
     */
    private static final String CLE_VALEUR = "valeurReponse";

    /**
     * Valeur minimale (incluse) pour qu'une réponse soit "bonne".
     * Échelle 0-3 : 0=très mal, 1=mal, 2=moyen/bien, 3=très bien.
     * DOIT être identique à PasserTestController.SEUIL_BONNE_REPONSE.
     */
    private static final int SEUIL_BONNE_REPONSE = 2;

    /** Nombre de premières questions analysées pour la terminaison anticipée. */
    private static final int NB_QUESTIONS_TERMINAISON = 3;

    // =========================================================================
    // ATTRIBUTS
    // =========================================================================

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;
    private final Logger       logger;
    private final String       groqApiKey;

    // =========================================================================
    // CONSTRUCTEURS
    // =========================================================================

    public GrokAIService() {
        this(API_KEY);
    }

    public GrokAIService(String groqApiKey) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper     = new ObjectMapper();
        this.logger     = Logger.getLogger(GrokAIService.class.getName());
        this.groqApiKey = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey : API_KEY;
    }

    // =========================================================================
    // TERMINAISON ANTICIPÉE
    // =========================================================================

    /**
     * Vérifie si le questionnaire doit se terminer de manière anticipée.
     *
     * <p>Règle : si les {@value #NB_QUESTIONS_TERMINAISON} premières réponses
     * ont toutes une valeur >= {@value #SEUIL_BONNE_REPONSE}, on arrête.
     *
     * <p>La clé lue dans chaque Map est {@value #CLE_VALEUR}, identique à
     * celle écrite par TestAdaptatif.ajouterQuestionReponse().
     *
     * @param historique liste des Q/R déjà enregistrées (peut être null)
     * @return true si le questionnaire doit s'arrêter immédiatement
     */
    public boolean doitTerminerPrecocement(List<Map<String, Object>> historique) {
        if (historique == null || historique.size() < NB_QUESTIONS_TERMINAISON) return false;

        long bonnesReponses = historique.subList(0, NB_QUESTIONS_TERMINAISON)
                .stream()
                .filter(h -> {
                    Object val = h.get(CLE_VALEUR);
                    if (val instanceof Number) {
                        return ((Number) val).intValue() >= SEUIL_BONNE_REPONSE;
                    }
                    return false;
                })
                .count();

        boolean terminer = bonnesReponses == NB_QUESTIONS_TERMINAISON;

        if (terminer) {
            logger.info("Terminaison anticipée : les " + NB_QUESTIONS_TERMINAISON
                    + " premières réponses sont toutes bonnes (score >= "
                    + SEUIL_BONNE_REPONSE + ").");
        }
        return terminer;
    }

    // =========================================================================
    // GÉNÉRATION DE QUESTION
    // =========================================================================

    /**
     * Retourne la prochaine question à poser, ou {@code null} si le
     * questionnaire doit se terminer (terminaison anticipée).
     *
     * @param categorie  catégorie du test
     * @param historique liste des Q/R déjà enregistrées (null = premier appel)
     * @param patient    utilisateur concerné
     * @return prochaine Question, ou null si terminaison anticipée
     */
    public Question getProchaineQuestion(String categorie,
                                         List<Map<String, Object>> historique,
                                         User patient) {
        // ── Terminaison anticipée ─────────────────────────────────────────────
        if (doitTerminerPrecocement(historique)) {
            return null;
        }
        // ──────────────────────────────────────────────────────────────────────

        Map<String, Object> profilPatient   = construireProfilPatient(patient);
        String              typeQuestion    = determinerTypeQuestion(historique);
        Map<String, Object> contenuQuestion =
                genererQuestionPsychologique(categorie, historique, profilPatient, typeQuestion);

        int ordre = (historique == null) ? 1 : historique.size() + 1;
        return mapToQuestion(categorie, ordre, typeQuestion, contenuQuestion);
    }

    public Map<String, Object> genererQuestionPsychologique(String categorie,
                                                            List<Map<String, Object>> historique,
                                                            Map<String, Object> profilPatient,
                                                            String typeQuestion) {
        if (!isConfigured()) return getQuestionParDefaut(categorie, typeQuestion);

        String systemPrompt = construireSystemPrompt(categorie);
        String userPrompt   = construireUserPrompt(categorie, historique, profilPatient, typeQuestion);

        try {
            Map<String, Object> response =
                    appelGroqApi(systemPrompt, userPrompt, 0.8, 500, true);
            return parseResponse(response);
        } catch (Exception e) {
            logger.severe("Erreur Groq API (génération question): " + e.getMessage());
            return getQuestionParDefaut(categorie, typeQuestion);
        }
    }

    // =========================================================================
    // GÉNÉRATION D'ANALYSE
    // =========================================================================

    public String genererAnalyse(String categorie,
                                 List<Map<String, Object>> questionsReponses,
                                 int score,
                                 int nombreQuestions) {
        if (!isConfigured()) return getAnalyseParDefaut(categorie, score, nombreQuestions);

        String systemPrompt =
                "Tu es un psychologue expert. Reponds EXCLUSIVEMENT en JSON valide. "
                        + "Les valeurs doivent etre des chaines de caracteres (String), pas des tableaux. "
                        + "Structure exacte : "
                        + "{ \"analyse_globale\": \"...\", \"points_attention\": \"...\", \"recommandations\": \"...\" }";

        String userPrompt =
                "Analyse ce test de " + categorie + ".\n"
                        + "Score: " + score + "/" + (nombreQuestions * 3) + ".\n"
                        + "Historique des reponses: " + questionsReponses.toString()
                        + "\nReponds en JSON avec trois champs string : "
                        + "analyse_globale, points_attention, recommandations.";

        try {
            Map<String, Object> response =
                    appelGroqApi(systemPrompt, userPrompt, 0.7, 1000, true);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");

            @SuppressWarnings("unchecked")
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            String jsonStr = (String) message.get("content");
            logger.info("Réponse brute API (analyse) : " + jsonStr);

            JsonNode root            = mapper.readTree(jsonStr);
            String   analyseGlobale  = extraireTexte(root, "analyse_globale");
            String   pointsAttention = extraireTexte(root, "points_attention");
            String   recommandations = extraireTexte(root, "recommandations");

            return "--- ANALYSE GLOBALE ---\n" + analyseGlobale
                    + "\n\n--- POINTS D'ATTENTION ---\n" + pointsAttention
                    + "\n\n--- CONSEILS ---\n" + recommandations;

        } catch (Exception e) {
            logger.severe("Erreur analyse: " + e.getMessage());
            return getAnalyseParDefaut(categorie, score, nombreQuestions);
        }
    }

    // =========================================================================
    // EXTRACTION JSON
    // =========================================================================

    private String extraireTexte(JsonNode root, String champ) {
        JsonNode node = root.path(champ);

        if (node.isMissingNode() || node.isNull()) return "(non disponible)";
        if (node.isTextual())  return node.asText();

        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : node) {
                if (sb.length() > 0) sb.append("\n- ");
                else                 sb.append("- ");
                if (item.isTextual()) {
                    sb.append(item.asText());
                } else {
                    item.fields().forEachRemaining(
                            e -> sb.append(e.getValue().asText()).append(" "));
                }
            }
            return sb.toString();
        }

        if (node.isObject()) {
            StringBuilder sb = new StringBuilder();
            node.fields().forEachRemaining(
                    e -> sb.append(e.getKey()).append(": ")
                            .append(e.getValue().asText()).append("\n"));
            return sb.toString().trim();
        }

        return node.asText();
    }

    // =========================================================================
    // CONSTRUCTION DES PROMPTS
    // =========================================================================

    private String construireSystemPrompt(String categorie) {
        return "Tu es un psychologue virtuel. Tu dois generer une question en format JSON.\n"
                + "Structure JSON obligatoire :\n"
                + "{\n"
                + "  \"question\": \"Le texte de la question\",\n"
                + "  \"reponses\": [\n"
                + "    {\"texte\": \"Option 1\", \"valeur\": 0},\n"
                + "    {\"texte\": \"Option 2\", \"valeur\": 1},\n"
                + "    {\"texte\": \"Option 3\", \"valeur\": 2},\n"
                + "    {\"texte\": \"Option 4\", \"valeur\": 3}\n"
                + "  ]\n"
                + "}";
    }

    private String construireUserPrompt(String categorie,
                                        List<Map<String, Object>> historique,
                                        Map<String, Object> profil,
                                        String type) {
        StringBuilder sb = new StringBuilder(
                "Genere une question de type '" + type + "' pour un test de " + categorie + ".\n");
        if (historique != null && !historique.isEmpty()) {
            sb.append("Historique : ").append(historique).append("\n");
        }
        sb.append("Formatte la reponse en JSON.");
        return sb.toString();
    }

    // =========================================================================
    // COMMUNICATION API (UTF-8 forcé)
    // =========================================================================

    private Map<String, Object> appelGroqApi(String system,
                                             String user,
                                             double temp,
                                             int tokens,
                                             boolean isJson) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model",       DEFAULT_MODEL);
        body.put("temperature", temp);
        body.put("max_tokens",  tokens);

        if (isJson) {
            body.set("response_format",
                    mapper.createObjectNode().put("type", "json_object"));
        }

        ArrayNode messages = mapper.createArrayNode();
        messages.add(mapper.createObjectNode().put("role", "system").put("content", system));
        messages.add(mapper.createObjectNode().put("role", "user").put("content", user));
        body.set("messages", messages);

        String bodyStr = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_API_URL))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type",  "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        String responseBody = new String(response.body(), StandardCharsets.UTF_8);

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + responseBody);
        }

        return mapper.readValue(responseBody, Map.class);
    }

    // =========================================================================
    // PARSING
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(Map<String, Object> response) throws Exception {
        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");
        String content =
                (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
        JsonNode node = mapper.readTree(content);

        Map<String, Object>       res  = new HashMap<>();
        List<Map<String, Object>> reps = new ArrayList<>();

        res.put("texte", node.get("question").asText());
        node.get("reponses").forEach(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("texte",  r.get("texte").asText());
            m.put("valeur", r.get("valeur").asInt());
            reps.add(m);
        });
        res.put("reponses", reps);
        return res;
    }

    @SuppressWarnings("unchecked")
    private Question mapToQuestion(String cat, int ord, String type,
                                   Map<String, Object> content) {
        Question q = new Question();
        q.setCategorie(cat);
        q.setOrdre(ord);
        q.setTypeQuestion(type);
        q.setTexte((String) content.get("texte"));

        List<Reponse>             reponses = new ArrayList<>();
        List<Map<String, Object>> raw      =
                (List<Map<String, Object>>) content.get("reponses");

        for (int i = 0; i < raw.size(); i++) {
            Reponse r = new Reponse();
            r.setTexte((String)   raw.get(i).get("texte"));
            r.setValeur((Integer) raw.get(i).get("valeur"));
            r.setOrdre(i + 1);
            r.setQuestion(q);
            reponses.add(r);
        }
        q.setReponses(reponses);
        return q;
    }

    private Map<String, Object> construireProfilPatient(User p) {
        Map<String, Object> m = new HashMap<>();
        if (p != null) {
            m.put("nom",    p.getNom());
            m.put("prenom", p.getPrenom());
        }
        return m;
    }

    private String determinerTypeQuestion(List<Map<String, Object>> h) {
        return (h == null || h.isEmpty()) ? "initial" : "standard";
    }

    // =========================================================================
    // FALLBACKS
    // =========================================================================

    private Map<String, Object> getQuestionParDefaut(String cat, String t) {
        Map<String, Object> q = new HashMap<>();
        q.put("texte", "Comment vous sentez-vous ?");
        q.put("reponses", Arrays.asList(
                createReponse("Bien",     0),
                createReponse("Moyen",    1),
                createReponse("Mal",      2),
                createReponse("Très mal", 3)
        ));
        return q;
    }

    private Map<String, Object> createReponse(String t, int v) {
        Map<String, Object> m = new HashMap<>();
        m.put("texte",  t);
        m.put("valeur", v);
        return m;
    }

    private String getAnalyseParDefaut(String c, int s, int n) {
        return "Analyse simplifiée : Score de " + s + "/" + (n * 3);
    }

    // =========================================================================
    // ÉTAT
    // =========================================================================

    public boolean isConfigured() {
        return groqApiKey != null && !groqApiKey.isBlank();
    }
}