package org.example.services;

import org.example.entities.Reponse;
import org.example.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements IService<Reponse> {

    private Connection connection;

    public ReponseService() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Reponse r) {
        String sql = "INSERT INTO reponse (texte, valeur, ordre, question_id) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, r.getTexte());
            ps.setInt(2, r.getValeur());
            ps.setObject(3, r.getOrdre());
            ps.setInt(4, r.getQuestion().getId());
            ps.executeUpdate();
            System.out.println("Réponse ajoutée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur ajouter réponse : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Reponse r) {
        String sql = "UPDATE reponse SET texte=?, valeur=?, ordre=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, r.getTexte());
            ps.setInt(2, r.getValeur());
            ps.setObject(3, r.getOrdre());
            ps.setInt(4, r.getId());
            ps.executeUpdate();
            System.out.println("Réponse modifiée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur modifier réponse : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM reponse WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Réponse supprimée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur supprimer réponse : " + e.getMessage());
        }
    }

    @Override
    public List<Reponse> afficherTout() {
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse ORDER BY ordre ASC";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Reponse r = new Reponse();
                r.setId(rs.getInt("id"));
                r.setTexte(rs.getString("texte"));
                r.setValeur(rs.getInt("valeur"));
                r.setOrdre(rs.getInt("ordre"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("Erreur afficherTout réponses : " + e.getMessage());
        }
        return list;
    }

    @Override
    public Reponse afficherUn(int id) {
        String sql = "SELECT * FROM reponse WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Reponse r = new Reponse();
                r.setId(rs.getInt("id"));
                r.setTexte(rs.getString("texte"));
                r.setValeur(rs.getInt("valeur"));
                r.setOrdre(rs.getInt("ordre"));
                return r;
            }
        } catch (SQLException e) {
            System.out.println("Erreur afficherUn réponse : " + e.getMessage());
        }
        return null;
    }

    public List<Reponse> getReponsesByQuestion(int questionId) {
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
            System.out.println("Erreur getReponsesByQuestion : " + e.getMessage());
        }
        return reponses;
    }
}