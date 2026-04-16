package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.example.entities.Journal;
import org.example.entities.User;
import org.example.service.ContentValidationService;
import org.example.service.JournalService;

import java.io.IOException;
import java.sql.SQLException;

public class JournalEditController {
    @FXML private Label titleLabel;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> humeurCombo;
    @FXML private TextArea contenuArea;

    private final JournalService journalService = new JournalService();
    private User currentUser;
    private Journal currentJournal;

    @FXML
    public void initialize() {
        humeurCombo.getItems().addAll("heureux", "calme", "SOS", "en colere");
    }

    public void setData(User user, Journal journal) {
        currentUser = user;
        currentJournal = journal;
        titleLabel.setText("Modifier le journal du " + journal.getDateCreationFormatted());
        humeurCombo.setValue(journal.getHumeur());
        contenuArea.setText(journal.getContenu());
    }

    @FXML
    private void handleSave() {
        if (currentUser == null || currentJournal == null) {
            errorLabel.setText("Journal introuvable.");
            return;
        }

        String contenu = contenuArea.getText() == null ? "" : contenuArea.getText().trim();
        String humeur = humeurCombo.getValue();

        if (humeur == null || humeur.isBlank()) {
            errorLabel.setText("Veuillez choisir une humeur.");
            return;
        }
        String contentError = ContentValidationService.validateContent(contenu);
        if (contentError != null) {
            errorLabel.setText(contentError);
            return;
        }

        currentJournal.setHumeur(humeur);
        currentJournal.setContenu(contenu);

        try {
            journalService.update(currentJournal);
            new Alert(Alert.AlertType.INFORMATION, "Journal modifie avec succes.", ButtonType.OK).showAndWait();
            goBack();
        } catch (SQLException e) {
            errorLabel.setText("Erreur SQL: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        goBack();
    }

    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/journal.fxml"));
            Parent root = loader.load();
            JournalController controller = loader.getController();
            controller.setUserData(currentUser);
            contenuArea.getScene().setRoot(root);
        } catch (IOException e) {
            errorLabel.setText("Retour impossible.");
        }
    }
}
