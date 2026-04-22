package org.example.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Journal {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private String contenu;
    private String humeur;
    private LocalDateTime dateCreation;
    private int userId;
    private String etatAnalyse;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getHumeur() {
        return humeur;
    }

    public void setHumeur(String humeur) {
        this.humeur = humeur;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDateCreationFormatted() {
        return dateCreation == null ? "" : dateCreation.format(FORMATTER);
    }

    public String getEtatAnalyse() {
        return etatAnalyse;
    }

    public void setEtatAnalyse(String etatAnalyse) {
        this.etatAnalyse = etatAnalyse;
    }
}
