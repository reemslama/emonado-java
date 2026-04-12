package org.example.controllers;

import entities.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import services.*;
import org.example.utils.DataSource;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RendezVousController {

    @FXML private Label welcomeLabel;

    @FXML private TextField txtAge;
    @FXML private TextField txtAdresse;

    @FXML private ComboBox<TypeRendezVous> comboTypes;
    @FXML private ComboBox<Disponibilite> comboDispos;

    @FXML private Label errorGlobal;

    ServiceRendezVous srv = new ServiceRendezVous();
    ServiceDisponibilite sd = new ServiceDisponibilite();
    ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    // =====================================================
    // INITIALISATION USER + COMBOS
    // =====================================================
    @FXML
    public void initialize() {

        comboTypes.setItems(FXCollections.observableArrayList(st.afficherTout()));
        comboDispos.setItems(FXCollections.observableArrayList(sd.getDisposLibres()));

        try {
            int userId = UserSession.getInstance().getId();

            Connection cnx = DataSource.getInstance().getConnection();

            String sql = "SELECT nom, prenom FROM user WHERE id = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");

                // ✔ AFFICHAGE PROPRE SUR UNE LIGNE
                welcomeLabel.setText("Bienvenue " + nom + " " + prenom);
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur user: " + e.getMessage());
        }
    }

    // =====================================================
    // AJOUT RDV
    // =====================================================
    @FXML
    public void validerRendezVous() {

        if (txtAge.getText().isEmpty() || txtAdresse.getText().isEmpty()) {
            showMessage("Champs obligatoires", "red");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(txtAge.getText());
        } catch (Exception e) {
            showMessage("Âge invalide", "red");
            return;
        }

        TypeRendezVous type = comboTypes.getValue();
        Disponibilite dispo = comboDispos.getValue();

        if (type == null || dispo == null) {
            showMessage("Sélection obligatoire", "red");
            return;
        }

        int userId = UserSession.getInstance().getId();

        RendezVous r = new RendezVous(
                age,
                txtAdresse.getText().trim(),
                type.getId(),
                dispo.getId(),
                userId
        );

        if (srv.ajouter(r)) {

            sd.rendreIndisponible(dispo.getId());

            showMessage("Rendez-vous ajouté !", "green");

            clearFields();

            comboDispos.setItems(
                    FXCollections.observableArrayList(sd.getDisposLibres())
            );

        } else {
            showMessage("Erreur lors de l'ajout", "red");
        }
    }

    // =====================================================
    // RETOUR DASHBOARD
    // =====================================================
    @FXML
    public void returnToDashboard() {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/patient_dashboard.fxml")
            );

            BorderPane mainContainer =
                    (BorderPane) txtAge.getScene().lookup("#mainContainer");

            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                txtAge.getScene().setRoot(root);
            }

        } catch (IOException e) {
            System.out.println("❌ Erreur retour dashboard: " + e.getMessage());
        }
    }

    // =====================================================
    // UI
    // =====================================================
    private void showMessage(String msg, String color) {
        errorGlobal.setText(msg);
        errorGlobal.setStyle("-fx-text-fill: " + color + ";");
    }

    private void clearFields() {
        txtAge.clear();
        txtAdresse.clear();
        comboTypes.getSelectionModel().clearSelection();
        comboDispos.getSelectionModel().clearSelection();
    }
}