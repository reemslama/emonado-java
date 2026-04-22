package org.example.service;

import org.example.entities.Participation;
import org.example.entities.ProfilPsychologique;

import java.util.List;

public class AnalyseComportementaleService {

    public ProfilPsychologique analyser(List<Participation> participations) {
        int sociabilite = 0;
        int timidite = 0;
        int stress = 0;
        int curiosite = 0;
        long totalTemps = 0L;

        for (Participation participation : participations) {
            String tag = normalizeTag(participation.getComportementTag());
            switch (tag) {
                case "jouer":
                case "partage":
                case "groupe":
                    sociabilite += 3;
                    curiosite += 1;
                    participation.setScoreImpact(3);
                    break;
                case "rester_seul":
                case "seul":
                case "timidite":
                    timidite += 3;
                    participation.setScoreImpact(-2);
                    break;
                case "peur":
                case "stress":
                case "evitement":
                    stress += 4;
                    timidite += 1;
                    participation.setScoreImpact(-4);
                    break;
                case "exploration":
                case "curiosite":
                    curiosite += 3;
                    participation.setScoreImpact(2);
                    break;
                case "calme":
                case "repos":
                    stress = Math.max(0, stress - 1);
                    curiosite += 1;
                    participation.setScoreImpact(1);
                    break;
                default:
                    curiosite += 1;
                    participation.setScoreImpact(0);
                    break;
            }

            if (participation.isDecisionRapide()) {
                sociabilite += 1;
            }
            if (participation.isDecisionLente()) {
                stress += 1;
                timidite += 1;
            }
            totalTemps += Math.max(0L, participation.getTempsReponseMs());
        }

        int nb = Math.max(1, participations.size());
        long moyenneTemps = totalTemps / nb;
        if (moyenneTemps > 7000) {
            stress += 2;
        } else if (moyenneTemps < 2000) {
            sociabilite += 1;
        }

        int scoreEmotionnel = clamp(50 + sociabilite + curiosite - stress - timidite, 0, 100);
        String profil = determinerProfil(sociabilite, timidite, stress, curiosite);
        String tendance = determinerTendance(sociabilite, timidite, stress, curiosite, moyenneTemps);
        String anomalie = determinerAnomalie(stress, timidite, moyenneTemps);
        String syntheseClinique = buildSynthese(sociabilite, timidite, stress, curiosite, moyenneTemps, participations.size());

        return new ProfilPsychologique(
                profil,
                scoreEmotionnel,
                sociabilite,
                timidite,
                stress,
                curiosite,
                tendance,
                anomalie,
                syntheseClinique
        );
    }

    private String normalizeTag(String tag) {
        return tag == null ? "neutre" : tag.trim().toLowerCase();
    }

    private String determinerProfil(int sociabilite, int timidite, int stress, int curiosite) {
        if (stress >= 7) {
            return "Profil sensible aux situations anxiogenes";
        }
        if (sociabilite >= timidite + 2) {
            return "Profil social et engage";
        }
        if (timidite >= sociabilite + 2) {
            return "Profil reserve et prudent";
        }
        if (curiosite >= 5) {
            return "Profil curieux et explorateur";
        }
        return "Profil emotionnel equilibre";
    }

    private String determinerTendance(int sociabilite, int timidite, int stress, int curiosite, long moyenneTemps) {
        if (stress >= 7 || moyenneTemps > 7000) {
            return "Tendance a l'hesitation et a la vigilance";
        }
        if (sociabilite >= 6) {
            return "Recherche spontanee d'interactions positives";
        }
        if (timidite >= 6) {
            return "Preference recurrente pour le retrait";
        }
        if (curiosite >= 5) {
            return "Interet marque pour l'exploration visuelle";
        }
        return "Aucune tendance dominante";
    }

    private String determinerAnomalie(int stress, int timidite, long moyenneTemps) {
        if (stress >= 9) {
            return "Niveau de stress inhabituellement eleve";
        }
        if (timidite >= 8 && moyenneTemps > 7000) {
            return "Retrait marque avec temps de decision eleve";
        }
        return "Aucune anomalie comportementale evidente";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String buildSynthese(int sociabilite,
                                 int timidite,
                                 int stress,
                                 int curiosite,
                                 long moyenneTemps,
                                 int nbParticipations) {
        return "Session analysee sur " + nbParticipations + " choix visuels. "
                + "Sociabilite=" + sociabilite
                + ", Timidite=" + timidite
                + ", Stress=" + stress
                + ", Curiosite=" + curiosite
                + ", Temps moyen=" + moyenneTemps + " ms.";
    }
}
