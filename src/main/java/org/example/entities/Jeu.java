package org.example.entities;

import java.util.ArrayList;
import java.util.List;

public class Jeu {
    private int id;
    private String titre;
    private String description;
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
        this.images = images;
    }

    public boolean isDisponible() {
        return actif && nombreParticipations < maxParticipants;
    }

    @Override
    public String toString() {
        return titre;
    }
}
