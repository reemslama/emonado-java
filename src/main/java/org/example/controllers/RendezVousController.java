package org.example.controllers;

import entities.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.*;

public class RendezVousController {

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtAge;
    @FXML private TextField txtAdresse;

    @FXML private ComboBox<TypeRendezVous> comboTypes;
    @FXML private ComboBox<Disponibilite> comboDispos;

    @FXML private Label errorGlobal;

    ServiceRendezVous srv = new ServiceRendezVous();
    ServiceDisponibilite sd = new ServiceDisponibilite();
    ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    @FXML
    public void initialize() {
        comboTypes.setItems(FXCollections.observableArrayList(st.afficherTout()));
        comboDispos.setItems(FXCollections.observableArrayList(sd.getDisposLibres()));
    }

    @FXML
    void validerRendezVous() {

        if (txtNom.getText().isEmpty()
                || txtPrenom.getText().isEmpty()
                || txtAge.getText().isEmpty()
                || txtAdresse.getText().isEmpty()) {

            showMessage("Champs obligatoires", "red");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(txtAge.getText());
        } catch (NumberFormatException e) {
            showMessage("Âge invalide", "red");
            return;
        }

        TypeRendezVous type = comboTypes.getValue();
        Disponibilite dispo = comboDispos.getValue();

        if (type == null || dispo == null) {
            showMessage("Sélection obligatoire", "red");
            return;
        }

        RendezVous r = new RendezVous(
                txtNom.getText().trim(),
                txtPrenom.getText().trim(),
                age,
                txtAdresse.getText().trim(),
                type.getId(),
                dispo.getId()
        );

        if (srv.ajouter(r)) {

            sd.rendreIndisponible(dispo.getId());

            showMessage("Rendez-vous ajouté !", "green");

            clearFields();

        } else {
            showMessage("Erreur lors de l'ajout", "red");
        }
    }

    private void showMessage(String msg, String color) {
        errorGlobal.setText(msg);
        errorGlobal.setStyle("-fx-text-fill: " + color + ";");
    }

    private void clearFields() {
        txtNom.clear();
        txtPrenom.clear();
        txtAge.clear();
        txtAdresse.clear();
        comboTypes.getSelectionModel().clearSelection();
        comboDispos.getSelectionModel().clearSelection();
    }
}