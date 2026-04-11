package org.example.controllers;

import entities.Disponibilite;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import services.ServiceDisponibilite;

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