package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.example.entities.AnalyseEmotionnelle;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;
import org.example.service.AnalyseEmotionnelleService;
import org.example.utils.JournalValidator;

import java.io.IOException;
import java.sql.SQLException;

public class AnalyseEmotionnelleEditController {
    @FXML private Label titleLabel;
    @FXML private Label journalLabel;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> etatCombo;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private TextArea declencheurArea;
    @FXML private TextArea conseilArea;

    private final AnalyseEmotionnelleService analyseService = new AnalyseEmotionnelleService();
    private User currentUser;
    private JournalAnalyseRow currentRow;

    @FXML
    public void initialize() {
        etatCombo.getItems().addAll("Apaise", "Fragile", "Stresse", "Anxieux", "En surcharge", "Optimiste");
        niveauCombo.getItems().addAll("Faible", "Modere", "Eleve");
    }

    public void setData(User user, JournalAnalyseRow row) {
        currentUser = user;
        currentRow = row;
        journalLabel.setText("Journal du " + row.getDateJournal() + " - " + row.getHumeur());

        AnalyseEmotionnelle analyse = row.getAnalyseEmotionnelle();
        if (analyse != null) {
            titleLabel.setText("Modifier l'analyse emotionnelle");
            etatCombo.setValue(analyse.getEtatEmotionnel());
            niveauCombo.setValue(analyse.getNiveau());
            declencheurArea.setText(analyse.getDeclencheur());
            conseilArea.setText(analyse.getConseil());
        } else {
            titleLabel.setText("Nouvelle analyse emotionnelle");
        }
    }

    @FXML
    private void handleSave() {
        if (currentUser == null || currentRow == null) {
            errorLabel.setText("Analyse introuvable.");
            return;
        }

        String validationError = validateForm();
        if (validationError != null) {
            errorLabel.setText(validationError);
            return;
        }

        try {
            AnalyseEmotionnelle analyse = analyseService.build(
                    currentRow.getJournalId(),
                    etatCombo.getValue(),
                    niveauCombo.getValue(),
                    declencheurArea.getText().trim(),
                    conseilArea.getText().trim()
            );

            if (currentRow.getAnalyseEmotionnelle() == null) {
                analyseService.create(analyse);
                new Alert(Alert.AlertType.INFORMATION, "Analyse emotionnelle ajoutee.", ButtonType.OK).showAndWait();
            } else {
                analyse.setId(currentRow.getAnalyseEmotionnelle().getId());
                analyseService.update(analyse);
                new Alert(Alert.AlertType.INFORMATION, "Analyse emotionnelle modifiee.", ButtonType.OK).showAndWait();
            }

            goBack();
        } catch (SQLException e) {
            errorLabel.setText("Erreur SQL: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        goBack();
    }

    private String validateForm() {
        if (etatCombo.getValue() == null || etatCombo.getValue().isBlank()) {
            return "Choisissez un etat emotionnel.";
        }
        if (niveauCombo.getValue() == null || niveauCombo.getValue().isBlank()) {
            return "Choisissez un niveau.";
        }
        String declencheurError = JournalValidator.validateContent(declencheurArea.getText());
        if (declencheurError != null) {
            return "Declencheur: " + declencheurError;
        }
        String conseilError = JournalValidator.validateContent(conseilArea.getText());
        if (conseilError != null) {
            return "Conseil: " + conseilError;
        }
        return null;
    }

    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/analyse_emotionnelle.fxml"));
            Parent root = loader.load();
            AnalyseEmotionnelleController controller = loader.getController();
            controller.setUserData(currentUser);
            conseilArea.getScene().setRoot(root);
        } catch (IOException e) {
            errorLabel.setText("Retour impossible.");
        }
    }
}
