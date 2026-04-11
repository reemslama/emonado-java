package org.example.controllers;

import entities.TypeRendezVous;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ServiceTypeRendezVous;

public class AjouterTypeController {

    @FXML private TextField txtLibelle;
    @FXML private Label errorLibelle;

    private final ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    @FXML
    void ajouterType() {

        String libelle = txtLibelle.getText();

        // reset message
        errorLibelle.setText("");
        errorLibelle.setStyle("-fx-text-fill: red;");

        // validation
        if (libelle == null || libelle.trim().isEmpty()) {
            errorLibelle.setText("Champ obligatoire");
            return;
        }

        TypeRendezVous t = new TypeRendezVous();
        t.setLibelle(libelle.trim());

        boolean ok = st.ajouter(t);

        if (ok) {

            // ✅ message succès dans interface (pas popup)
            errorLibelle.setText("✔ Type ajouté avec succès !");
            errorLibelle.setStyle("-fx-text-fill: green;");

            txtLibelle.clear();

        } else {

            errorLibelle.setText("❌ Erreur lors de l'ajout");
            errorLibelle.setStyle("-fx-text-fill: red;");
        }
    }
}