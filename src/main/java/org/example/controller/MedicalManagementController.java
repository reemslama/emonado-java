package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import org.example.entities.DossierMedical;
import org.example.entities.TestResultMedical;
import org.example.entities.User;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.service.PdfExportService;
import org.example.utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedicalManagementController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ComboBox<User> patientComboBox;
    @FXML private TextArea dossierPsychologueNoteArea;
    @FXML private Button savePsychologueNoteButton;
    @FXML private VBox psychologueNoteBox;
    @FXML private VBox antecedentEditorBox;
    @FXML private TextField antecedentTypeField;
    @FXML private TextArea antecedentDescriptionArea;
    @FXML private DatePicker antecedentDatePicker;
    @FXML private Button saveAntecedentButton;
    @FXML private Button cancelAntecedentButton;
    @FXML private VBox antecedentListContainer;
    @FXML private VBox testResultsContainer;
    @FXML private Label emptyPatientLabel;

    private final MedicalDataService medicalDataService = new MedicalDataService();
    private final PdfExportService pdfExportService = new PdfExportService();

    private User currentUser;
    private Mode mode;
    private AntecedentMedical selectedAntecedent;

    @FXML
    public void initialize() {
        patientComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getNom() + " " + user.getPrenom() + " (" + user.getEmail() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        patientComboBox.setOnAction(event -> loadSelectedPatientData());
    }

    public void initForAdmin(User user) {
        currentUser = user;
        mode = Mode.ADMIN;
        configureMode();
        loadPatients();
    }

    public void initForPsychologue(User user) {
        currentUser = user;
        mode = Mode.PSYCHOLOGUE;
        configureMode();
        loadPatients();
    }

    @FXML
    private void handleRefreshPatients() {
        loadPatients();
    }

    @FXML
    private void handleSaveAntecedent() {
        User patient = patientComboBox.getValue();
        if (!ensurePatientSelected(patient)) {
            return;
        }

        AntecedentMedical antecedentMedical = selectedAntecedent == null ? new AntecedentMedical() : selectedAntecedent;
        antecedentMedical.setType(MedicalValidationService.normalize(antecedentTypeField.getText()));
        antecedentMedical.setDescription(MedicalValidationService.normalize(antecedentDescriptionArea.getText()));
        antecedentMedical.setDateDiagnostic(antecedentDatePicker.getValue());

        String validationError = MedicalValidationService.validateAntecedent(antecedentMedical);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            boolean creation = selectedAntecedent == null;
            medicalDataService.saveAntecedent(patient.getId(), antecedentMedical);
            loadAntecedents(patient.getId());
            resetAntecedentForm();
            showInfo(creation ? "Antecedent ajoute." : "Antecedent modifie.");
        } catch (SQLException e) {
            showError("Impossible d'enregistrer l'antecedent : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelAntecedent() {
        resetAntecedentForm();
    }

    @FXML
    private void handleSavePsychologueDossierNote() {
        User patient = patientComboBox.getValue();
        if (!ensurePatientSelected(patient)) {
            return;
        }
        if (mode != Mode.PSYCHOLOGUE) {
            showError("Seul le psychologue peut modifier cette note.");
            return;
        }

        String validationError = MedicalValidationService.validatePsychologueNote(dossierPsychologueNoteArea.getText());
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            medicalDataService.updateMedicalRecordPsychologueNote(
                    patient.getId(),
                    MedicalValidationService.normalize(dossierPsychologueNoteArea.getText()),
                    currentUser == null ? null : currentUser.getId()
            );
            loadSelectedPatientData();
            showInfo("Note du dossier enregistree.");
        } catch (SQLException e) {
            showError("Impossible d'enregistrer la note du dossier : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        if (mode == Mode.ADMIN) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin_dashboard.fxml"));
                Parent root = loader.load();
                AdminDashboardController controller = loader.getController();
                controller.setUserData(UserSession.getInstance());
                titleLabel.getScene().setRoot(root);
            } catch (IOException e) {
                showError("Impossible de revenir au dashboard admin.");
            }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/psy_dashboard.fxml"));
            Parent root = loader.load();
            PsyDashboardController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Impossible de revenir au dashboard psychologue.");
        }
    }

    @FXML
    private void handleExportPdf() {
        User patient = patientComboBox.getValue();
        if (!ensurePatientSelected(patient)) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le dossier medical en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName("dossier-medical-" + patient.getNom() + "-" + patient.getPrenom() + ".pdf");
        File targetFile = fileChooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (targetFile == null) {
            return;
        }

        try {
            DossierMedical dossierMedical = medicalDataService.getMedicalRecordByPatient(patient.getId());
            List<AntecedentMedical> antecedents = medicalDataService.getAntecedentsByPatient(patient.getId());
            List<TestResultMedical> testResults = medicalDataService.getTestResultsByPatient(patient.getId());
            pdfExportService.exportPsychologueMedicalRecord(targetFile.toPath(), patient, dossierMedical, antecedents, testResults);
            showInfo("PDF exporte : " + targetFile.getAbsolutePath());
        } catch (Exception e) {
            showError("Impossible d'exporter le PDF : " + e.getMessage());
        }
    }

    private void configureMode() {
        boolean adminMode = mode == Mode.ADMIN;
        titleLabel.setText(adminMode ? "Gestion medicale admin" : "Suivi medical psychologue");
        subtitleLabel.setText(adminMode
                ? "Antecedents et resultats de test centralises dans le dossier du patient."
                : "Note du dossier, antecedents et resultats de test du patient.");

        saveAntecedentButton.setVisible(adminMode);
        saveAntecedentButton.setManaged(adminMode);
        cancelAntecedentButton.setVisible(adminMode);
        cancelAntecedentButton.setManaged(adminMode);
        antecedentTypeField.setEditable(adminMode);
        antecedentDescriptionArea.setEditable(adminMode);
        antecedentDatePicker.setDisable(!adminMode);

        if (psychologueNoteBox != null) {
            psychologueNoteBox.setVisible(true);
            psychologueNoteBox.setManaged(true);
        }
        if (savePsychologueNoteButton != null) {
            savePsychologueNoteButton.setVisible(!adminMode);
            savePsychologueNoteButton.setManaged(!adminMode);
        }
        if (antecedentEditorBox != null) {
            antecedentEditorBox.setVisible(adminMode);
            antecedentEditorBox.setManaged(adminMode);
        }
    }

    private void loadPatients() {
        try {
            medicalDataService.ensureSchema();
            List<User> patients = mode == Mode.PSYCHOLOGUE && currentUser != null
                    ? medicalDataService.getPatientsForPsychologue(currentUser.getId())
                    : medicalDataService.getAllPatients();
            patientComboBox.setItems(FXCollections.observableArrayList(patients));
            if (!patients.isEmpty()) {
                patientComboBox.getSelectionModel().selectFirst();
                loadSelectedPatientData();
            } else {
                clearAllPatientData();
            }
        } catch (SQLException e) {
            showError("Impossible de charger les patients : " + e.getMessage());
        }
    }

    private void loadSelectedPatientData() {
        User patient = patientComboBox.getValue();
        if (patient == null) {
            clearAllPatientData();
            return;
        }

        emptyPatientLabel.setVisible(false);
        emptyPatientLabel.setManaged(false);

        try {
            DossierMedical dossierMedical = medicalDataService.getMedicalRecordByPatient(patient.getId());
            if (dossierPsychologueNoteArea != null) {
                String note = dossierMedical == null ? "" : MedicalValidationService.normalize(dossierMedical.getPsychologueNote());
                dossierPsychologueNoteArea.setText(note);
                dossierPsychologueNoteArea.setPromptText(mode == Mode.PSYCHOLOGUE
                        ? "Ajoutez une note clinique visible dans le dossier du patient..."
                        : "");
                dossierPsychologueNoteArea.setEditable(mode == Mode.PSYCHOLOGUE);
            }
            loadAntecedents(patient.getId());
            loadTestResults(patient.getId());
            resetAntecedentForm();
        } catch (SQLException e) {
            showError("Impossible de charger les donnees du patient : " + e.getMessage());
        }
    }

    private void loadAntecedents(int patientId) throws SQLException {
        antecedentListContainer.getChildren().clear();
        List<AntecedentMedical> antecedents = medicalDataService.getAntecedentsByPatient(patientId);
        if (antecedents.isEmpty()) {
            Label empty = new Label("Aucun antecedent medical.");
            empty.setStyle("-fx-text-fill: #64748b;");
            antecedentListContainer.getChildren().add(empty);
            return;
        }

        for (AntecedentMedical antecedent : antecedents) {
            antecedentListContainer.getChildren().add(buildAntecedentItem(antecedent));
        }
    }

    private void loadTestResults(int patientId) throws SQLException {
        testResultsContainer.getChildren().clear();
        List<TestResultMedical> testResults = medicalDataService.getTestResultsByPatient(patientId);
        if (testResults.isEmpty()) {
            Label empty = new Label("Aucun resultat de test disponible.");
            empty.setStyle("-fx-text-fill: #64748b;");
            testResultsContainer.getChildren().add(empty);
            return;
        }

        for (TestResultMedical testResult : testResults) {
            testResultsContainer.getChildren().add(buildTestResultCard(testResult));
        }
    }

    private VBox buildAntecedentItem(AntecedentMedical antecedentMedical) {
        if (mode == Mode.ADMIN) {
            return AntecedentCardFactory.build(antecedentMedical, this::startAntecedentEdit, this::deleteAntecedent);
        }

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 12;");
        Label title = new Label(antecedentMedical.getType());
        title.setStyle("-fx-font-weight: bold;");
        String date = antecedentMedical.getDateDiagnostic() == null ? "Date non precisee"
                : antecedentMedical.getDateDiagnostic().format(DATE_FORMATTER);
        Label dateLabel = new Label(date);
        Label desc = new Label(antecedentMedical.getDescription());
        desc.setWrapText(true);
        card.getChildren().addAll(title, dateLabel, desc);
        return card;
    }

    private void startAntecedentEdit(AntecedentMedical antecedentMedical) {
        selectedAntecedent = antecedentMedical;
        antecedentTypeField.setText(antecedentMedical.getType());
        antecedentDescriptionArea.setText(antecedentMedical.getDescription());
        antecedentDatePicker.setValue(antecedentMedical.getDateDiagnostic());
        cancelAntecedentButton.setVisible(true);
        cancelAntecedentButton.setManaged(true);
        saveAntecedentButton.setText("Mettre a jour l'antecedent");
    }

    private void deleteAntecedent(AntecedentMedical antecedentMedical) {
        try {
            medicalDataService.deleteAntecedent(antecedentMedical.getId());
            loadAntecedents(patientComboBox.getValue().getId());
            resetAntecedentForm();
            showInfo("Antecedent supprime.");
        } catch (SQLException e) {
            showError("Impossible de supprimer l'antecedent : " + e.getMessage());
        }
    }

    private void resetAntecedentForm() {
        selectedAntecedent = null;
        if (antecedentTypeField != null) {
            antecedentTypeField.clear();
        }
        if (antecedentDescriptionArea != null) {
            antecedentDescriptionArea.clear();
        }
        if (antecedentDatePicker != null) {
            antecedentDatePicker.setValue(null);
        }
        if (saveAntecedentButton != null) {
            saveAntecedentButton.setText("Enregistrer l'antecedent");
        }
        if (cancelAntecedentButton != null) {
            cancelAntecedentButton.setVisible(false);
            cancelAntecedentButton.setManaged(false);
        }
    }

    private void clearAllPatientData() {
        emptyPatientLabel.setVisible(true);
        emptyPatientLabel.setManaged(true);
        if (dossierPsychologueNoteArea != null) {
            dossierPsychologueNoteArea.clear();
        }
        antecedentListContainer.getChildren().clear();
        testResultsContainer.getChildren().clear();
        resetAntecedentForm();
    }

    private boolean ensurePatientSelected(User patient) {
        if (patient != null) {
            return true;
        }
        showError("Veuillez selectionner un patient.");
        return false;
    }

    private VBox buildTestResultCard(TestResultMedical testResult) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #fff8ed; -fx-background-radius: 16; -fx-padding: 14; "
                + "-fx-border-color: #efcf95; -fx-border-radius: 16;");

        Label category = new Label(testResult.getCategorie());
        category.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #8a4b12;");

        double ratio = testResult.getScoreMax() <= 0 ? 0 : (double) testResult.getScore() / testResult.getScoreMax();
        String level = ratio >= 0.7 ? "niveau eleve" : ratio >= 0.4 ? "niveau modere" : "niveau faible";

        Label score = new Label("Score : " + testResult.getScore() + " / " + testResult.getScoreMax());
        score.setStyle("-fx-text-fill: #7c4a0d;");

        Label interpretation = new Label("Interpretation : " + level);
        interpretation.setStyle("-fx-text-fill: #7c4a0d;");

        String date = testResult.getCreatedAt() == null ? "-" : testResult.getCreatedAt().format(DATE_TIME_FORMATTER);
        Label createdAt = new Label("Passe le : " + date);
        createdAt.setStyle("-fx-text-fill: #8c6b4e; -fx-font-size: 13px;");

        card.getChildren().addAll(category, score, interpretation, createdAt);
        return card;
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }

    private enum Mode {
        ADMIN,
        PSYCHOLOGUE
    }
}
