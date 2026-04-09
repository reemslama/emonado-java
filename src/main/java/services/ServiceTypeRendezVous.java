package services;

import entities.TypeRendezVous;
import utils.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ServiceTypeRendezVous {

    // On récupère la connexion qu'on a établie dans DataSource
    private Connection connection = DataSource.getInstance().getConnection();

    // Méthode pour AJOUTER un type (CREATE)
    public void ajouter(TypeRendezVous t) {
        // Requête SQL (on utilise ? pour la sécurité contre les injections)
        String req = "INSERT INTO type_rendez_vous (libelle) VALUES (?)";

        try {
            PreparedStatement pst = connection.prepareStatement(req);
            pst.setString(1, t.getLibelle()); // On remplace le ? par le libellé

            pst.executeUpdate();
            System.out.println("Type ajouté avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout : " + e.getMessage());
        }
    }
}