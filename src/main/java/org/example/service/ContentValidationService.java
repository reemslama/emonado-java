package org.example.service;

import java.util.regex.Pattern;

public final class ContentValidationService {
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-zÀ-ÿ]{2,}");

    private ContentValidationService() {
    }

    public static String validateContent(String contenu) {
        if (contenu == null || contenu.trim().isEmpty()) {
            return "Le contenu du journal est obligatoire.";
        }

        String trimmed = contenu.trim();
        if (trimmed.length() < 10) {
            return "Le contenu doit contenir au moins 10 caracteres.";
        }
        if (trimmed.length() > 1000) {
            return "Le contenu ne peut pas depasser 1000 caracteres.";
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length < 3) {
            return "Ecrivez une phrase claire avec au moins 3 mots.";
        }

        int validWordCount = 0;
        for (String token : tokens) {
            if (WORD_PATTERN.matcher(token).find()) {
                validWordCount++;
            }
        }

        if (validWordCount < 3) {
            return "Ecrivez une phrase claire avec au moins 3 mots.";
        }

        boolean hasLongWord = false;
        for (String token : tokens) {
            if (token.matches(".*[A-Za-zÀ-ÿ]{4,}.*")) {
                hasLongWord = true;
                break;
            }
        }

        if (!hasLongWord) {
            return "Le journal doit contenir de vrais mots, pas seulement quelques lettres.";
        }

        if (!trimmed.matches(".*[.!?,;:].*") && tokens.length < 5) {
            return "Ecrivez une phrase plus complete pour decrire votre journal.";
        }

        return null;
    }
}
