package org.example.controllers;

import entities.TypeRendezVous;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import services.ServiceTypeRendezVous;

import java.io.IOException;

public class AjouterTypeController {

    @FXML private TextField txtLibelle;
    @FXML private Label errorLibelle;

    private final ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    /**
     * Méthode pour retourner au Dashboard du Patient
     */
    @FXML
    private void returnToDashboard() {
        try {
            // Chargement du dashboard Patient
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));

            // Recherche du conteneur principal BorderPane
            BorderPane mainContainer = (BorderPane) txtLibelle.getScene().lookup("#mainContainer");

            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                // Fallback si le mainContainer n'est pas trouvé
                txtLibelle.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void ajouterType() {
        String libelle = txtLibelle.getText();

        // Reset message
        errorLibelle.setText("");
        errorLibelle.setStyle("-fx-text-fill: red;");

        // Validation
        if (libelle == null || libelle.trim().isEmpty()) {
            errorLibelle.setText("Champ obligatoire");
            errorLibelle.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        TypeRendezVous t = new TypeRendezVous();
        t.setLibelle(libelle.trim());

        boolean ok = st.ajouter(t);

        if (ok) {
            // ✅ Message succès
            errorLibelle.setText("✔ Type ajouté avec succès !");
            errorLibelle.setStyle("-fx-text-fill: green;");
            txtLibelle.clear();
        } else {
            errorLibelle.setText("❌ Erreur lors de l'ajout");
            errorLibelle.setStyle("-fx-text-fill: red;");
        }
    }
}