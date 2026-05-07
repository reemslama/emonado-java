package org.example.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiskDetectionService {
    private static final Map<String, Integer> HIGH_RISK_TERMS = new LinkedHashMap<>();
    private static final Map<String, Integer> MEDIUM_RISK_TERMS = new LinkedHashMap<>();

    static {
        HIGH_RISK_TERMS.put("suicide", 10);
        HIGH_RISK_TERMS.put("me suicider", 10);
        HIGH_RISK_TERMS.put("mettre fin a mes jours", 10);
        HIGH_RISK_TERMS.put("je veux mourir", 10);
        HIGH_RISK_TERMS.put("je veut mourir", 10);
        HIGH_RISK_TERMS.put("veux mourir", 10);
        HIGH_RISK_TERMS.put("veut mourir", 10);
        HIGH_RISK_TERMS.put("je veux mourrir", 10);
        HIGH_RISK_TERMS.put("je veut mourrir", 10);
        HIGH_RISK_TERMS.put("mourrir", 9);
        HIGH_RISK_TERMS.put("mourir", 8);
        HIGH_RISK_TERMS.put("je vais mourir", 8);
        HIGH_RISK_TERMS.put("envie de mourir", 10);
        HIGH_RISK_TERMS.put("plus envie de vivre", 10);
        HIGH_RISK_TERMS.put("je ne veux plus vivre", 10);
        HIGH_RISK_TERMS.put("je veux disparaitre", 9);
        HIGH_RISK_TERMS.put("disparaitre", 7);
        HIGH_RISK_TERMS.put("tout arreter", 8);
        HIGH_RISK_TERMS.put("mettre fin", 9);
        HIGH_RISK_TERMS.put("automutilation", 10);
        HIGH_RISK_TERMS.put("me couper", 8);
        HIGH_RISK_TERMS.put("overdose", 10);
        HIGH_RISK_TERMS.put("tuer quelqu", 9);
        HIGH_RISK_TERMS.put("frapper quelqu", 8);
        HIGH_RISK_TERMS.put("poignarder", 9);
        HIGH_RISK_TERMS.put("etrangler", 9);
        HIGH_RISK_TERMS.put("violence", 7);
        HIGH_RISK_TERMS.put("agresser", 8);
        HIGH_RISK_TERMS.put("arme", 8);

        MEDIUM_RISK_TERMS.put("haine", 4);
        MEDIUM_RISK_TERMS.put("depression", 6);
        MEDIUM_RISK_TERMS.put("depressif", 6);
        MEDIUM_RISK_TERMS.put("depressive", 6);
        MEDIUM_RISK_TERMS.put("deprime", 5);
        MEDIUM_RISK_TERMS.put("deprimer", 5);
        MEDIUM_RISK_TERMS.put("je suis deprimer", 6);
        MEDIUM_RISK_TERMS.put("je suis deprime", 6);
        MEDIUM_RISK_TERMS.put("triste tout le temps", 5);
        MEDIUM_RISK_TERMS.put("tres triste", 4);
        MEDIUM_RISK_TERMS.put("vide", 3);
        MEDIUM_RISK_TERMS.put("je me sens vide", 5);
        MEDIUM_RISK_TERMS.put("seul", 3);
        MEDIUM_RISK_TERMS.put("seule", 3);
        MEDIUM_RISK_TERMS.put("solitude", 4);
        MEDIUM_RISK_TERMS.put("pleurer", 3);
        MEDIUM_RISK_TERMS.put("je pleure", 4);
        MEDIUM_RISK_TERMS.put("fatigue de vivre", 7);
        MEDIUM_RISK_TERMS.put("plus la force", 5);
        MEDIUM_RISK_TERMS.put("sans espoir", 6);
        MEDIUM_RISK_TERMS.put("inutile", 4);
        MEDIUM_RISK_TERMS.put("je suis nul", 4);
        MEDIUM_RISK_TERMS.put("je suis nulle", 4);
        MEDIUM_RISK_TERMS.put("colere noire", 5);
        MEDIUM_RISK_TERMS.put("envie de frapper", 6);
        MEDIUM_RISK_TERMS.put("je n en peux plus", 5);
        MEDIUM_RISK_TERMS.put("j en peux plus", 5);
        MEDIUM_RISK_TERMS.put("je ne peux plus", 5);
        MEDIUM_RISK_TERMS.put("desespoir", 5);
        MEDIUM_RISK_TERMS.put("peur de moi", 4);
        MEDIUM_RISK_TERMS.put("faire du mal", 6);
        MEDIUM_RISK_TERMS.put("crise", 4);
        MEDIUM_RISK_TERMS.put("panique", 3);
        MEDIUM_RISK_TERMS.put("insomnie", 2);
    }

    public RiskAssessment assess(String content) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return new RiskAssessment("Aucun", 0, List.of(), "Aucun terme critique detecte.");
        }

        int score = 0;
        List<String> matchedTerms = new ArrayList<>();

        score += scanTerms(normalized, HIGH_RISK_TERMS, matchedTerms);
        score += scanTerms(normalized, MEDIUM_RISK_TERMS, matchedTerms);

        String label;
        if (score >= 10) {
            label = "Critique";
        } else if (score >= 6) {
            label = "Eleve";
        } else if (score >= 3) {
            label = "Modere";
        } else {
            label = "Aucun";
        }

        String summary = matchedTerms.isEmpty()
                ? "Aucun terme critique detecte."
                : "Termes detectes : " + String.join(", ", matchedTerms);

        return new RiskAssessment(label, score, matchedTerms, summary);
    }

    private int scanTerms(String content, Map<String, Integer> terms, List<String> matchedTerms) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : terms.entrySet()) {
            if (content.contains(entry.getKey())) {
                score += entry.getValue();
                matchedTerms.add(entry.getKey());
            }
        }
        return score;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        String lower = content.toLowerCase(Locale.ROOT);
        String noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record RiskAssessment(String level, int score, List<String> matchedTerms, String summary) {
        public boolean requiresAlert() {
            return !"Aucun".equalsIgnoreCase(level);
        }

        public boolean isCritical() {
            return "Critique".equalsIgnoreCase(level) || "Eleve".equalsIgnoreCase(level);
        }
    }
}
