package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.entities.AntecedentMedical;
import org.example.entities.Consultation;
import org.example.entities.DossierMedical;
import org.example.entities.User;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.service.UserService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedicalRecordController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

    @FXML private TextArea reminderArea;
    @FXML private TextArea medicalHistoryArea;
    @FXML private TextField antecedentTypeField;
    @FXML private TextArea antecedentDescriptionArea;
    @FXML private DatePicker antecedentDatePicker;
    @FXML private VBox antecedentsContainer;
    @FXML private Button saveAntecedentButton;
    @FXML private Button cancelAntecedentButton;
    @FXML private Button deleteAntecedentButton;
    @FXML private Button deleteMedicalRecordButton;

    private final MedicalDataService medicalDataService = new MedicalDataService();

    private User currentUser;
    private AntecedentMedical selectedAntecedent;

    @FXML
    public void initialize() {
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
            resetAntecedentForm();
        } catch (SQLException e) {
            showError("Impossible de charger le dossier medical : " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveMedicalRecord() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        DossierMedical dossierMedical = new DossierMedical();
        dossierMedical.setPatientId(currentUser.getId());
        dossierMedical.setReminderText(MedicalValidationService.normalize(reminderArea.getText()));
        dossierMedical.setMedicalHistory(MedicalValidationService.normalize(medicalHistoryArea.getText()));

        String validationError = MedicalValidationService.validateDossier(dossierMedical);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            medicalDataService.saveMedicalRecord(dossierMedical);
            loadMedicalRecord();
            showInfo("Dossier medical enregistre avec succes.");
        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement du dossier medical : " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteMedicalRecord() {
        if (currentUser == null) {
            return;
        }

        try {
            medicalDataService.deleteMedicalRecord(currentUser.getId());
            reminderArea.clear();
            medicalHistoryArea.clear();
            dossierCreationLabel.setText("-");
            consultationCountLabel.setText("0");
            lastConsultationLabel.setText("-");
            loadAntecedents();
            showInfo("Dossier medical supprime avec succes.");
        } catch (SQLException e) {
            showError("Impossible de supprimer le dossier medical : " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveAntecedent() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
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
    private void handleSavePersonalInfo() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        currentUser.setNom(MedicalValidationService.normalize(nomField.getText()));
        currentUser.setPrenom(MedicalValidationService.normalize(prenomField.getText()));
        currentUser.setEmail(MedicalValidationService.normalize(emailField.getText()));
        currentUser.setTelephone(MedicalValidationService.normalize(phoneField.getText()).replaceAll("\\D", ""));
        currentUser.setSexe(MedicalValidationService.normalize(sexeField.getText()));
        currentUser.setDateNaissance(birthDatePicker.getValue());

        String validationError = MedicalValidationService.validatePatientProfile(currentUser);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            UserService.updatePatientProfile(currentUser);
            UserSession.setInstance(currentUser);
            showInfo("Informations personnelles mises a jour.");
        } catch (SQLException e) {
            showError("Impossible de mettre a jour vos informations personnelles.");
        }
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
    }

    private void loadMedicalRecord() throws SQLException {
        DossierMedical dossierMedical = medicalDataService.getMedicalRecordByPatient(currentUser.getId());
        if (dossierMedical == null) {
            reminderArea.clear();
            medicalHistoryArea.clear();
            dossierCreationLabel.setText("-");
            return;
        }

        reminderArea.setText(MedicalValidationService.normalize(dossierMedical.getReminderText()));
        medicalHistoryArea.setText(MedicalValidationService.normalize(dossierMedical.getMedicalHistory()));
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
        antecedentTypeField.setText(antecedentMedical.getType());
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

    private void resetAntecedentForm() {
        selectedAntecedent = null;
        antecedentTypeField.clear();
        antecedentDescriptionArea.clear();
        antecedentDatePicker.setValue(null);
        saveAntecedentButton.setText("Ajouter l'antecedent medical");
        cancelAntecedentButton.setVisible(false);
        cancelAntecedentButton.setManaged(false);
        deleteAntecedentButton.setVisible(false);
        deleteAntecedentButton.setManaged(false);
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
