package org.example.service;

import java.util.Map;

public class AnalyseJeuService {

    public static String analyserEtat(Map<String, String> reponses) {

        int score = 0;

        for (String rep : reponses.values()) {
            if (rep.contains("triste") || rep.contains("seul")) {
                score++;
            }
        }

        if (score >= 3) {
            return "Enfant potentiellement anxieux";
        } else {
            return "État émotionnel normal";
        }
    }

    public static String genererConseil(String etat) {

        if (etat.contains("anxieux")) {
            return "Encourager la communication et consulter un spécialiste si nécessaire.";
        } else {
            return "Continuer à surveiller et encourager les activités sociales.";
        }
    }
}