package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.entities.AntecedentMedical;
import org.example.entities.AnalyseEmotionnelle;
import org.example.entities.DossierMedical;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.TestResultMedical;
import org.example.entities.User;
import org.example.service.AnalyseEmotionnelleService;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.service.PdfExportService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MedicalRecordController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TIMELINE_ALL = "Tout afficher";
    private static final String TIMELINE_CONSULTATIONS = "Consultations";
    private static final String TIMELINE_ANTECEDENTS = "Antecedents";
    private static final String TIMELINE_TESTS = "Tests";
    private static final String TIMELINE_ANALYSES = "Analyses emotionnelles";
    private static final List<String> ANTECEDENT_TYPES = List.of(
            "Allergie",
            "Maladie chronique",
            "Chirurgie",
            "Hospitalisation",
            "Traitement en cours",
            "Antecedent familial",
            "Autre"
    );

    @FXML private Label titleLabel;
    @FXML private Label dossierCreationLabel;
    @FXML private Label dossierUpdateLabel;
    @FXML private Label ageSummaryLabel;
    @FXML private Label antecedentCountLabel;
    @FXML private Label consultationCountLabel;
    @FXML private Label alertCountLabel;
    @FXML private Label timelineSummaryLabel;
    @FXML private Label clinicalStatusLabel;
    @FXML private TextArea psychologueNoteArea;
    @FXML private TextArea reminderArea;
    @FXML private TextArea medicalHistoryArea;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField sexeField;
    @FXML private DatePicker birthDatePicker;

    @FXML private ComboBox<String> antecedentTypeCombo;
    @FXML private TextArea antecedentDescriptionArea;
    @FXML private DatePicker antecedentDatePicker;
    @FXML private VBox antecedentsContainer;
    @FXML private Button saveAntecedentButton;
    @FXML private Button cancelAntecedentButton;
    @FXML private Button deleteAntecedentButton;

    @FXML private VBox analysesContainer;
    @FXML private VBox testResultsContainer;
    @FXML private VBox consultationsContainer;
    @FXML private VBox timelineContainer;
    @FXML private TextField timelineSearchField;
    @FXML private ComboBox<String> timelineFilterCombo;

    private final MedicalDataService medicalDataService = new MedicalDataService();

    private final AnalyseEmotionnelleService analyseService = new AnalyseEmotionnelleService();
    private final PdfExportService pdfExportService = new PdfExportService();

    private User currentUser;
    private AntecedentMedical selectedAntecedent;
    private DossierMedical currentMedicalRecord;
    private final List<AntecedentMedical> cachedAntecedents = new ArrayList<>();
    private final List<TestResultMedical> cachedTestResults = new ArrayList<>();
    private final List<AnalyseEmotionnelle> cachedAnalyses = new ArrayList<>();
    private final List<org.example.entities.Consultation> cachedConsultations = new ArrayList<>();

    @FXML
    public void initialize() {
        configureAntecedentInputs();
        configureTimelineFilters();
        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        fillPersonalInfo();

        try {
            medicalDataService.ensureSchema();
            loadMedicalRecordMeta();
            loadAntecedents();
            loadConsultations();
            loadTestResults();
            loadAnalyses();
            refreshClinicalSummary();
            refreshTimeline();
            resetAntecedentForm();
        } catch (SQLException e) {
            showError("Impossible de charger le dossier medical : " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveAntecedent() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        AntecedentMedical antecedentMedical = selectedAntecedent == null ? new AntecedentMedical() : selectedAntecedent;
        antecedentMedical.setType(MedicalValidationService.normalize(antecedentTypeCombo.getValue()));
        antecedentMedical.setDescription(MedicalValidationService.normalize(antecedentDescriptionArea.getText()));
        antecedentMedical.setDateDiagnostic(antecedentDatePicker.getValue());

        String validationError = MedicalValidationService.validateAntecedent(antecedentMedical);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            boolean creation = selectedAntecedent == null;
            medicalDataService.saveAntecedent(currentUser.getId(), antecedentMedical);
            loadAntecedents();
            resetAntecedentForm();
            showInfo(creation ? "Antecedent medical ajoute." : "Antecedent medical modifie.");
        } catch (SQLException e) {
            showError("Impossible d'enregistrer l'antecedent : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelAntecedentEdit() {
        resetAntecedentForm();
    }

    @FXML
    private void handleDeleteSelectedAntecedent() {
        if (selectedAntecedent == null) {
            showError("Selectionnez un antecedent a supprimer.");
            return;
        }
        deleteAntecedent(selectedAntecedent);
    }

    @FXML
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent view = loader.load();
            PatientDashboardController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            showError("Impossible de revenir au tableau de bord.");
        }
    }

    @FXML
    private void goToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil_patient.fxml"));
            Parent view = loader.load();
            ProfilPatientController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            showError("Impossible d'ouvrir le profil.");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent view = FXMLLoader.load(getClass().getResource("/login.fxml"));
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            showError("Impossible de se deconnecter.");
        }
    }

    @FXML
    private void handleExportPdf() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le dossier medical en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName("dossier-medical-" + currentUser.getNom() + "-" + currentUser.getPrenom() + ".pdf");
        File targetFile = fileChooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (targetFile == null) {
            return;
        }

        try {
            var dossierMedical = medicalDataService.getMedicalRecordByPatient(currentUser.getId());
            List<AntecedentMedical> antecedents = medicalDataService.getAntecedentsByPatient(currentUser.getId());
            List<TestResultMedical> testResults = medicalDataService.getTestResultsByPatient(currentUser.getId());
            List<AnalyseEmotionnelle> analyses = analyseService.findRowsByUser(currentUser).stream()
                    .filter(row -> row.getAnalyseEmotionnelle() != null)
                    .map(JournalAnalyseRow::getAnalyseEmotionnelle)
                    .toList();
            pdfExportService.exportPatientMedicalRecord(targetFile.toPath(), currentUser, dossierMedical, antecedents, testResults, analyses);
            showInfo("PDF exporte : " + targetFile.getAbsolutePath());
        } catch (Exception e) {
            showError("Impossible d'exporter le PDF : " + e.getMessage());
        }
    }

    private void fillPersonalInfo() {
        titleLabel.setText("Mon Dossier Medical");
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getTelephone());
        sexeField.setText(currentUser.getSexe() == null ? "" : currentUser.getSexe());
        birthDatePicker.setValue(currentUser.getDateNaissance());

        nomField.setEditable(false);
        prenomField.setEditable(false);
        emailField.setEditable(false);
        phoneField.setEditable(false);
        sexeField.setEditable(false);
        birthDatePicker.setDisable(true);
    }

    private void loadMedicalRecordMeta() throws SQLException {
        currentMedicalRecord = medicalDataService.getMedicalRecordByPatient(currentUser.getId());
        dossierCreationLabel.setText(
                currentMedicalRecord == null || currentMedicalRecord.getCreatedAt() == null
                        ? "-"
                        : currentMedicalRecord.getCreatedAt().toLocalDate().toString()
        );
        if (dossierUpdateLabel != null) {
            dossierUpdateLabel.setText(
                    "Derniere mise a jour: " + (currentMedicalRecord == null || currentMedicalRecord.getUpdatedAt() == null
                            ? "-"
                            : currentMedicalRecord.getUpdatedAt().format(DATE_TIME_FORMATTER))
            );
        }
        if (psychologueNoteArea != null) {
            String psychologueNote = currentMedicalRecord == null ? "" : MedicalValidationService.normalize(currentMedicalRecord.getPsychologueNote());
            psychologueNoteArea.setText(psychologueNote.isBlank()
                    ? "Aucune note psychologue n'a encore ete ajoutee a votre dossier."
                    : psychologueNote);
            psychologueNoteArea.setEditable(false);
        }
        if (reminderArea != null) {
            String reminderText = currentMedicalRecord == null ? "" : MedicalValidationService.normalize(currentMedicalRecord.getReminderText());
            reminderArea.setText(reminderText.isBlank() ? "Aucun rappel medical enregistre." : reminderText);
            reminderArea.setEditable(false);
        }
        if (medicalHistoryArea != null) {
            String medicalHistory = currentMedicalRecord == null ? "" : MedicalValidationService.normalize(currentMedicalRecord.getMedicalHistory());
            medicalHistoryArea.setText(medicalHistory.isBlank() ? "Aucun resume clinique global n'a encore ete renseigne." : medicalHistory);
            medicalHistoryArea.setEditable(false);
        }
    }

    private void loadAntecedents() throws SQLException {
        antecedentsContainer.getChildren().clear();
        cachedAntecedents.clear();
        cachedAntecedents.addAll(medicalDataService.getAntecedentsByPatient(currentUser.getId()));
        List<AntecedentMedical> antecedents = cachedAntecedents;
        if (antecedents.isEmpty()) {
            Label emptyLabel = new Label("Aucun antecedent medical.");
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
            antecedentsContainer.getChildren().add(emptyLabel);
        } else {
            for (AntecedentMedical antecedent : antecedents) {
                antecedentsContainer.getChildren().add(AntecedentCardFactory.build(
                        antecedent,
                        this::startAntecedentEdition,
                        this::deleteAntecedent
                ));
            }
        }
    }

    private void loadAnalyses() throws SQLException {
        analysesContainer.getChildren().clear();
        List<JournalAnalyseRow> rows = analyseService.findRowsByUser(currentUser);
        cachedAnalyses.clear();
        cachedAnalyses.addAll(rows.stream()
                .filter(row -> row.getAnalyseEmotionnelle() != null)
                .map(JournalAnalyseRow::getAnalyseEmotionnelle)
                .toList());
        List<AnalyseEmotionnelle> analyses = cachedAnalyses;
        if (analyses.isEmpty()) {
            Label emptyLabel = new Label("Aucune analyse emotionnelle disponible.");
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
            analysesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (AnalyseEmotionnelle analyse : analyses) {
            analysesContainer.getChildren().add(buildAnalyseCard(analyse));
        }
    }

    private void loadTestResults() throws SQLException {
        testResultsContainer.getChildren().clear();
        cachedTestResults.clear();
        cachedTestResults.addAll(medicalDataService.getTestResultsByPatient(currentUser.getId()));
        List<TestResultMedical> testResults = cachedTestResults;
        if (testResults.isEmpty()) {
            Label emptyLabel = new Label("Aucun resultat de test disponible.");
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
            testResultsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (TestResultMedical testResult : testResults) {
            testResultsContainer.getChildren().add(buildTestResultCard(testResult));
        }
    }

    private void loadConsultations() throws SQLException {
        if (consultationsContainer == null) {
            return;
        }
        consultationsContainer.getChildren().clear();
        cachedConsultations.clear();
        cachedConsultations.addAll(medicalDataService.getConsultationsByPatient(currentUser.getId()));
        if (cachedConsultations.isEmpty()) {
            Label emptyLabel = new Label("Aucune consultation medicale enregistree.");
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
            consultationsContainer.getChildren().add(emptyLabel);
            return;
        }
        for (org.example.entities.Consultation consultation : cachedConsultations) {
            consultationsContainer.getChildren().add(buildConsultationCard(consultation));
        }
    }

    private void startAntecedentEdition(AntecedentMedical antecedentMedical) {
        selectedAntecedent = antecedentMedical;
        antecedentTypeCombo.getSelectionModel().select(antecedentMedical.getType());
        antecedentDescriptionArea.setText(antecedentMedical.getDescription());
        antecedentDatePicker.setValue(antecedentMedical.getDateDiagnostic());
        saveAntecedentButton.setText("Mettre a jour l'antecedent");
        cancelAntecedentButton.setVisible(true);
        cancelAntecedentButton.setManaged(true);
        deleteAntecedentButton.setVisible(true);
        deleteAntecedentButton.setManaged(true);
    }

    private void deleteAntecedent(AntecedentMedical antecedentMedical) {
        try {
            medicalDataService.deleteAntecedent(antecedentMedical.getId());
            if (selectedAntecedent != null && selectedAntecedent.getId() == antecedentMedical.getId()) {
                resetAntecedentForm();
            }
            loadAntecedents();
            showInfo("Antecedent medical supprime.");
        } catch (SQLException e) {
            showError("Impossible de supprimer l'antecedent : " + e.getMessage());
        }
    }

    private void resetAntecedentForm() {
        selectedAntecedent = null;
        antecedentTypeCombo.getSelectionModel().clearSelection();
        antecedentDescriptionArea.clear();
        antecedentDatePicker.setValue(null);
        saveAntecedentButton.setText("Ajouter l'antecedent medical");
        cancelAntecedentButton.setVisible(false);
        cancelAntecedentButton.setManaged(false);
        deleteAntecedentButton.setVisible(false);
        deleteAntecedentButton.setManaged(false);
    }

    private void configureAntecedentInputs() {
        if (antecedentTypeCombo != null) {
            antecedentTypeCombo.getItems().setAll(ANTECEDENT_TYPES);
        }
        if (antecedentDatePicker != null) {
            antecedentDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    boolean disabled = empty || item == null || !item.isBefore(LocalDate.now());
                    setDisable(disabled);
                }
            });
        }
    }

    private void configureTimelineFilters() {
        if (timelineFilterCombo != null) {
            timelineFilterCombo.getItems().setAll(
                    TIMELINE_ALL,
                    TIMELINE_CONSULTATIONS,
                    TIMELINE_ANTECEDENTS,
                    TIMELINE_TESTS,
                    TIMELINE_ANALYSES
            );
            timelineFilterCombo.getSelectionModel().select(TIMELINE_ALL);
            timelineFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshTimeline());
        }
        if (timelineSearchField != null) {
            timelineSearchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTimeline());
        }
    }

    private VBox buildAnalyseCard(AnalyseEmotionnelle analyse) {
        VBox card = new VBox(8);
        card.setPadding(new javafx.geometry.Insets(12));
        card.setStyle("-fx-background-color: #f0f9f0; -fx-background-radius: 10; "
                + "-fx-border-color: #b8e6b8; -fx-border-radius: 10;");

        Label title = new Label("Analyse Emotionnelle");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f8b57;");

        String dateText = analyse.getDateAnalyseFormatted();
        Label date = new Label("Date: " + dateText);
        date.setStyle("-fx-text-fill: #2d5a3d;");

        Label etat = new Label("État émotionnel: " + (analyse.getEtatEmotionnel() != null ? analyse.getEtatEmotionnel() : "N/A"));
        etat.setStyle("-fx-text-fill: #2d5a3d;");

        Label niveau = new Label("Niveau: " + (analyse.getNiveau() != null ? analyse.getNiveau() : "N/A"));
        niveau.setStyle("-fx-text-fill: #2d5a3d;");

        Label declencheur = new Label("Déclencheur: " + (analyse.getDeclencheur() != null ? analyse.getDeclencheur() : "N/A"));
        declencheur.setWrapText(true);
        declencheur.setStyle("-fx-text-fill: #2d5a3d;");

        Label conseil = new Label("Conseil: " + (analyse.getConseil() != null ? analyse.getConseil() : "N/A"));
        conseil.setWrapText(true);
        conseil.setStyle("-fx-text-fill: #2d5a3d;");

        card.getChildren().addAll(title, date, etat, niveau, declencheur, conseil);
        return card;
    }

    private VBox buildConsultationCard(org.example.entities.Consultation consultation) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #eef7ff; -fx-background-radius: 18; "
                + "-fx-border-color: #bfdbfe; -fx-border-radius: 18;");

        Label title = new Label("Consultation du " + formatDate(consultation.getConsultationDate()));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f4f7a;");

        String createdAt = consultation.getCreatedAt() == null ? "-" : consultation.getCreatedAt().format(DATE_TIME_FORMATTER);
        Label meta = new Label("Creee le: " + createdAt);
        meta.setStyle("-fx-text-fill: #5b7085; -fx-font-size: 13px;");

        Label patientNote = new Label("Notes patient: " + fallback(consultation.getNotesPatient()));
        patientNote.setWrapText(true);
        patientNote.setStyle("-fx-text-fill: #334155;");

        Label psychologueNote = new Label("Notes psychologue: " + fallback(consultation.getNotesPsychologue()));
        psychologueNote.setWrapText(true);
        psychologueNote.setStyle("-fx-text-fill: #334155;");

        card.getChildren().addAll(title, meta, patientNote, psychologueNote);
        return card;
    }

    private VBox buildTestResultCard(TestResultMedical testResult) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #fff8ed; -fx-background-radius: 18; "
                + "-fx-border-color: #f1c27d; -fx-border-radius: 18;");

        Label category = new Label(testResult.getCategorie());
        category.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #9a5312;");

        double ratio = testResult.getScoreMax() <= 0 ? 0 : (double) testResult.getScore() / testResult.getScoreMax();
        String status = ratio >= 0.7 ? "niveau eleve" : ratio >= 0.4 ? "niveau modere" : "niveau faible";

        Label score = new Label("Score : " + testResult.getScore() + " / " + testResult.getScoreMax());
        score.setStyle("-fx-text-fill: #7c4a0d; -fx-font-size: 14px;");

        Label level = new Label("Interpretation : " + status);
        level.setStyle("-fx-text-fill: #7c4a0d; -fx-font-size: 14px;");

        String createdAt = testResult.getCreatedAt() == null ? "-" : testResult.getCreatedAt().format(DATE_TIME_FORMATTER);
        Label date = new Label("Passe le : " + createdAt);
        date.setStyle("-fx-text-fill: #8c6b4e; -fx-font-size: 13px;");

        card.getChildren().addAll(category, score, level, date);
        return card;
    }

    private void refreshClinicalSummary() {
        if (ageSummaryLabel != null) {
            ageSummaryLabel.setText(resolveAgeText());
        }
        if (antecedentCountLabel != null) {
            antecedentCountLabel.setText(String.valueOf(cachedAntecedents.size()));
        }
        if (consultationCountLabel != null) {
            consultationCountLabel.setText(String.valueOf(cachedConsultations.size()));
        }
        int alertCount = countAlerts();
        if (alertCountLabel != null) {
            alertCountLabel.setText(String.valueOf(alertCount));
        }
        if (clinicalStatusLabel != null) {
            clinicalStatusLabel.setText(buildClinicalStatusMessage(alertCount));
        }
    }

    private String resolveAgeText() {
        if (currentUser == null || currentUser.getDateNaissance() == null) {
            return "-";
        }
        return Period.between(currentUser.getDateNaissance(), LocalDate.now()).getYears() + " ans";
    }

    private int countAlerts() {
        int alerts = 0;
        for (TestResultMedical testResult : cachedTestResults) {
            if (testResult.getScoreMax() > 0 && ((double) testResult.getScore() / testResult.getScoreMax()) >= 0.7) {
                alerts++;
            }
        }
        for (AnalyseEmotionnelle analyse : cachedAnalyses) {
            String niveau = MedicalValidationService.normalize(analyse.getNiveau()).toLowerCase(Locale.ROOT);
            String etat = MedicalValidationService.normalize(analyse.getEtatEmotionnel()).toLowerCase(Locale.ROOT);
            if (niveau.contains("eleve") || niveau.contains("grave") || etat.contains("anx") || etat.contains("detresse")) {
                alerts++;
            }
        }
        String psychologueNote = currentMedicalRecord == null ? "" : MedicalValidationService.normalize(currentMedicalRecord.getPsychologueNote()).toLowerCase(Locale.ROOT);
        if (psychologueNote.contains("urgence") || psychologueNote.contains("surve") || psychologueNote.contains("grave")) {
            alerts++;
        }
        return alerts;
    }

    private String buildClinicalStatusMessage(int alertCount) {
        if (alertCount == 0) {
            return "Statut clinique: stable, aucun signal d'alerte majeur detecte.";
        }
        if (alertCount <= 2) {
            return "Statut clinique: a surveiller, quelques indicateurs meritent un suivi regulier.";
        }
        return "Statut clinique: sensible, plusieurs alertes justifient une attention renforcee du psychologue.";
    }

    private void refreshTimeline() {
        if (timelineContainer == null) {
            return;
        }
        timelineContainer.getChildren().clear();
        List<TimelineEntry> entries = buildTimelineEntries();
        String filter = timelineFilterCombo == null || timelineFilterCombo.getValue() == null
                ? TIMELINE_ALL
                : timelineFilterCombo.getValue();
        String search = timelineSearchField == null ? "" : MedicalValidationService.normalize(timelineSearchField.getText()).toLowerCase(Locale.ROOT);

        List<TimelineEntry> filteredEntries = entries.stream()
                .filter(entry -> TIMELINE_ALL.equals(filter) || entry.type.equals(filter))
                .filter(entry -> search.isBlank()
                        || entry.title.toLowerCase(Locale.ROOT).contains(search)
                        || entry.subtitle.toLowerCase(Locale.ROOT).contains(search)
                        || entry.details.toLowerCase(Locale.ROOT).contains(search))
                .sorted(Comparator.comparing((TimelineEntry entry) -> entry.when, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (timelineSummaryLabel != null) {
            timelineSummaryLabel.setText(filteredEntries.size() + " element(s) dans la chronologie");
        }

        if (filteredEntries.isEmpty()) {
            Label emptyLabel = new Label("Aucun element ne correspond a votre recherche dans la chronologie.");
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
            timelineContainer.getChildren().add(emptyLabel);
            return;
        }

        for (TimelineEntry entry : filteredEntries) {
            timelineContainer.getChildren().add(buildTimelineCard(entry));
        }
    }

    private List<TimelineEntry> buildTimelineEntries() {
        List<TimelineEntry> entries = new ArrayList<>();
        for (AntecedentMedical antecedent : cachedAntecedents) {
            entries.add(new TimelineEntry(
                    TIMELINE_ANTECEDENTS,
                    antecedent.getDateDiagnostic() == null ? null : antecedent.getDateDiagnostic().atStartOfDay(),
                    "Antecedent: " + fallback(antecedent.getType()),
                    antecedent.getDateDiagnostic() == null ? "Date non precisee" : "Diagnostic le " + formatDate(antecedent.getDateDiagnostic()),
                    fallback(antecedent.getDescription()),
                    "#7c2235"
            ));
        }
        for (TestResultMedical testResult : cachedTestResults) {
            entries.add(new TimelineEntry(
                    TIMELINE_TESTS,
                    testResult.getCreatedAt(),
                    "Test: " + fallback(testResult.getCategorie()),
                    "Score " + testResult.getScore() + "/" + testResult.getScoreMax(),
                    "Passe le " + formatDateTime(testResult.getCreatedAt()),
                    "#9a5312"
            ));
        }
        for (AnalyseEmotionnelle analyse : cachedAnalyses) {
            entries.add(new TimelineEntry(
                    TIMELINE_ANALYSES,
                    analyse.getDateAnalyse(),
                    "Analyse emotionnelle",
                    fallback(analyse.getEtatEmotionnel()) + " | Niveau " + fallback(analyse.getNiveau()),
                    "Declencheur: " + fallback(analyse.getDeclencheur()) + " | Conseil: " + fallback(analyse.getConseil()),
                    "#1f8b57"
            ));
        }
        for (org.example.entities.Consultation consultation : cachedConsultations) {
            LocalDateTime when = consultation.getConsultationDate() == null ? consultation.getCreatedAt() : consultation.getConsultationDate().atStartOfDay();
            entries.add(new TimelineEntry(
                    TIMELINE_CONSULTATIONS,
                    when,
                    "Consultation",
                    consultation.getConsultationDate() == null ? "Date non precisee" : "Le " + formatDate(consultation.getConsultationDate()),
                    "Patient: " + fallback(consultation.getNotesPatient()) + " | Psychologue: " + fallback(consultation.getNotesPsychologue()),
                    "#1f4f7a"
            ));
        }
        return entries;
    }

    private VBox buildTimelineCard(TimelineEntry entry) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; "
                + "-fx-border-color: " + entry.accentColor + "; -fx-border-width: 0 0 0 4;");

        Label title = new Label(entry.title);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + entry.accentColor + ";");

        Label subtitle = new Label(entry.subtitle);
        subtitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        Label details = new Label(entry.details);
        details.setWrapText(true);
        details.setStyle("-fx-text-fill: #334155;");

        card.getChildren().addAll(title, subtitle, details);
        return card;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMATTER);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String fallback(String value) {
        String normalized = MedicalValidationService.normalize(value);
        return normalized.isBlank() ? "-" : normalized;
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }

    private static final class TimelineEntry {
        private final String type;
        private final LocalDateTime when;
        private final String title;
        private final String subtitle;
        private final String details;
        private final String accentColor;

        private TimelineEntry(String type, LocalDateTime when, String title, String subtitle, String details, String accentColor) {
            this.type = type;
            this.when = when;
            this.title = title;
            this.subtitle = subtitle;
            this.details = details;
            this.accentColor = accentColor;
        }
    }
}
