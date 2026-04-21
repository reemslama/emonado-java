package org.example.entities;

import java.util.ArrayList;
import java.util.List;

public class Question {

    private int id;
    private String texte;
    private Integer ordre;
    private String typeQuestion;
    private String categorie;
    private List<Reponse> reponses;

    public Question() {
        this.reponses = new ArrayList<>();
    }

    public Question(String texte, Integer ordre, String typeQuestion, String categorie) {
        this.texte = texte;
        this.ordre = ordre;
        this.typeQuestion = typeQuestion;
        this.categorie = categorie;
        this.reponses = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }

    public String getTypeQuestion() { return typeQuestion; }
    public void setTypeQuestion(String typeQuestion) { this.typeQuestion = typeQuestion; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public List<Reponse> getReponses() { return reponses; }
    public void setReponses(List<Reponse> reponses) { this.reponses = reponses; }

    public void addReponse(Reponse reponse) {
        if (!this.reponses.contains(reponse)) {
            this.reponses.add(reponse);
            reponse.setQuestion(this);
        }
    }

    public void removeReponse(Reponse reponse) {
        if (this.reponses.remove(reponse)) {
            if (reponse.getQuestion() == this) {
                reponse.setQuestion(null);
            }
        }
    }

    @Override
    public String toString() {
        return "Question{id=" + id + ", texte='" + texte + "', ordre=" + ordre +
                ", typeQuestion='" + typeQuestion + "', categorie='" + categorie + "'}";
    }
}