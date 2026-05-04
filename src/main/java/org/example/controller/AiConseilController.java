package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;
import org.example.service.AiJournalAnalysisService;
import org.example.utils.UserSession;

import java.io.IOException;

public class AiConseilController {
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label statusLabel;
    @FXML private TextArea journalTextArea;
    @FXML private TextArea resultTextArea;

    private final AiJournalAnalysisService aiService = new AiJournalAnalysisService();
    private User viewerUser;
    private User patient;
    private JournalAnalyseRow row;

    public void setData(User viewerUser, User patient, JournalAnalyseRow row) {
        this.viewerUser = viewerUser;
        this.patient = patient;
        this.row = row;
        titleLabel.setText("Conseil AI - " + patientName());
        metaLabel.setText(safe(row.getDateJournal()) + " | Humeur: " + safe(row.getHumeur()));
        journalTextArea.setText(safe(row.getContenuComplet()));
        resultTextArea.setText("");
        runAdvice();
    }

    @FXML
    private void runAdvice() {
        if (row == null) {
            statusLabel.setText("Aucun journal selectionne.");
            return;
        }
        statusLabel.setText("Generation du conseil en cours...");
        resultTextArea.setText("");
        new Thread(() -> {
            AiJournalAnalysisService.AiResult result = aiService.generateAdvice(patient, row);
            Platform.runLater(() -> {
                statusLabel.setText("Source: " + result.provider());
                resultTextArea.setText(result.text());
            });
        }, "ai-conseil").start();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/analyse_emotionnelle.fxml"));
            Parent root = loader.load();
            AnalyseEmotionnelleController controller = loader.getController();
            if (viewerUser != null && "ROLE_PSYCHOLOGUE".equalsIgnoreCase(viewerUser.getRole())) {
                UserSession.setInstance(viewerUser);
                controller.initForPsychologueView();
                controller.focusPatient(patient);
            } else {
                controller.setUserData(patient);
            }
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            statusLabel.setText("Retour impossible.");
        }
    }

    private String patientName() {
        if (patient == null) {
            return "patient";
        }
        return (safe(patient.getPrenom()) + " " + safe(patient.getNom())).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
