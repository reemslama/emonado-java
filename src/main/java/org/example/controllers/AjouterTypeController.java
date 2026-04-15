package org.example.controllers;

import entities.TypeRendezVous;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import services.ServiceTypeRendezVous;

import java.io.IOException;

public class AjouterTypeController {

    @FXML private TextField txtLibelle;
    @FXML private Label errorLibelle;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private TableView<TypeRendezVous> tableTypes;
    @FXML private TableColumn<TypeRendezVous, String> colLibelle;

    private final ServiceTypeRendezVous st = new ServiceTypeRendezVous();
    private TypeRendezVous selectedType;

    @FXML
    public void initialize() {
        colLibelle.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getLibelle()));
        tableTypes.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedType = newValue;
            if (newValue != null) {
                txtLibelle.setText(newValue.getLibelle());
            }
            updateButtons();
        });
        refreshTable();
        updateButtons();
    }

    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) txtLibelle.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                txtLibelle.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard : " + e.getMessage());
        }
    }

    @FXML
    public void ajouterType() {
        String libelle = validateLibelle();
        if (libelle == null) {
            return;
        }

        TypeRendezVous t = new TypeRendezVous();
        t.setLibelle(libelle);
        if (st.ajouter(t)) {
            showMessage("Type ajoute avec succes", "green");
            resetForm();
            refreshTable();
        } else {
            showMessage("Erreur lors de l'ajout", "red");
        }
    }

    @FXML
    public void modifierType() {
        if (selectedType == null) {
            showMessage("Selectionnez un type", "red");
            return;
        }

        String libelle = validateLibelle();
        if (libelle == null) {
            return;
        }

        selectedType.setLibelle(libelle);
        if (st.modifier(selectedType)) {
            showMessage("Type modifie avec succes", "green");
            resetForm();
            refreshTable();
        } else {
            showMessage("Erreur lors de la modification", "red");
        }
    }

    @FXML
    public void supprimerType() {
        if (selectedType == null) {
            showMessage("Selectionnez un type", "red");
            return;
        }

        if (st.supprimer(selectedType.getId())) {
            showMessage("Type supprime avec succes", "green");
            resetForm();
            refreshTable();
        } else {
            showMessage("Suppression impossible", "red");
        }
    }

    @FXML
    public void resetForm() {
        selectedType = null;
        txtLibelle.clear();
        tableTypes.getSelectionModel().clearSelection();
        errorLibelle.setText("");
        updateButtons();
    }

    private String validateLibelle() {
        String libelle = txtLibelle.getText();
        if (libelle == null || libelle.trim().isEmpty()) {
            showMessage("Champ obligatoire", "red");
            return null;
        }

        libelle = libelle.trim();
        if (libelle.length() < 3 || libelle.length() > 60) {
            showMessage("Libelle doit contenir entre 3 et 60 caracteres", "red");
            return null;
        }
        if (!libelle.matches("[A-Za-zÀ-ÿ0-9'()\\-\\s]+")) {
            showMessage("Libelle contient des caracteres invalides", "red");
            return null;
        }

        Integer excludedId = selectedType == null ? null : selectedType.getId();
        if (st.existeLibelle(libelle, excludedId)) {
            showMessage("Ce type existe deja", "red");
            return null;
        }

        return libelle;
    }

    private void refreshTable() {
        tableTypes.setItems(FXCollections.observableArrayList(st.afficherTout()));
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

    private void showMessage(String message, String color) {
        errorLibelle.setText(message);
        errorLibelle.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}
