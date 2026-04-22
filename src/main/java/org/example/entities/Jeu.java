package org.example.entities;

import java.util.ArrayList;
import java.util.List;

public class Jeu {
    private int id;
    private String titre;
    private String description;
    private String sceneImagePath;
    private String sceneCode;
    private String sceneType;
    private int maxParticipants;
    private boolean actif;
    private int nombreParticipations;
    private List<ImageCarte> images = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSceneImagePath() {
        return sceneImagePath;
    }

    public void setSceneImagePath(String sceneImagePath) {
        this.sceneImagePath = sceneImagePath;
        if (sceneImagePath != null && !sceneImagePath.isBlank()) {
            String normalized = sceneImagePath.replace("\\", "/").toLowerCase();
            this.sceneCode = normalized;
            if (normalized.contains("parc")) {
                this.sceneType = "parc";
            } else if (normalized.contains("maison")) {
                this.sceneType = "maison";
            } else if (normalized.contains("nuit")) {
                this.sceneType = "nuit";
            } else if (normalized.contains("ecole")) {
                this.sceneType = "ecole";
            } else {
                this.sceneType = "scene";
            }
        }
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public int getNombreParticipations() {
        return nombreParticipations;
    }

    public void setNombreParticipations(int nombreParticipations) {
        this.nombreParticipations = nombreParticipations;
    }

    public List<ImageCarte> getImages() {
        return images;
    }

    public void setImages(List<ImageCarte> images) {
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
    }

    public boolean isDisponible() {
        return actif && nombreParticipations < maxParticipants;
    }

    public int getNombreChoix() {
        return images == null ? 0 : images.size();
    }

    public boolean isValidePourExperienceEnfant() {
        return actif
                && sceneImagePath != null
                && !sceneImagePath.isBlank()
                && getNombreChoix() >= 2
                && getNombreChoix() <= 3;
    }

    public String getSituationImage() {
        return sceneImagePath;
    }

    @Override
    public String toString() {
        return titre;
    }
}
