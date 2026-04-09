package org.example.services;

import org.example.entities.Question;
import org.example.entities.Reponse;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IService<Question> {

    private Connection connection;

    public QuestionService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public List<Question> getQuestionsByCategorie(String categorie) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE categorie = ? ORDER BY ordre ASC";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, categorie);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTexte(rs.getString("texte"));
                q.setOrdre(rs.getInt("ordre"));
                q.setTypeQuestion(rs.getString("type_question"));
                q.setCategorie(rs.getString("categorie"));
                q.setReponses(getReponsesByQuestion(q.getId()));
                questions.add(q);
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return questions;
    }

    private List<Reponse> getReponsesByQuestion(int questionId) {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE question_id = ? ORDER BY ordre ASC";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reponse r = new Reponse();
                r.setId(rs.getInt("id"));
                r.setTexte(rs.getString("texte"));
                r.setValeur(rs.getInt("valeur"));
                r.setOrdre(rs.getInt("ordre"));
                reponses.add(r);
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return reponses;
    }

    @Override public void ajouter(Question question) {}
    @Override public void modifier(Question question) {}
    @Override public void supprimer(int id) {}
    @Override public List<Question> afficherTout() { return new ArrayList<>(); }
    @Override public Question afficherUn(int id) { return null; }
}