package org.example.controller;

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
import org.example.entities.AntecedentMedical;
import org.example.entities.Consultation;
import org.example.entities.DossierMedical;
import org.example.entities.Journal;
import org.example.entities.TestResultMedical;
import org.example.entities.User;
import org.example.service.JournalService;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedicalRecordController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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
    @FXML private Label consultationCountLabel;
    @FXML private Label dossierCreationLabel;
    @FXML private Label lastConsultationLabel;

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
    @FXML private VBox journalsContainer;
    @FXML private VBox testResultsContainer;
    @FXML private Button saveAntecedentButton;
    @FXML private Button cancelAntecedentButton;
    @FXML private Button deleteAntecedentButton;
    private final MedicalDataService medicalDataService = new MedicalDataService();
    private final JournalService journalService = new JournalService();

    private User currentUser;
    private AntecedentMedical selectedAntecedent;

    @FXML
    public void initialize() {
        configureAntecedentInputs();
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
            loadMedicalRecord();
            loadAntecedents();
            loadConsultationStats();
            loadJournals();
            loadTestResults();
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
            showInfo(creation
                    ? "Antecedent medical ajoute avec succes."
                    : "Antecedent medical modifie avec succes.");
        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement de l'antecedent : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelAntecedentEdit() {
        resetAntecedentForm();
    }

    @FXML
    private void handleDeleteSelectedAntecedent() {
        if (selectedAntecedent == null) {
            showError("Veuillez selectionner un antecedent a supprimer.");
            return;
        }
        deleteAntecedent(selectedAntecedent);
    }

    @FXML
    private void returnToDashboard() {
        navigateTo("/patient_dashboard.fxml", PatientDashboardController.class);
    }

    @FXML
    private void goToConsultations() {
        navigateTo("/consultations.fxml", ConsultationController.class);
    }

    @FXML
    private void goToProfil() {
        navigateTo("/profil_patient.fxml", ProfilPatientController.class);
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

    private void navigateTo(String path, Class<?> controllerClass) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof PatientDashboardController patientDashboardController) {
                patientDashboardController.setUserData(UserSession.getInstance());
            } else if (controller instanceof ConsultationController consultationController) {
                consultationController.setUserData(UserSession.getInstance());
            } else if (controller instanceof ProfilPatientController profilPatientController) {
                profilPatientController.setUserData(UserSession.getInstance());
            } else {
                controllerClass.getMethod("setUserData", User.class).invoke(controller, UserSession.getInstance());
            }
            titleLabel.getScene().setRoot(view);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de charger la vue demandee.");
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

    private void loadMedicalRecord() throws SQLException {
        DossierMedical dossierMedical = medicalDataService.getMedicalRecordByPatient(currentUser.getId());
        if (dossierMedical == null) {
            dossierCreationLabel.setText("-");
            return;
        }

        dossierCreationLabel.setText(dossierMedical.getCreatedAt() == null ? "-" : dossierMedical.getCreatedAt().toLocalDate().format(DATE_FORMATTER));
    }

    private void loadAntecedents() throws SQLException {
        antecedentsContainer.getChildren().clear();
        List<AntecedentMedical> antecedents = medicalDataService.getAntecedentsByPatient(currentUser.getId());

        if (antecedents.isEmpty()) {
            Label emptyLabel = new Label("Aucun antecedent medical enregistre.");
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
            antecedentsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (AntecedentMedical antecedent : antecedents) {
            antecedentsContainer.getChildren().add(AntecedentCardFactory.build(
                    antecedent,
                    this::startAntecedentEdition,
                    this::deleteAntecedent
            ));
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
            showInfo("Antecedent medical supprime avec succes.");
        } catch (SQLException e) {
            showError("Impossible de supprimer l'antecedent : " + e.getMessage());
        }
    }

    private void loadConsultationStats() throws SQLException {
        List<Consultation> consultations = medicalDataService.getConsultationsByPatient(currentUser.getId());
        consultationCountLabel.setText(String.valueOf(consultations.size()));
        if (consultations.isEmpty()) {
            lastConsultationLabel.setText("-");
        } else {
            lastConsultationLabel.setText(consultations.get(0).getConsultationDate().format(DATE_FORMATTER));
        }
    }

    private void loadJournals() throws SQLException {
        journalsContainer.getChildren().clear();
        List<Journal> journals = journalService.findByUserId(currentUser.getId());
        if (journals.isEmpty()) {
            clearContainer(journalsContainer, "Aucun journal a afficher.");
            return;
        }

        for (Journal journal : journals) {
            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 12;");
            Label header = new Label(journal.getDateCreationFormatted() + " | Humeur : " + journal.getHumeur());
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937;");
            Label analyse = new Label("Analyse emotionnelle : " +
                    (MedicalValidationService.normalize(journal.getEtatAnalyse()).isBlank() ? "Non analysee" : journal.getEtatAnalyse()));
            analyse.setStyle("-fx-text-fill: #475569;");
            Label content = new Label(journal.getContenu());
            content.setWrapText(true);
            card.getChildren().addAll(header, analyse, content);
            journalsContainer.getChildren().add(card);
        }
    }

    private void loadTestResults() throws SQLException {
        testResultsContainer.getChildren().clear();
        List<TestResultMedical> results = medicalDataService.getTestResultsByPatient(currentUser.getId());
        if (results.isEmpty()) {
            clearContainer(testResultsContainer, "Aucun resultat de test a afficher.");
            return;
        }

        for (TestResultMedical result : results) {
            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 12;");
            Label header = new Label(formatCategorie(result.getCategorie()) + " - " + result.getScore() + " / " + result.getScoreMax());
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937;");
            Label date = new Label("Passe le " + formatDateTime(result.getCreatedAt()));
            date.setStyle("-fx-text-fill: #475569;");
            card.getChildren().addAll(header, date);
            testResultsContainer.getChildren().add(card);
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

    private void clearContainer(VBox container, String message) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        Label emptyLabel = new Label(message);
        emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-padding: 10 0;");
        container.getChildren().add(emptyLabel);
    }

    private String formatCategorie(String categorie) {
        String value = MedicalValidationService.normalize(categorie);
        if (value.isBlank()) {
            return "Test";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
