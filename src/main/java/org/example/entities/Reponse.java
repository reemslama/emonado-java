package org.example.entities;

public class Reponse {

    private int id;
    private String texte;
    private int valeur;
    private Integer ordre;
    private Question question;

    public Reponse() {}

    public Reponse(String texte, int valeur, Integer ordre, Question question) {
        this.texte = texte;
        this.valeur = valeur;
        this.ordre = ordre;
        this.question = question;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public int getValeur() { return valeur; }
    public void setValeur(int valeur) { this.valeur = valeur; }

    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) {
        this.question = question;
    }

    @Override
    public String toString() {
        return "Reponse{id=" + id + ", texte='" + texte + "', valeur=" + valeur + ", ordre=" + ordre + "}";
    }
}