package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;
import org.example.utils.UserSession;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class AiAlertesController {
    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private VBox alertsBox;

    private User viewerUser;
    private User patient;
    private List<JournalAnalyseRow> rows = List.of();

    public void setData(User viewerUser, User patient, List<JournalAnalyseRow> rows) {
        this.viewerUser = viewerUser;
        this.patient = patient;
        this.rows = rows == null ? List.of() : rows;
        titleLabel.setText("Alertes AI - " + patientName());
        renderAlerts();
    }

    private void renderAlerts() {
        alertsBox.getChildren().clear();
        List<JournalAnalyseRow> alerts = rows.stream()
                .filter(row -> row.getRisqueScore() > 0)
                .sorted(Comparator.comparingInt(JournalAnalyseRow::getRisqueScore).reversed())
                .toList();

        if (alerts.isEmpty()) {
            summaryLabel.setText("Aucune alerte detectee sur les journaux de ce patient.");
            return;
        }

        long critical = alerts.stream().filter(row -> row.getRisqueScore() >= 10).count();
        summaryLabel.setText(alerts.size() + " alerte(s), dont " + critical + " critique(s).");

        for (JournalAnalyseRow row : alerts) {
            VBox card = new VBox(8);
            String color = row.getRisqueScore() >= 10 ? "#b91c1c" : "#b45309";
            String background = row.getRisqueScore() >= 10 ? "#fff1f2" : "#fff7ed";
            card.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 14; -fx-padding: 16; -fx-border-color: " + color + "; -fx-border-radius: 14;");
            Label title = new Label(row.getDateJournal() + " | " + row.getRisqueLabel() + " | Score " + row.getRisqueScore());
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            Label content = new Label(row.getContenuComplet());
            content.setWrapText(true);
            content.setStyle("-fx-text-fill: #374151;");
            Label details = new Label(row.getRisqueDetails());
            details.setWrapText(true);
            details.setStyle("-fx-text-fill: #6b7280;");
            card.getChildren().addAll(title, content, details);
            alertsBox.getChildren().add(card);
        }
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
            summaryLabel.setText("Retour impossible.");
        }
    }

    private String patientName() {
        if (patient == null) {
            return "patient";
        }
        return ((patient.getPrenom() == null ? "" : patient.getPrenom()) + " "
                + (patient.getNom() == null ? "" : patient.getNom())).trim();
    }
}
