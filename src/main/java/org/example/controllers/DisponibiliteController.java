package org.example.controllers;

import entities.Disponibilite;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import services.ServiceDisponibilite;

import java.io.IOException;
import java.time.LocalTime;

public class DisponibiliteController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboHeureDebut;
    @FXML private ComboBox<String> comboHeureFin;

    @FXML private Label errorDate;
    @FXML private Label errorHeureDebut;
    @FXML private Label errorHeureFin;

    @FXML private Label msgSuccess;

    private final ServiceDisponibilite sd = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        var heures = FXCollections.<String>observableArrayList();

        for (int i = 8; i <= 19; i++) {
            heures.add(String.format("%02d:00", i));
            heures.add(String.format("%02d:30", i));
        }

        comboHeureDebut.setItems(heures);
        comboHeureFin.setItems(heures);
    }

    /**
     * Méthode liée au bouton "← Retour" dans le FXML
     */
    @FXML
    private void returnToDashboard() {
        try {
            // Chargement de la vue du dashboard Psy
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));

            // Accès au conteneur principal BorderPane pour changer le centre
            BorderPane mainContainer = (BorderPane) datePicker.getScene().lookup("#mainContainer");

            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                // Si le mainContainer n'est pas trouvé, on change toute la racine
                datePicker.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur de chargement du dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================
    // STYLE ROUGE
    // =========================
    private void setError(Label label, String msg) {
        label.setText(msg);
        label.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    @FXML
    void enregistrer() {
        // reset messages
        errorDate.setText("");
        errorHeureDebut.setText("");
        errorHeureFin.setText("");
        msgSuccess.setText("");

        boolean valid = true;

        // DATE
        if (datePicker.getValue() == null) {
            setError(errorDate, "Champ obligatoire");
            valid = false;
        }

        // HEURE DEBUT
        if (comboHeureDebut.getValue() == null) {
            setError(errorHeureDebut, "Champ obligatoire");
            valid = false;
        }

        // HEURE FIN
        if (comboHeureFin.getValue() == null) {
            setError(errorHeureFin, "Champ obligatoire");
            valid = false;
        }

        if (!valid) return;

        try {
            LocalTime debut = LocalTime.parse(comboHeureDebut.getValue());
            LocalTime fin = LocalTime.parse(comboHeureFin.getValue());

            if (!fin.isAfter(debut)) {
                setError(errorHeureFin, "Fin doit être après début");
                return;
            }

            Disponibilite d = new Disponibilite();
            d.setDate(datePicker.getValue());
            d.setHeureDebut(debut);
            d.setHeureFin(fin);

            if (sd.ajouter(d)) {
                msgSuccess.setText("✔ Disponibilité ajoutée !");
                msgSuccess.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                datePicker.setValue(null);
                comboHeureDebut.setValue(null);
                comboHeureFin.setValue(null);

                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(e -> msgSuccess.setText(""));
                pause.play();
            } else {
                setError(msgSuccess, "Erreur SQL");
            }

        } catch (Exception e) {
            setError(errorHeureDebut, "Format heure invalide");
        }
    }
}