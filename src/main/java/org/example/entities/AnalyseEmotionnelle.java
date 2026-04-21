package org.example.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AnalyseEmotionnelle {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private int journalId;
    private String etatEmotionnel;
    private String niveau;
    private String declencheur;
    private String conseil;
    private LocalDateTime dateAnalyse;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getJournalId() { return journalId; }
    public void setJournalId(int journalId) { this.journalId = journalId; }
    public String getEtatEmotionnel() { return etatEmotionnel; }
    public void setEtatEmotionnel(String etatEmotionnel) { this.etatEmotionnel = etatEmotionnel; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public String getDeclencheur() { return declencheur; }
    public void setDeclencheur(String declencheur) { this.declencheur = declencheur; }
    public String getConseil() { return conseil; }
    public void setConseil(String conseil) { this.conseil = conseil; }
    public LocalDateTime getDateAnalyse() { return dateAnalyse; }
    public void setDateAnalyse(LocalDateTime dateAnalyse) { this.dateAnalyse = dateAnalyse; }
    public String getDateAnalyseFormatted() { return dateAnalyse == null ? "" : dateAnalyse.format(FORMATTER); }
}
