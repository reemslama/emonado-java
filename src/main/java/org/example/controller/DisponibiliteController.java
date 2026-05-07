package org.example.controller;

import entities.Disponibilite;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import org.example.entities.User;
import org.example.utils.UserSession;
import services.ServiceDisponibilite;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DisponibiliteController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
    @FXML private Label totalSlotsLabel;
    @FXML private Label availableSlotsLabel;
    @FXML private Label reservedSlotsLabel;
    @FXML private Label formTitleLabel;
    @FXML private Label selectedHintLabel;

    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();
    private Disponibilite selectedDisponibilite;
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance();
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

        colDate.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getDate() == null ? "-" : DATE_FORMATTER.format(data.getValue().getDate())));
        colDebut.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getHeureDebut() == null ? "-" : TIME_FORMATTER.format(data.getValue().getHeureDebut())));
        colFin.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getHeureFin() == null ? "-" : TIME_FORMATTER.format(data.getValue().getHeureFin())));
        colEtat.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isLibre() ? "Libre" : "Reserve"));
        colEtat.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if ("Libre".equals(item)) {
                    setStyle("-fx-text-fill: #159a73; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #c27a18; -fx-font-weight: bold;");
                }
            }
        });
        tableDisponibilites.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                if (item.isLibre()) {
                    setStyle("-fx-background-color: rgba(21,154,115,0.04);");
                } else {
                    setStyle("-fx-background-color: rgba(217,119,6,0.05);");
                }
            }
        });

        tableDisponibilites.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedDisponibilite = newValue;
            populateForm(newValue);
            updateButtons();
        });

        clearMessages();
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
        Disponibilite disponibilite = validateForm();
        if (disponibilite == null) {
            return;
        }
        if (serviceDisponibilite.ajouter(disponibilite)) {
            showSuccess("Le creneau a ete ajoute avec succes.");
            resetForm();
            refreshTable();
        } else {
            setError(errorGlobal, "Impossible d'ajouter cette disponibilite.");
        }
    }

    @FXML
    void modifier() {
        if (selectedDisponibilite == null) {
            setError(errorGlobal, "Selectionnez une disponibilite a modifier.");
            return;
        }
        if (!selectedDisponibilite.isLibre()) {
            setError(errorGlobal, "Impossible de modifier un creneau deja reserve.");
            return;
        }

        Disponibilite disponibilite = validateForm();
        if (disponibilite == null) {
            return;
        }
        disponibilite.setId(selectedDisponibilite.getId());
        if (serviceDisponibilite.modifier(disponibilite)) {
            showSuccess("Le creneau a ete mis a jour.");
            resetForm();
            refreshTable();
        } else {
            setError(errorGlobal, "La modification du creneau a echoue.");
        }
    }

    @FXML
    void supprimer() {
        if (selectedDisponibilite == null) {
            setError(errorGlobal, "Selectionnez une disponibilite a supprimer.");
            return;
        }
        if (!selectedDisponibilite.isLibre()) {
            setError(errorGlobal, "Impossible de supprimer un creneau deja reserve.");
            return;
        }

        if (serviceDisponibilite.supprimer(selectedDisponibilite.getId())) {
            showSuccess("Le creneau a ete supprime.");
            resetForm();
            refreshTable();
        } else {
            setError(errorGlobal, "La suppression du creneau a echoue.");
        }
    }

    @FXML
    void resetForm() {
        selectedDisponibilite = null;
        datePicker.setValue(null);
        comboHeureDebut.setValue(null);
        comboHeureFin.setValue(null);
        checkLibre.setSelected(true);
        clearMessages();
        tableDisponibilites.getSelectionModel().clearSelection();
        updateButtons();
    }

    private Disponibilite validateForm() {
        clearMessages();

        boolean valid = true;
        if (datePicker.getValue() == null) {
            setError(errorDate, "La date est obligatoire.");
            valid = false;
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            setError(errorDate, "La date doit etre future ou egale a aujourd'hui.");
            valid = false;
        }
        if (comboHeureDebut.getValue() == null) {
            setError(errorHeureDebut, "L'heure de debut est obligatoire.");
            valid = false;
        }
        if (comboHeureFin.getValue() == null) {
            setError(errorHeureFin, "L'heure de fin est obligatoire.");
            valid = false;
        }
        if (!valid) {
            return null;
        }

        try {
            LocalTime debut = LocalTime.parse(comboHeureDebut.getValue());
            LocalTime fin = LocalTime.parse(comboHeureFin.getValue());
            if (!fin.isAfter(debut)) {
                setError(errorHeureFin, "L'heure de fin doit etre apres l'heure de debut.");
                return null;
            }

            Disponibilite disponibilite = new Disponibilite();
            disponibilite.setPsychologueId(getCurrentPsychologueId());
            disponibilite.setDate(datePicker.getValue());
            disponibilite.setHeureDebut(debut);
            disponibilite.setHeureFin(fin);
            disponibilite.setLibre(checkLibre.isSelected());

            Integer excludedId = selectedDisponibilite == null ? null : selectedDisponibilite.getId();
            if (serviceDisponibilite.existeChevauchement(disponibilite, excludedId)) {
                setError(errorGlobal, "Ce creneau chevauche une disponibilite existante.");
                return null;
            }
            return disponibilite;
        } catch (Exception e) {
            setError(errorHeureDebut, "Le format d'heure est invalide.");
            return null;
        }
    }

    private void populateForm(Disponibilite disponibilite) {
        if (disponibilite == null) {
            return;
        }
        datePicker.setValue(disponibilite.getDate());
        comboHeureDebut.setValue(disponibilite.getHeureDebut() == null ? null : TIME_FORMATTER.format(disponibilite.getHeureDebut()));
        comboHeureFin.setValue(disponibilite.getHeureFin() == null ? null : TIME_FORMATTER.format(disponibilite.getHeureFin()));
        checkLibre.setSelected(disponibilite.isLibre());
        msgSuccess.setText("");
        errorGlobal.setText("");
    }

    private void refreshTable() {
        var disponibilites = FXCollections.observableArrayList(serviceDisponibilite.afficherToutByPsychologue(getCurrentPsychologueId()));
        tableDisponibilites.setItems(disponibilites);

        long total = disponibilites.size();
        long libres = disponibilites.stream().filter(Disponibilite::isLibre).count();
        long reservees = total - libres;

        if (totalSlotsLabel != null) {
            totalSlotsLabel.setText(String.valueOf(total));
        }
        if (availableSlotsLabel != null) {
            availableSlotsLabel.setText(String.valueOf(libres));
        }
        if (reservedSlotsLabel != null) {
            reservedSlotsLabel.setText(String.valueOf(reservees));
        }
    }

    private void updateButtons() {
        if (btnEnregistrer != null) {
            btnEnregistrer.setDisable(false);
        }
        if (btnModifier != null) {
            btnModifier.setDisable(selectedDisponibilite == null || !selectedDisponibilite.isLibre());
        }
        if (btnSupprimer != null) {
            btnSupprimer.setDisable(selectedDisponibilite == null || !selectedDisponibilite.isLibre());
        }
        if (formTitleLabel != null) {
            formTitleLabel.setText(selectedDisponibilite == null ? "Ajouter un creneau" : "Modifier le creneau");
        }
        if (selectedHintLabel != null) {
            if (selectedDisponibilite == null) {
                selectedHintLabel.setText("Definissez un nouveau creneau disponible pour vos patients.");
            } else if (selectedDisponibilite.isLibre()) {
                selectedHintLabel.setText("Le creneau selectionne est libre. Vous pouvez le modifier ou le supprimer.");
            } else {
                selectedHintLabel.setText("Le creneau selectionne est deja reserve. Il reste visible mais verrouille.");
            }
        }
    }

    private void setError(Label label, String message) {
        label.setText(message);
        if (label == errorGlobal) {
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void showSuccess(String message) {
        msgSuccess.setText(message);
        msgSuccess.setVisible(true);
        msgSuccess.setManaged(true);
    }

    private void clearMessages() {
        errorDate.setText("");
        errorHeureDebut.setText("");
        errorHeureFin.setText("");
        errorGlobal.setText("");
        msgSuccess.setText("");
        msgSuccess.setVisible(false);
        msgSuccess.setManaged(false);
        errorGlobal.setVisible(false);
        errorGlobal.setManaged(false);
    }

    private int getCurrentPsychologueId() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        return currentUser.getId();
    }
}
