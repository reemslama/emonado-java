package org.example.entities;

public class ProfilPsychologique {
    private final String profil;
    private final int scoreEmotionnel;
    private final int sociabilite;
    private final int timidite;
    private final int stress;
    private final int curiosite;
    private final String tendance;
    private final String anomalie;
    private final String syntheseClinique;

    public ProfilPsychologique(String profil,
                               int scoreEmotionnel,
                               int sociabilite,
                               int timidite,
                               int stress,
                               int curiosite,
                               String tendance,
                               String anomalie,
                               String syntheseClinique) {
        this.profil = profil;
        this.scoreEmotionnel = scoreEmotionnel;
        this.sociabilite = sociabilite;
        this.timidite = timidite;
        this.stress = stress;
        this.curiosite = curiosite;
        this.tendance = tendance;
        this.anomalie = anomalie;
        this.syntheseClinique = syntheseClinique;
    }

    public String getProfil() {
        return profil;
    }

    public int getScoreEmotionnel() {
        return scoreEmotionnel;
    }

    public int getSociabilite() {
        return sociabilite;
    }

    public int getTimidite() {
        return timidite;
    }

    public int getStress() {
        return stress;
    }

    public int getCuriosite() {
        return curiosite;
    }

    public String getTendance() {
        return tendance;
    }

    public String getAnomalie() {
        return anomalie;
    }

    public String getSyntheseClinique() {
        return syntheseClinique;
    }
}
