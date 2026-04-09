package controllers;

import entities.Disponibilite;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ServiceDisponibilite;
import java.time.LocalTime;

public class DisponibiliteController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboHeureDebut;
    @FXML private ComboBox<String> comboHeureFin;
    @FXML private Label errorDate, errorHeureDebut, errorHeureFin;

    private ServiceDisponibilite sd = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        // Cette méthode est appelée AUTOMATIQUEMENT par JavaFX
        ObservableList<String> heures = FXCollections.observableArrayList();
        for (int i = 8; i <= 19; i++) {
            heures.add(String.format("%02d:00", i));
            heures.add(String.format("%02d:30", i));
        }

        // On vérifie si les composants sont bien liés avant de les remplir
        if (comboHeureDebut != null && comboHeureFin != null) {
            comboHeureDebut.setItems(heures);
            comboHeureFin.setItems(heures);
        } else {
            System.err.println("Erreur : Les ComboBox ne sont pas liées au FXML !");
        }
    }

    @FXML
    void enregistrer() {
        errorDate.setText("");
        errorHeureDebut.setText("");
        errorHeureFin.setText("");

        boolean valide = true;

        if (datePicker.getValue() == null) {
            errorDate.setText("La date est obligatoire.");
            valide = false;
        }

        if (comboHeureDebut.getValue() == null || comboHeureFin.getValue() == null) {
            errorHeureDebut.setText("Sélectionnez les heures.");
            valide = false;
        } else {
            LocalTime debut = LocalTime.parse(comboHeureDebut.getValue());
            LocalTime fin = LocalTime.parse(comboHeureFin.getValue());

            if (fin.isBefore(debut) || fin.equals(debut)) {
                errorHeureFin.setText("L'heure de fin doit être après le début.");
                valide = false;
            }

            if (valide) {
                Disponibilite d = new Disponibilite();
                d.setDate(datePicker.getValue());
                d.setHeureDebut(debut);
                d.setHeureFin(fin);

                if (sd.ajouter(d)) {
                    System.out.println("Succès : Créneau enregistré !");
                    datePicker.setValue(null);
                    comboHeureDebut.setValue(null);
                    comboHeureFin.setValue(null);
                } else {
                    errorDate.setText("Erreur SQL : Impossible d'enregistrer.");
                }
            }
        }
    }
}