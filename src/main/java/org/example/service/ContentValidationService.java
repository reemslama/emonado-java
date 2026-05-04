package org.example.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ContentValidationService {
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-zÀ-ÿ]{2,}");
    private static final String VOWELS = "aeiouyàâäéèêëîïôöùûüÿ";
    private static final String RARE_LETTERS = "jkqwxyz";

    private ContentValidationService() {
    }

    public static String validateContent(String contenu) {
        if (contenu == null || contenu.trim().isEmpty()) {
            return "Le contenu est obligatoire.";
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

        List<String> words = extractWords(trimmed);
        if (words.size() < 3) {
            return "Ecrivez une phrase claire avec au moins 3 mots.";
        }

        int validWordCount = 0;
        for (String word : words) {
            if (WORD_PATTERN.matcher(word).matches()) {
                validWordCount++;
            }
        }

        if (validWordCount < 3) {
            return "Ecrivez une phrase claire avec au moins 3 mots.";
        }

        boolean hasLongWord = false;
        for (String word : words) {
            if (word.matches("[A-Za-zÀ-ÿ]{4,}")) {
                hasLongWord = true;
                break;
            }
        }

        if (!hasLongWord) {
            return "Le texte doit contenir de vrais mots, pas seulement quelques lettres.";
        }

        if (!hasEnoughNaturalWords(words)) {
            return "Le texte doit contenir une phrase claire et des mots comprehensibles.";
        }

        if (!trimmed.matches(".*[.!?,;:].*") && tokens.length < 5) {
            return "Ecrivez une phrase plus complete pour decrire votre pensee.";
        }

        return null;
    }

    private static List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        for (String token : text.split("\\s+")) {
            String cleaned = token.replaceAll("^[^A-Za-zÀ-ÿ]+|[^A-Za-zÀ-ÿ'-]+$", "");
            if (!cleaned.isBlank()) {
                words.add(cleaned);
            }
        }
        return words;
    }

    private static boolean hasEnoughNaturalWords(List<String> words) {
        int eligibleWords = 0;
        int naturalWords = 0;

        for (String word : words) {
            String normalized = normalizeWord(word);
            if (normalized.length() < 3) {
                continue;
            }
            eligibleWords++;
            if (looksNaturalWord(normalized)) {
                naturalWords++;
            }
        }

        if (eligibleWords < 3) {
            return false;
        }

        // Tolerant: accept a mostly-correct sentence even if it contains
        // a small amount of gibberish (e.g. one random token at the end).
        if (naturalWords < 3) {
            return false;
        }
        if (eligibleWords <= 5) {
            return true;
        }
        return naturalWords >= Math.ceil(eligibleWords * 0.4);
    }

    private static boolean looksNaturalWord(String word) {
        int vowelCount = 0;
        int rareLetterCount = 0;
        int maxRepeatedChar = 1;
        int maxConsonantRun = 0;
        int currentRepeated = 1;
        int currentConsonantRun = 0;

        for (int i = 0; i < word.length(); i++) {
            char current = word.charAt(i);
            boolean isVowel = isVowel(current);

            if (isVowel) {
                vowelCount++;
                currentConsonantRun = 0;
            } else {
                currentConsonantRun++;
                maxConsonantRun = Math.max(maxConsonantRun, currentConsonantRun);
            }

            if (RARE_LETTERS.indexOf(current) >= 0) {
                rareLetterCount++;
            }

            if (i > 0 && word.charAt(i - 1) == current) {
                currentRepeated++;
                maxRepeatedChar = Math.max(maxRepeatedChar, currentRepeated);
            } else {
                currentRepeated = 1;
            }
        }

        double vowelRatio = (double) vowelCount / word.length();
        double rareRatio = (double) rareLetterCount / word.length();

        if (vowelCount == 0) {
            return false;
        }
        if (vowelRatio < 0.20 || vowelRatio > 0.85) {
            return false;
        }
        if (rareRatio > 0.45) {
            return false;
        }
        if (maxRepeatedChar > 2) {
            return false;
        }
        return maxConsonantRun <= 4;
    }

    private static boolean isVowel(char c) {
        return VOWELS.indexOf(c) >= 0;
    }

    private static String normalizeWord(String word) {
        String lowered = word.toLowerCase().replace("'", "").replace("-", "");
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFC);
        return normalized.replaceAll("[^a-zàâäéèêëîïôöùûüÿ]", "");
    }
}
