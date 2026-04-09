package services;

import entities.Disponibilite;
import utils.DataSource;
import java.sql.*;

public class ServiceDisponibilite {
    private Connection connection = DataSource.getInstance().getConnection();

    public boolean ajouter(Disponibilite d) {
        // La requête SQL inclut maintenant est_libre
        String req = "INSERT INTO disponibilite (date, heure_debut, heure_fin, est_libre) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));
            ps.setBoolean(4, d.isEstLibre());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0; // Retourne vrai si une ligne a été ajoutée
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
            return false;
        }
    }
}