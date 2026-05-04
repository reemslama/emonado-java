package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.ConsultationPayment;
import org.example.entities.ConsultationQuestionnaire;
import org.example.entities.User;
import org.example.service.ConsultationWorkflowService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminPaymentsController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label totalPaymentsLabel;
    @FXML private Label paidPaymentsLabel;
    @FXML private Label pendingPaymentsLabel;
    @FXML private Label questionnaireCountLabel;
    @FXML private VBox paymentsBox;

    private final ConsultationWorkflowService workflowService = new ConsultationWorkflowService();

    public void setUserData(User user) {
        loadPayments();
    }

    @FXML
    public void initialize() {
        loadPayments();
    }

    @FXML
    private void refreshPayments() {
        loadPayments();
    }

    private void loadPayments() {
        if (paymentsBox == null) {
            return;
        }
        try {
            List<ConsultationPayment> payments = workflowService.getAllPayments();
            long paid = payments.stream().filter(ConsultationPayment::isPaid).count();
            long pending = payments.stream().filter(ConsultationPayment::requiresPayment).count();
            long questionnaires = payments.stream().filter(payment -> payment.getQuestionnaire() != null).count();

            totalPaymentsLabel.setText(String.valueOf(payments.size()));
            paidPaymentsLabel.setText(String.valueOf(paid));
            pendingPaymentsLabel.setText(String.valueOf(pending));
            questionnaireCountLabel.setText(String.valueOf(questionnaires));

            paymentsBox.getChildren().clear();
            if (payments.isEmpty()) {
                Label empty = new Label("Aucun paiement enregistre pour le moment.");
                empty.setStyle("-fx-text-fill: #64748b;");
                paymentsBox.getChildren().add(empty);
                return;
            }

            for (ConsultationPayment payment : payments) {
                paymentsBox.getChildren().add(buildPaymentCard(payment));
            }
        } catch (SQLException e) {
            paymentsBox.getChildren().setAll(new Label("Erreur de chargement: " + e.getMessage()));
        }
    }

    private VBox buildPaymentCard(ConsultationPayment payment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; "
                + "-fx-border-color: " + (payment.isPaid() ? "#16a34a" : "#f59e0b") + "; -fx-border-width: 0 0 0 4;");

        Label title = new Label(payment.getPatientName() + " - " + payment.getAppointmentType());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label meta = new Label("Psychologue: " + payment.getPsychologueName()
                + " | Date: " + payment.getAppointmentDate().format(DATE_FORMATTER)
                + " | Horaire: " + payment.getAppointmentTimeRange());
        meta.setStyle("-fx-text-fill: #475569;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label status = new Label(payment.getStatus().toUpperCase());
        status.setStyle("-fx-background-color: " + (payment.isPaid() ? "#dcfce7" : "#fef3c7")
                + "; -fx-text-fill: #0f172a; -fx-padding: 6 12; -fx-background-radius: 999;");
        Label amount = new Label(payment.getFormattedAmount());
        amount.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        HBox row = new HBox(10, amount, spacer, status);

        ConsultationQuestionnaire questionnaire = payment.getQuestionnaire();
        Label questionnaireLabel = new Label(questionnaire == null
                ? "Questionnaire: non soumis"
                : "Questionnaire: score " + questionnaire.getRiskScore() + " | " + questionnaire.getPredictedState());
        questionnaireLabel.setWrapText(true);
        questionnaireLabel.setStyle("-fx-text-fill: #334155;");

        card.getChildren().addAll(title, meta, row, questionnaireLabel);
        return card;
    }
}
