package org.example.services;

import org.example.entities.Reponse;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements IService<Reponse> {

    private Connection connection;

    public ReponseService() {
        connection = MyDatabase.getInstance().getConnection();
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
            System.out.println("Erreur : " + e.getMessage());
        }
        return reponses;
    }

    @Override public void ajouter(Reponse reponse) {}
    @Override public void modifier(Reponse reponse) {}
    @Override public void supprimer(int id) {}
    @Override public List<Reponse> afficherTout() { return new ArrayList<>(); }
    @Override public Reponse afficherUn(int id) { return null; }
}