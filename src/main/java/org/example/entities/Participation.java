package org.example.entities;

import java.time.LocalDateTime;

public class Participation {
    private int id;
    private int userId;
    private int jeuId;
    private String jeuTitre;
    private int imageChoisieId;
    private String imagePath;       // chargé par JOIN pour l'affichage
    private String resultatPsy;  // copié depuis image_carte.interpretation_psy
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
    }

    public String getResultatPsy() {
        return resultatPsy;
    }

    public void setResultatPsy(String resultatPsy) {
        this.resultatPsy = resultatPsy;
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
}
