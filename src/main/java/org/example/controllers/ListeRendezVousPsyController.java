package org.example.controllers;

import entities.Disponibilite;
import entities.RendezVous;
import entities.RendezVousPsy;
import entities.TypeRendezVous;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import services.ServiceDisponibilite;
import services.ServiceRendezVous;
import services.ServiceTypeRendezVous;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

public class ListeRendezVousPsyController {

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("[A-Za-z0-9À-ÿ'.,\\-\\s]{5,150}");

    @FXML private TableView<RendezVousPsy> tableRdv;
    @FXML private TableColumn<RendezVousPsy, String> colNom;
    @FXML private TableColumn<RendezVousPsy, String> colPrenom;
    @FXML private TableColumn<RendezVousPsy, String> colType;
    @FXML private TableColumn<RendezVousPsy, String> colDate;
    @FXML private TableColumn<RendezVousPsy, String> colHeure;
    @FXML private TableColumn<RendezVousPsy, String> colAge;
    @FXML private TableColumn<RendezVousPsy, String> colAdresse;
    @FXML private Label errorGlobal;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final ServiceRendezVous srv = new ServiceRendezVous();
    private final ServiceTypeRendezVous st = new ServiceTypeRendezVous();
    private final ServiceDisponibilite sd = new ServiceDisponibilite();

    private RendezVousPsy selectedRdv;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNom()));
        colPrenom.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPrenom()));
        colType.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTypeRdv()));
        colDate.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getDate()));
        colHeure.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getHeureDebut() + " - " + d.getValue().getHeureFin()));
        colAge.setCellValueFactory(d -> new ReadOnlyStringWrapper(String.valueOf(d.getValue().getAge())));
        colAdresse.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getAdresse()));

        refreshTable();

        tableRdv.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRdv = newValue;
            errorGlobal.setText("");
            updateButtons();
        });

        updateButtons();
    }

    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) tableRdv.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                tableRdv.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard Psy : " + e.getMessage());
        }
    }

    @FXML
    public void modifierRendezVous() {
        if (selectedRdv == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }

        Optional<RendezVous> updated = showEditDialog(selectedRdv);
        if (updated.isEmpty()) {
            return;
        }

        int ancienneDispoId = selectedRdv.getDispoId();
        RendezVous rendezVous = updated.get();
        rendezVous.setId(selectedRdv.getId());
        rendezVous.setUserId(selectedRdv.getUserId());

        if (srv.modifier(rendezVous)) {
            if (ancienneDispoId != rendezVous.getDispoId()) {
                sd.rendreDisponible(ancienneDispoId);
                sd.rendreIndisponible(rendezVous.getDispoId());
            }
            showMessage("Rendez-vous modifie", "green");
            refreshTable();
        } else {
            showMessage("Modification impossible", "red");
        }
    }

    @FXML
    public void supprimerRendezVous() {
        if (selectedRdv == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }

        if (srv.supprimer(selectedRdv.getId())) {
            sd.rendreDisponible(selectedRdv.getDispoId());
            showMessage("Rendez-vous supprime", "green");
            refreshTable();
        } else {
            showMessage("Suppression impossible", "red");
        }
    }

    @FXML
    public void resetForm() {
        selectedRdv = null;
        tableRdv.getSelectionModel().clearSelection();
        errorGlobal.setText("");
        updateButtons();
    }

    private void refreshTable() {
        tableRdv.setItems(FXCollections.observableArrayList(srv.getRendezVousForPsy()));
    }

    private Optional<RendezVous> showEditDialog(RendezVousPsy source) {
        Dialog<RendezVous> dialog = new Dialog<>();
        dialog.setTitle("Modifier rendez-vous");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField ageField = new TextField(String.valueOf(source.getAge()));
        TextField adresseField = new TextField(source.getAdresse());
        ComboBox<TypeRendezVous> typeCombo = new ComboBox<>(FXCollections.observableArrayList(st.afficherTout()));
        ComboBox<Disponibilite> dispoCombo = new ComboBox<>(FXCollections.observableArrayList(sd.getDisposLibresIncluding(source.getDispoId())));
        Label errorLabel = new Label();

        typeCombo.getSelectionModel().select(
                typeCombo.getItems().stream().filter(type -> type.getId() == source.getTypeId()).findFirst().orElse(null)
        );
        dispoCombo.getSelectionModel().select(
                dispoCombo.getItems().stream().filter(dispo -> dispo.getId() == source.getDispoId()).findFirst().orElse(null)
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Age"), 0, 0);
        grid.add(ageField, 1, 0);
        grid.add(new Label("Adresse"), 0, 1);
        grid.add(adresseField, 1, 1);
        grid.add(new Label("Type"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Creneau"), 0, 3);
        grid.add(dispoCombo, 1, 3);
        grid.add(errorLabel, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String validationError = validateFields(ageField.getText(), adresseField.getText(), typeCombo.getValue(), dispoCombo.getValue());
            if (validationError != null) {
                errorLabel.setText(validationError);
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            RendezVous rendezVous = new RendezVous();
            rendezVous.setAge(Integer.parseInt(ageField.getText().trim()));
            rendezVous.setAdresse(adresseField.getText().trim());
            rendezVous.setTypeId(typeCombo.getValue().getId());
            rendezVous.setDispoId(dispoCombo.getValue().getId());
            return rendezVous;
        });

        return dialog.showAndWait();
    }

    private String validateFields(String ageText, String adresse, TypeRendezVous type, Disponibilite dispo) {
        if (ageText == null || ageText.isBlank() || adresse == null || adresse.isBlank()) {
            return "Champs obligatoires";
        }

        try {
            int age = Integer.parseInt(ageText.trim());
            if (age < 5 || age > 120) {
                return "Age doit etre entre 5 et 120";
            }
        } catch (Exception e) {
            return "Age invalide";
        }

        if (!ADDRESS_PATTERN.matcher(adresse.trim()).matches()) {
            return "Adresse invalide ou trop courte";
        }

        if (type == null || dispo == null) {
            return "Selection obligatoire";
        }

        return null;
    }

    private void updateButtons() {
        if (btnModifier != null) {
            btnModifier.setDisable(false);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setDisable(false);
        }
    }

    private void showMessage(String msg, String color) {
        errorGlobal.setText(msg);
        errorGlobal.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}
