package org.example.entities;

import java.time.LocalDateTime;

public class ResultatTest {
    private Integer id;
    private String categorie;
    private int scoreActuel;
    private int scoreMax;
    private String niveau;       // FAIBLE / MODÉRÉ / ÉLEVÉ
    private String analyseIA;
    private LocalDateTime dateTest;

    public ResultatTest() {}

    public ResultatTest(String categorie, int scoreActuel, int scoreMax,
                        String niveau, String analyseIA, LocalDateTime dateTest) {
        this.categorie   = categorie;
        this.scoreActuel = scoreActuel;
        this.scoreMax    = scoreMax;
        this.niveau      = niveau;
        this.analyseIA   = analyseIA;
        this.dateTest    = dateTest;
    }

    // ---------- Getters / Setters ----------
    public Integer getId()                     { return id; }
    public void   setId(Integer id)            { this.id = id; }

    public String getCategorie()               { return categorie; }
    public void   setCategorie(String c)       { this.categorie = c; }

    public int    getScoreActuel()             { return scoreActuel; }
    public void   setScoreActuel(int s)        { this.scoreActuel = s; }

    public int    getScoreMax()                { return scoreMax; }
    public void   setScoreMax(int s)           { this.scoreMax = s; }

    public String getNiveau()                  { return niveau; }
    public void   setNiveau(String n)          { this.niveau = n; }

    public String getAnalyseIA()               { return analyseIA; }
    public void   setAnalyseIA(String a)       { this.analyseIA = a; }

    public LocalDateTime getDateTest()         { return dateTest; }
    public void          setDateTest(LocalDateTime d) { this.dateTest = d; }

    /** Pourcentage 0.0 – 1.0 utile pour les graphiques */
    public double getPourcentage() {
        return scoreMax == 0 ? 0 : (double) scoreActuel / scoreMax;
    }
}
