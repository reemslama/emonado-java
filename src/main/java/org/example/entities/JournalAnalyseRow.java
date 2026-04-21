package org.example.entities;

public class JournalAnalyseRow {
    private int journalId;
    private String humeur;
    private String contenuResume;
    private String dateJournal;
    private String etatEmotionnel;
    private String niveau;
    private String statut;
    private AnalyseEmotionnelle analyseEmotionnelle;

    public int getJournalId() { return journalId; }
    public void setJournalId(int journalId) { this.journalId = journalId; }
    public String getHumeur() { return humeur; }
    public void setHumeur(String humeur) { this.humeur = humeur; }
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
    public AnalyseEmotionnelle getAnalyseEmotionnelle() { return analyseEmotionnelle; }
    public void setAnalyseEmotionnelle(AnalyseEmotionnelle analyseEmotionnelle) { this.analyseEmotionnelle = analyseEmotionnelle; }
}
