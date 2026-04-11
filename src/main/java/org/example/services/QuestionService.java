package org.example.services;

import org.example.entities.Question;
import org.example.entities.Reponse;
import org.example.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IService<Question> {

    private Connection connection;

    public QuestionService() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Question q) {
        String sql = "INSERT INTO question (texte, ordre, type_question, categorie) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, q.getTexte());
            ps.setObject(2, q.getOrdre());
            ps.setString(3, q.getTypeQuestion());
            ps.setString(4, q.getCategorie());
            ps.executeUpdate();
            System.out.println("Question ajoutée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur ajouter question : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Question q) {
        String sql = "UPDATE question SET texte=?, ordre=?, type_question=?, categorie=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, q.getTexte());
            ps.setObject(2, q.getOrdre());
            ps.setString(3, q.getTypeQuestion());
            ps.setString(4, q.getCategorie());
            ps.setInt(5, q.getId());
            ps.executeUpdate();
            System.out.println("Question modifiée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur modifier question : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        // Supprimer d'abord les réponses liées
        String sqlReponses = "DELETE FROM reponse WHERE question_id=?";
        String sqlQuestion = "DELETE FROM question WHERE id=?";
        try {
            PreparedStatement ps1 = connection.prepareStatement(sqlReponses);
            ps1.setInt(1, id);
            ps1.executeUpdate();

            PreparedStatement ps2 = connection.prepareStatement(sqlQuestion);
            ps2.setInt(1, id);
            ps2.executeUpdate();
            System.out.println("Question supprimée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur supprimer question : " + e.getMessage());
        }
    }

    @Override
    public List<Question> afficherTout() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question ORDER BY ordre ASC";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTexte(rs.getString("texte"));
                q.setOrdre(rs.getInt("ordre"));
                q.setTypeQuestion(rs.getString("type_question"));
                q.setCategorie(rs.getString("categorie"));
                q.setReponses(getReponsesByQuestion(q.getId()));
                list.add(q);
            }
        } catch (SQLException e) {
            System.out.println("Erreur afficherTout : " + e.getMessage());
        }
        return list;
    }

    @Override
    public Question afficherUn(int id) {
        String sql = "SELECT * FROM question WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTexte(rs.getString("texte"));
                q.setOrdre(rs.getInt("ordre"));
                q.setTypeQuestion(rs.getString("type_question"));
                q.setCategorie(rs.getString("categorie"));
                q.setReponses(getReponsesByQuestion(q.getId()));
                return q;
            }
        } catch (SQLException e) {
            System.out.println("Erreur afficherUn : " + e.getMessage());
        }
        return null;
    }

    public List<Question> rechercher(String keyword) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE texte LIKE ? ORDER BY ordre ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTexte(rs.getString("texte"));
                q.setOrdre(rs.getInt("ordre"));
                q.setTypeQuestion(rs.getString("type_question"));
                q.setCategorie(rs.getString("categorie"));
                q.setReponses(getReponsesByQuestion(q.getId()));
                list.add(q);
            }
        } catch (SQLException e) {
            System.out.println("Erreur rechercher : " + e.getMessage());
        }
        return list;
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
}