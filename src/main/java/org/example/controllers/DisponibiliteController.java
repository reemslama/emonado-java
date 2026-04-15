package org.example.controllers;

import entities.Disponibilite;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.DateCell;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import services.ServiceDisponibilite;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DisponibiliteController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboHeureDebut;
    @FXML private ComboBox<String> comboHeureFin;
    @FXML private CheckBox checkLibre;
    @FXML private Label errorDate;
    @FXML private Label errorHeureDebut;
    @FXML private Label errorHeureFin;
    @FXML private Label msgSuccess;
    @FXML private Label errorGlobal;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private TableView<Disponibilite> tableDisponibilites;
    @FXML private TableColumn<Disponibilite, String> colDate;
    @FXML private TableColumn<Disponibilite, String> colDebut;
    @FXML private TableColumn<Disponibilite, String> colFin;
    @FXML private TableColumn<Disponibilite, String> colEtat;

    private final ServiceDisponibilite sd = new ServiceDisponibilite();
    private Disponibilite selectedDisponibilite;

    @FXML
    public void initialize() {
        var heures = FXCollections.<String>observableArrayList();
        for (int i = 8; i <= 19; i++) {
            heures.add(String.format("%02d:00", i));
            heures.add(String.format("%02d:30", i));
        }

        comboHeureDebut.setItems(heures);
        comboHeureFin.setItems(heures);
        comboHeureDebut.setEditable(false);
        comboHeureFin.setEditable(false);
        checkLibre.setSelected(true);
        configureDatePicker();

        colDate.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getDate())));
        colDebut.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getHeureDebut())));
        colFin.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getHeureFin())));
        colEtat.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isLibre() ? "Libre" : "Reservee"));

        tableDisponibilites.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedDisponibilite = newValue;
            populateForm(newValue);
            updateButtons();
        });

        refreshTable();
        updateButtons();
    }

    private void configureDatePicker() {
        datePicker.setEditable(false);
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FORMATTER.format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                return (value == null || value.isBlank()) ? null : LocalDate.parse(value, DATE_FORMATTER);
            }
        });
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                boolean disabled = empty || item == null || item.isBefore(LocalDate.now());
                setDisable(disabled);
                if (disabled) {
                    setStyle("-fx-background-color: #f1f1f1; -fx-text-fill: #999999;");
                }
            }
        });
    }

    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) datePicker.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                datePicker.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur de chargement du dashboard : " + e.getMessage());
        }
    }

    @FXML
    void enregistrer() {
        Disponibilite d = validateForm();
        if (d == null) {
            return;
        }

        if (sd.ajouter(d)) {
            showSuccess("Disponibilite ajoutee");
            resetForm();
            refreshTable();
        } else {
            setError(errorGlobal, "Erreur SQL");
        }
    }

    @FXML
    void modifier() {
        if (selectedDisponibilite == null) {
            setError(errorGlobal, "Selectionnez une disponibilite");
            return;
        }

        Disponibilite d = validateForm();
        if (d == null) {
            return;
        }

        d.setId(selectedDisponibilite.getId());
        if (sd.modifier(d)) {
            showSuccess("Disponibilite modifiee");
            resetForm();
            refreshTable();
        } else {
            setError(errorGlobal, "Modification impossible");
        }
    }

    @FXML
    void supprimer() {
        if (selectedDisponibilite == null) {
            setError(errorGlobal, "Selectionnez une disponibilite");
            return;
        }

        if (sd.supprimer(selectedDisponibilite.getId())) {
            showSuccess("Disponibilite supprimee");
            resetForm();
            refreshTable();
        } else {
            setError(errorGlobal, "Suppression impossible");
        }
    }

    @FXML
    void resetForm() {
        selectedDisponibilite = null;
        datePicker.setValue(null);
        comboHeureDebut.setValue(null);
        comboHeureFin.setValue(null);
        checkLibre.setSelected(true);
        errorDate.setText("");
        errorHeureDebut.setText("");
        errorHeureFin.setText("");
        errorGlobal.setText("");
        tableDisponibilites.getSelectionModel().clearSelection();
        updateButtons();
    }

    private Disponibilite validateForm() {
        errorDate.setText("");
        errorHeureDebut.setText("");
        errorHeureFin.setText("");
        errorGlobal.setText("");
        msgSuccess.setText("");

        boolean valid = true;
        if (datePicker.getValue() == null) {
            setError(errorDate, "Champ obligatoire");
            valid = false;
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            setError(errorDate, "Date passee interdite");
            valid = false;
        }
        if (comboHeureDebut.getValue() == null) {
            setError(errorHeureDebut, "Champ obligatoire");
            valid = false;
        }
        if (comboHeureFin.getValue() == null) {
            setError(errorHeureFin, "Champ obligatoire");
            valid = false;
        }
        if (!valid) {
            return null;
        }

        try {
            LocalTime debut = LocalTime.parse(comboHeureDebut.getValue());
            LocalTime fin = LocalTime.parse(comboHeureFin.getValue());
            if (!fin.isAfter(debut)) {
                setError(errorHeureFin, "Fin apres debut obligatoire");
                return null;
            }

            Disponibilite d = new Disponibilite();
            d.setDate(datePicker.getValue());
            d.setHeureDebut(debut);
            d.setHeureFin(fin);
            d.setLibre(checkLibre.isSelected());

            Integer excludedId = selectedDisponibilite == null ? null : selectedDisponibilite.getId();
            if (sd.existeChevauchement(d, excludedId)) {
                setError(errorGlobal, "Ce creneau chevauche une disponibilite existante");
                return null;
            }
            return d;
        } catch (Exception e) {
            setError(errorHeureDebut, "Format heure invalide");
            return null;
        }
    }

    private void populateForm(Disponibilite disponibilite) {
        if (disponibilite == null) {
            return;
        }
        datePicker.setValue(disponibilite.getDate());
        comboHeureDebut.setValue(String.valueOf(disponibilite.getHeureDebut()));
        comboHeureFin.setValue(String.valueOf(disponibilite.getHeureFin()));
        checkLibre.setSelected(disponibilite.isLibre());
        msgSuccess.setText("");
        errorGlobal.setText("");
    }

    private void refreshTable() {
        tableDisponibilites.setItems(FXCollections.observableArrayList(sd.afficherTout()));
    }

    private void updateButtons() {
        if (btnEnregistrer != null) {
            btnEnregistrer.setDisable(false);
        }
        if (btnModifier != null) {
            btnModifier.setDisable(false);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setDisable(false);
        }
    }

    private void setError(Label label, String msg) {
        label.setText(msg);
        label.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void showSuccess(String msg) {
        msgSuccess.setText(msg);
        msgSuccess.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }
}
