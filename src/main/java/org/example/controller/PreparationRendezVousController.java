package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.ConsultationPayment;
import org.example.entities.ConsultationQuestionnaire;
import org.example.entities.User;
import org.example.service.ConsultationWorkflowService;
import org.example.service.MedicalValidationService;
import org.example.utils.UserSession;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PreparationRendezVousController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ALL_PATIENTS = "Tous les patients";
    private static final String SORT_BY_DATE_ASC = "Date la plus proche";
    private static final String SORT_BY_DATE_DESC = "Date la plus recente";
    private static final String SORT_BY_SEVERITY = "Etat le plus grave";

    @FXML private Label totalPreparationsLabel;
    @FXML private Label paidPreparationsLabel;
    @FXML private Label questionnairesLabel;
    @FXML private TextField searchPatientField;
    @FXML private ComboBox<String> patientFilterCombo;
    @FXML private ComboBox<String> sortModeCombo;
    @FXML private Label listSummaryLabel;
    @FXML private VBox patientListBox;
    @FXML private Label selectedPatientLabel;
    @FXML private Label selectedRendezVousLabel;
    @FXML private Label paymentStateLabel;
    @FXML private Label formStateLabel;
    @FXML private Label predictionTitleLabel;
    @FXML private Label predictionSummaryLabel;
    @FXML private Label scoreCliniqueLabel;
    @FXML private Label motifLabel;
    @FXML private Label symptomesLabel;
    @FXML private Label autoEvaluationLabel;
    @FXML private Label risqueLabel;
    @FXML private Label contexteLabel;
    @FXML private Label voiceTranscriptLabel;
    @FXML private Label submittedAtLabel;
    @FXML private BarChart<String, Number> predictionChart;
    @FXML private Label gravityActionLabel;
    @FXML private Button openPatientMapButton;

    private final ConsultationWorkflowService workflowService = new ConsultationWorkflowService();
    private final List<ConsultationPayment> allPayments = new ArrayList<>();
    private User currentUser;
    private ConsultationPayment selectedPayment;

    @FXML
    public void initialize() {
        if (searchPatientField != null) {
            searchPatientField.textProperty().addListener((obs, oldValue, newValue) -> refreshPatientList());
        }
        configureFilters();
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        loadPreparations();
    }

    public void setUserData(User user) {
        this.currentUser = user;
        loadPreparations();
    }

    @FXML
    private void refreshPreparations() {
        loadPreparations();
    }

    @FXML
    private void openPatientMap() {
        if (selectedPayment == null || selectedPayment.getPatientLatitude() == null || selectedPayment.getPatientLongitude() == null) {
            gravityActionLabel.setText("Aucune localisation patient disponible.");
            return;
        }
        String url = String.format(Locale.US, "https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=16/%f/%f",
                selectedPayment.getPatientLatitude(),
                selectedPayment.getPatientLongitude(),
                selectedPayment.getPatientLatitude(),
                selectedPayment.getPatientLongitude());
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            gravityActionLabel.setText("Impossible d'ouvrir la carte du patient : " + e.getMessage());
        }
    }

    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) selectedPatientLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                selectedPatientLabel.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard Psy : " + e.getMessage());
        }
    }

    private void loadPreparations() {
        if (patientListBox == null) {
            return;
        }
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        if (currentUser == null) {
            patientListBox.getChildren().setAll(buildInfoLabel("Aucun psychologue en session.", "#b91c1c"));
            showEmptyDetails("Aucun psychologue en session.");
            return;
        }

        try {
            allPayments.clear();
            allPayments.addAll(workflowService.getPaymentsByPsychologue(currentUser.getId()));
            updatePatientFilterOptions();

            long paidCount = allPayments.stream().filter(ConsultationPayment::isPaid).count();
            long questionnaireCount = allPayments.stream().filter(payment -> payment.getQuestionnaire() != null).count();

            totalPreparationsLabel.setText(String.valueOf(allPayments.size()));
            paidPreparationsLabel.setText(String.valueOf(paidCount));
            questionnairesLabel.setText(String.valueOf(questionnaireCount));

            if (selectedPayment != null) {
                int selectedId = selectedPayment.getId();
                selectedPayment = allPayments.stream()
                        .filter(payment -> payment.getId() == selectedId)
                        .findFirst()
                        .orElse(null);
            }

            refreshPatientList();
        } catch (SQLException e) {
            patientListBox.getChildren().setAll(buildInfoLabel("Impossible de charger les preparations: " + e.getMessage(), "#b91c1c"));
            showEmptyDetails("Impossible de charger les preparations.");
        }
    }

    private void refreshPatientList() {
        if (patientListBox == null) {
            return;
        }

        patientListBox.getChildren().clear();
        List<ConsultationPayment> filteredPayments = filteredPayments();
        updateListSummary(filteredPayments);
        if (filteredPayments.isEmpty()) {
            patientListBox.getChildren().add(buildInfoLabel("Aucun patient ne correspond a la recherche.", "#64748b"));
            if (selectedPayment == null || !allPayments.contains(selectedPayment)) {
                showEmptyDetails("Selectionnez un patient pour voir sa preparation.");
            }
            return;
        }

        for (ConsultationPayment payment : filteredPayments) {
            patientListBox.getChildren().add(buildPatientCard(payment));
        }

        if (selectedPayment == null || !filteredPayments.contains(selectedPayment)) {
            selectedPayment = filteredPayments.get(0);
        }
        populateDetails(selectedPayment);
    }

    private List<ConsultationPayment> filteredPayments() {
        String search = searchPatientField == null ? "" : MedicalValidationService.normalize(searchPatientField.getText()).toLowerCase();
        List<ConsultationPayment> filtered = new ArrayList<>();
        String patientFilter = patientFilterCombo == null || patientFilterCombo.getValue() == null
                ? ALL_PATIENTS
                : patientFilterCombo.getValue();
        for (ConsultationPayment payment : allPayments) {
            boolean matchesPatient = ALL_PATIENTS.equals(patientFilter)
                    || fallback(payment.getPatientName()).equalsIgnoreCase(patientFilter);
            if (!matchesPatient) {
                continue;
            }
            if (search.isBlank() || contains(payment.getPatientName(), search)
                    || contains(payment.getAppointmentType(), search)
                    || contains(payment.getAppointmentDate() == null ? "" : payment.getAppointmentDate().format(DATE_FORMATTER), search)
                    || contains(payment.getAppointmentTimeRange(), search)
                    || contains(buildPatientCardState(payment), search)) {
                filtered.add(payment);
            }
        }
        filtered.sort(resolveComparator());
        return filtered;
    }

    private void configureFilters() {
        if (patientFilterCombo != null) {
            patientFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshPatientList());
        }
        if (sortModeCombo != null) {
            sortModeCombo.getItems().setAll(SORT_BY_DATE_ASC, SORT_BY_DATE_DESC, SORT_BY_SEVERITY);
            sortModeCombo.getSelectionModel().select(SORT_BY_SEVERITY);
            sortModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshPatientList());
        }
    }

    private void updatePatientFilterOptions() {
        if (patientFilterCombo == null) {
            return;
        }
        String previousValue = patientFilterCombo.getValue();
        List<String> patientNames = allPayments.stream()
                .map(ConsultationPayment::getPatientName)
                .map(this::fallback)
                .filter(name -> !"-".equals(name))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        patientFilterCombo.getItems().setAll(ALL_PATIENTS);
        patientFilterCombo.getItems().addAll(patientNames);
        if (previousValue != null && patientFilterCombo.getItems().contains(previousValue)) {
            patientFilterCombo.getSelectionModel().select(previousValue);
        } else {
            patientFilterCombo.getSelectionModel().select(ALL_PATIENTS);
        }
    }

    private Comparator<ConsultationPayment> resolveComparator() {
        String sortMode = sortModeCombo == null || sortModeCombo.getValue() == null
                ? SORT_BY_SEVERITY
                : sortModeCombo.getValue();
        if (SORT_BY_DATE_ASC.equals(sortMode)) {
            return Comparator.comparing(this::safeAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ConsultationPayment::getPatientName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
        if (SORT_BY_DATE_DESC.equals(sortMode)) {
            return Comparator.comparing(this::safeAppointmentDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ConsultationPayment::getPatientName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
        return Comparator.comparingInt(this::severityRank).reversed()
                .thenComparing(this::safeAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ConsultationPayment::getPatientName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private LocalDate safeAppointmentDate(ConsultationPayment payment) {
        return payment == null ? null : payment.getAppointmentDate();
    }

    private int severityRank(ConsultationPayment payment) {
        if (payment == null || payment.getQuestionnaire() == null) {
            return payment != null && payment.isPaid() ? 5 : 0;
        }
        ConsultationQuestionnaire questionnaire = payment.getQuestionnaire();
        int score = questionnaire.getRiskScore();
        if (questionnaire.getUrgencyLevel() >= 8) {
            score += 20;
        }
        String predictedState = MedicalValidationService.normalize(questionnaire.getPredictedState()).toLowerCase();
        if (predictedState.contains("urgence")) {
            score += 25;
        } else if (predictedState.contains("priorite elevee")) {
            score += 15;
        }
        if ("high".equalsIgnoreCase(MedicalValidationService.normalize(questionnaire.getSelfHarmRisk()))) {
            score += 20;
        }
        return score;
    }

    private void updateListSummary(List<ConsultationPayment> filteredPayments) {
        if (listSummaryLabel == null) {
            return;
        }
        String patientFilter = patientFilterCombo == null || patientFilterCombo.getValue() == null
                ? ALL_PATIENTS
                : patientFilterCombo.getValue();
        String sortMode = sortModeCombo == null || sortModeCombo.getValue() == null
                ? SORT_BY_SEVERITY
                : sortModeCombo.getValue();
        if (filteredPayments.isEmpty()) {
            listSummaryLabel.setText("Aucun rendez-vous pour le filtre actuel.");
            return;
        }
        listSummaryLabel.setText(filteredPayments.size() + " rendez-vous affiches | Patient: " + patientFilter + " | Tri: " + sortMode);
    }

    private VBox buildPatientCard(ConsultationPayment payment) {
        boolean selected = selectedPayment != null && selectedPayment.getId() == payment.getId();
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + (selected ? "#dbeafe" : "white")
                + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + accentColor(payment)
                + "; -fx-border-width: 0 0 0 4; -fx-cursor: hand;");

        Label nameLabel = new Label(fallback(payment.getPatientName()));
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label metaLabel = new Label(fallback(payment.getAppointmentType()) + " | "
                + (payment.getAppointmentDate() == null ? "-" : payment.getAppointmentDate().format(DATE_FORMATTER)));
        metaLabel.setStyle("-fx-text-fill: #475569;");

        Label stateLabel = new Label(buildPatientCardState(payment));
        stateLabel.setWrapText(true);
        stateLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-size: 12px;");

        card.getChildren().addAll(nameLabel, metaLabel, stateLabel);
        card.setOnMouseClicked(event -> {
            selectedPayment = payment;
            refreshPatientList();
        });
        return card;
    }

    private void populateDetails(ConsultationPayment payment) {
        if (payment == null) {
            showEmptyDetails("Selectionnez un patient pour voir sa preparation.");
            return;
        }

        selectedPatientLabel.setText(fallback(payment.getPatientName()));
        selectedRendezVousLabel.setText("Rendez-vous: " + fallback(payment.getAppointmentType())
                + " le " + (payment.getAppointmentDate() == null ? "-" : payment.getAppointmentDate().format(DATE_FORMATTER))
                + " a " + fallback(payment.getAppointmentTimeRange()));
        paymentStateLabel.setText("Paiement: " + (payment.isPaid() ? "valide" : "en attente") + " | Montant: " + payment.getFormattedAmount());

        ConsultationQuestionnaire questionnaire = payment.getQuestionnaire();
        if (questionnaire == null) {
            formStateLabel.setText(payment.isPaid()
                    ? "Formulaire non encore rempli par le patient."
                    : "Le formulaire apparaitra ici apres paiement.");
            predictionTitleLabel.setText("Prediction d'etat psychologique");
            predictionSummaryLabel.setText("Aucune prediction disponible pour le moment.");
            scoreCliniqueLabel.setText("Score clinique: -");
            motifLabel.setText("Motif principal: -");
            symptomesLabel.setText("Symptomes: -");
            autoEvaluationLabel.setText("Auto-evaluation: -");
            risqueLabel.setText("Risque auto-agression: -");
            contexteLabel.setText("Contexte complementaire: -");
            voiceTranscriptLabel.setText("Note vocale transcrite: -");
            submittedAtLabel.setText("Formulaire envoye le: -");
            gravityActionLabel.setText("Aucune alerte de deplacement.");
            openPatientMapButton.setVisible(false);
            openPatientMapButton.setManaged(false);
            clearChart();
            return;
        }

        formStateLabel.setText("Formulaire rempli par le patient et pret pour la preparation du rendez-vous.");
        predictionTitleLabel.setText("Prediction d'etat psychologique du patient");
        predictionSummaryLabel.setText(fallback(questionnaire.getPredictedState()));
        scoreCliniqueLabel.setText("Score clinique: " + questionnaire.getRiskScore());
        motifLabel.setText("Motif principal: " + fallback(questionnaire.getChiefComplaint()));
        symptomesLabel.setText("Symptomes: " + fallback(questionnaire.getSymptomSummary()));
        autoEvaluationLabel.setText("Auto-evaluation: "
                + "Stress " + questionnaire.getStressLevel()
                + " | Anxiete " + questionnaire.getAnxietyLevel()
                + " | Humeur " + questionnaire.getMoodLevel()
                + " | Sommeil " + questionnaire.getSleepQuality()
                + " | Energie " + questionnaire.getEnergyLevel()
                + " | Soutien " + questionnaire.getSupportLevel()
                + " | Urgence " + questionnaire.getUrgencyLevel());
        risqueLabel.setText("Risque auto-agression: " + fallback(questionnaire.getSelfHarmRisk()));
        contexteLabel.setText("Contexte complementaire: " + fallback(questionnaire.getAdditionalContext()));
        voiceTranscriptLabel.setText("Note vocale transcrite: " + fallback(questionnaire.getVoiceTranscript()));
        submittedAtLabel.setText("Formulaire envoye le: "
                + (questionnaire.getSubmittedAt() == null ? "-" : questionnaire.getSubmittedAt().format(DATE_TIME_FORMATTER)));
        updateRiskChart(questionnaire);
        updateSeverityActions(payment, questionnaire);
    }

    private void showEmptyDetails(String message) {
        selectedPatientLabel.setText("Aucun patient selectionne");
        selectedRendezVousLabel.setText("Rendez-vous: -");
        paymentStateLabel.setText("Paiement: -");
        formStateLabel.setText(message);
        predictionTitleLabel.setText("Prediction d'etat psychologique");
        predictionSummaryLabel.setText("Selectionnez un patient pour afficher la prediction.");
        scoreCliniqueLabel.setText("Score clinique: -");
        motifLabel.setText("Motif principal: -");
        symptomesLabel.setText("Symptomes: -");
        autoEvaluationLabel.setText("Auto-evaluation: -");
        risqueLabel.setText("Risque auto-agression: -");
        contexteLabel.setText("Contexte complementaire: -");
        voiceTranscriptLabel.setText("Note vocale transcrite: -");
        submittedAtLabel.setText("Formulaire envoye le: -");
        gravityActionLabel.setText("Aucune alerte de deplacement.");
        openPatientMapButton.setVisible(false);
        openPatientMapButton.setManaged(false);
        clearChart();
    }

    private Label buildInfoLabel(String text, String color) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
        return label;
    }

    private String buildPatientCardState(ConsultationPayment payment) {
        ConsultationQuestionnaire questionnaire = payment.getQuestionnaire();
        if (questionnaire != null) {
            return "Prediction: " + fallback(questionnaire.getPredictedState()) + " | Score " + questionnaire.getRiskScore();
        }
        return payment.isPaid() ? "Paiement valide, formulaire en attente" : "Paiement en attente";
    }

    private String accentColor(ConsultationPayment payment) {
        if (payment.getQuestionnaire() != null) {
            return "#2563eb";
        }
        return payment.isPaid() ? "#16a34a" : "#f59e0b";
    }

    private void updateRiskChart(ConsultationQuestionnaire questionnaire) {
        if (predictionChart == null) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Stress", questionnaire.getStressLevel()));
        series.getData().add(new XYChart.Data<>("Anxiete", questionnaire.getAnxietyLevel()));
        series.getData().add(new XYChart.Data<>("Humeur", 10 - questionnaire.getMoodLevel()));
        series.getData().add(new XYChart.Data<>("Sommeil", 10 - questionnaire.getSleepQuality()));
        series.getData().add(new XYChart.Data<>("Energie", 10 - questionnaire.getEnergyLevel()));
        series.getData().add(new XYChart.Data<>("Urgence", questionnaire.getUrgencyLevel()));

        predictionChart.getData().setAll(series);
        Platform.runLater(() -> colorizeChart(series));
    }

    private void colorizeChart(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() == null) {
                continue;
            }
            double value = data.getYValue().doubleValue();
            String color;
            if (value >= 8) {
                color = "#dc2626";
            } else if (value >= 5) {
                color = "#f59e0b";
            } else {
                color = "#16a34a";
            }
            data.getNode().setStyle("-fx-bar-fill: " + color + ";");
        }
    }

    private void clearChart() {
        if (predictionChart != null) {
            predictionChart.getData().clear();
        }
    }

    private void updateSeverityActions(ConsultationPayment payment, ConsultationQuestionnaire questionnaire) {
        boolean severe = isSevere(questionnaire);
        boolean urgent = questionnaire.getUrgencyLevel() >= 8
                || MedicalValidationService.normalize(questionnaire.getPredictedState()).toLowerCase().contains("urgence");
        if (severe || urgent) {
            gravityActionLabel.setText("Etat grave ou urgent detecte : une visite a domicile est recommandee. Ouvrez la carte pour localiser le patient.");
            boolean hasLocation = payment.getPatientLatitude() != null && payment.getPatientLongitude() != null;
            openPatientMapButton.setVisible(hasLocation);
            openPatientMapButton.setManaged(hasLocation);
            openPatientMapButton.setDisable(!hasLocation);
            if (!hasLocation) {
                gravityActionLabel.setText("Etat grave ou urgent detecte, mais aucune position patient n'est disponible pour la visite a domicile.");
            }
            return;
        }
        gravityActionLabel.setText("Etat non grave : suivi standard possible avec consultation classique.");
        openPatientMapButton.setVisible(false);
        openPatientMapButton.setManaged(false);
    }

    private boolean isSevere(ConsultationQuestionnaire questionnaire) {
        String predictedState = MedicalValidationService.normalize(questionnaire.getPredictedState()).toLowerCase();
        String selfHarmRisk = MedicalValidationService.normalize(questionnaire.getSelfHarmRisk()).toLowerCase();
        return questionnaire.getRiskScore() >= 65
                || questionnaire.getUrgencyLevel() >= 8
                || "high".equals(selfHarmRisk)
                || predictedState.contains("priorite elevee");
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String fallback(String value) {
        String normalized = MedicalValidationService.normalize(value);
        return normalized.isBlank() ? "-" : normalized;
    }
}
