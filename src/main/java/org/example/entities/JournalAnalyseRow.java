package org.example.entities;

public class JournalAnalyseRow {
    private int journalId;
    private String humeur;
    private String contenuComplet;
    private String contenuResume;
    private String dateJournal;
    private String etatEmotionnel;
    private String niveau;
    private String statut;
    private String risqueLabel;
    private String risqueDetails;
    private int risqueScore;
    private AnalyseEmotionnelle analyseEmotionnelle;

    public int getJournalId() { return journalId; }
    public void setJournalId(int journalId) { this.journalId = journalId; }
    public String getHumeur() { return humeur; }
    public void setHumeur(String humeur) { this.humeur = humeur; }
    public String getContenuComplet() { return contenuComplet; }
    public void setContenuComplet(String contenuComplet) { this.contenuComplet = contenuComplet; }
    public String getContenuResume() { return contenuResume; }
    public void setContenuResume(String contenuResume) { this.contenuResume = contenuResume; }
    public String getDateJournal() { return dateJournal; }
    public void setDateJournal(String dateJournal) { this.dateJournal = dateJournal; }
    public String getEtatEmotionnel() { return etatEmotionnel; }
    public void setEtatEmotionnel(String etatEmotionnel) { this.etatEmotionnel = etatEmotionnel; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getRisqueLabel() { return risqueLabel; }
    public void setRisqueLabel(String risqueLabel) { this.risqueLabel = risqueLabel; }
    public String getRisqueDetails() { return risqueDetails; }
    public void setRisqueDetails(String risqueDetails) { this.risqueDetails = risqueDetails; }
    public int getRisqueScore() { return risqueScore; }
    public void setRisqueScore(int risqueScore) { this.risqueScore = risqueScore; }
    public AnalyseEmotionnelle getAnalyseEmotionnelle() { return analyseEmotionnelle; }
    public void setAnalyseEmotionnelle(AnalyseEmotionnelle analyseEmotionnelle) { this.analyseEmotionnelle = analyseEmotionnelle; }
}
