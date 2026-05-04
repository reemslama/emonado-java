package org.example.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAdaptatif {
    private Integer id;
    private User patient;
    private String categorie;
    private List<Map<String, Object>> questionsReponses = new ArrayList<>();
    private int scoreActuel = 0;
    private int nombreQuestions = 0;
    private boolean termine = false;
    private LocalDateTime dateDebut = LocalDateTime.now();
    private String analyse;

    public void ajouterQuestionReponse(String question, String reponse, int valeur) {
        Map<String, Object> qr = new HashMap<>();
        qr.put("question", question);
        qr.put("reponse", reponse);
        qr.put("valeur", valeur);
        qr.put("timestamp", LocalDateTime.now().toString());

        this.questionsReponses.add(qr);
        this.scoreActuel += valeur;
        this.nombreQuestions++;
    }

    // --- Méthodes nécessaires pour le contrôleur ---
    public List<Map<String, Object>> getQuestionsReponses() { return questionsReponses; }
    public int getScoreTotal() { return scoreActuel; }
    public int getNombreQuestions() { return nombreQuestions; }

    // --- Getters / Setters classiques ---
    public String getCategorie() { return categorie; }
    public void setCategorie(String c) { this.categorie = c; }
    public void setPatient(User p) { this.patient = p; }
    public void setAnalyse(String a) { this.analyse = a; }
    public void setTermine(boolean t) { this.termine = t; }
}