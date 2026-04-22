package org.example.entities;

import java.time.LocalDateTime;

public class Participation {
    private int id;
    private int userId;
    private int jeuId;
    private String jeuTitre;
    private int imageChoisieId;
    private String imagePath;
    private String choixImage;
    private String resultatPsy;
    private String comportementTag;
    private String sessionCode;
    private int scoreImpact;
    private long tempsReponseMs;
    private LocalDateTime dateParticipation;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJeuId() {
        return jeuId;
    }

    public void setJeuId(int jeuId) {
        this.jeuId = jeuId;
    }

    public String getJeuTitre() {
        return jeuTitre;
    }

    public void setJeuTitre(String jeuTitre) {
        this.jeuTitre = jeuTitre;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getImageChoisieId() {
        return imageChoisieId;
    }

    public void setImageChoisieId(int imageChoisieId) {
        this.imageChoisieId = imageChoisieId;
    }

    public int getImageCarteId() {
        return imageChoisieId;
    }

    public void setImageCarteId(int imageCarteId) {
        this.imageChoisieId = imageCarteId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        this.choixImage = imagePath;
    }

    public String getChoixImage() {
        return choixImage;
    }

    public void setChoixImage(String choixImage) {
        this.choixImage = choixImage;
        this.imagePath = choixImage;
    }

    public String getResultatPsy() {
        return resultatPsy;
    }

    public void setResultatPsy(String resultatPsy) {
        this.resultatPsy = resultatPsy;
    }

    public String getComportementTag() {
        return comportementTag;
    }

    public void setComportementTag(String comportementTag) {
        this.comportementTag = comportementTag;
    }

    public String getChoixTag() {
        return comportementTag;
    }

    public void setChoixTag(String choixTag) {
        this.comportementTag = choixTag;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public int getScoreImpact() {
        return scoreImpact;
    }

    public void setScoreImpact(int scoreImpact) {
        this.scoreImpact = scoreImpact;
    }

    public long getTempsReponseMs() {
        return tempsReponseMs;
    }

    public void setTempsReponseMs(long tempsReponseMs) {
        this.tempsReponseMs = tempsReponseMs;
    }

    public String getInterpretation() {
        return resultatPsy;
    }

    public void setInterpretation(String interpretation) {
        this.resultatPsy = interpretation;
    }

    public LocalDateTime getDateParticipation() {
        return dateParticipation;
    }

    public void setDateParticipation(LocalDateTime dateParticipation) {
        this.dateParticipation = dateParticipation;
    }

    public boolean isDecisionRapide() {
        return tempsReponseMs > 0 && tempsReponseMs < 2000;
    }

    public boolean isDecisionLente() {
        return tempsReponseMs >= 7000;
    }
}
