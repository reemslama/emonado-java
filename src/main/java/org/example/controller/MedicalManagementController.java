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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.example.entities.AntecedentMedical;
import org.example.entities.Consultation;
import org.example.entities.DossierMedical;
import org.example.entities.User;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedicalManagementController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ComboBox<User> patientComboBox;
    @FXML private TextArea dossierReminderArea;
    @FXML private TextArea dossierHistoryArea;
    @FXML private Button saveDossierButton;
    @FXML private Button deleteDossierButton;
    @FXML private TextField antecedentTypeField;
    @FXML private TextArea antecedentDescriptionArea;
    @FXML private DatePicker antecedentDatePicker;
    @FXML private Button saveAntecedentButton;
    @FXML private Button cancelAntecedentButton;
    @FXML private VBox antecedentListContainer;
    @FXML private DatePicker consultationDatePicker;
    @FXML private TextArea consultationPatientNotesArea;
    @FXML private TextArea consultationPsychologueNotesArea;
    @FXML private Button saveConsultationButton;
    @FXML private Button cancelConsultationButton;
    @FXML private VBox consultationListContainer;
    @FXML private Label emptyPatientLabel;

    private final MedicalDataService medicalDataService = new MedicalDataService();

    private User currentUser;
    private Mode mode;
    private AntecedentMedical selectedAntecedent;
    private Consultation selectedConsultation;

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
    private void handleSaveDossier() {
        User patient = patientComboBox.getValue();
        if (!ensurePatientSelected(patient)) {
            return;
        }

        DossierMedical dossierMedical = new DossierMedical();
        dossierMedical.setPatientId(patient.getId());
        dossierMedical.setReminderText(MedicalValidationService.normalize(dossierReminderArea.getText()));
        dossierMedical.setMedicalHistory(MedicalValidationService.normalize(dossierHistoryArea.getText()));

        String validationError = MedicalValidationService.validateDossier(dossierMedical);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            medicalDataService.saveMedicalRecord(dossierMedical);
            loadSelectedPatientData();
            showInfo("Dossier medical enregistre.");
        } catch (SQLException e) {
            showError("Impossible d'enregistrer le dossier medical : " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteDossier() {
        User patient = patientComboBox.getValue();
        if (!ensurePatientSelected(patient)) {
            return;
        }

        try {
            medicalDataService.deleteMedicalRecord(patient.getId());
            loadSelectedPatientData();
            showInfo("Dossier medical supprime.");
        } catch (SQLException e) {
            showError("Impossible de supprimer le dossier medical : " + e.getMessage());
        }
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
    private void handleSaveConsultation() {
        User patient = patientComboBox.getValue();
        if (!ensurePatientSelected(patient)) {
            return;
        }

        if (mode == Mode.PSYCHOLOGUE) {
            if (selectedConsultation == null) {
                showError("Selectionnez une consultation pour ajouter une note psychologue.");
                return;
            }

            String validationError = MedicalValidationService.validatePsychologueNote(consultationPsychologueNotesArea.getText());
            if (validationError != null) {
                showError(validationError);
                return;
            }

            try {
                medicalDataService.updatePsychologueNote(
                        selectedConsultation.getId(),
                        MedicalValidationService.normalize(consultationPsychologueNotesArea.getText()),
                        currentUser == null ? null : currentUser.getId()
                );
                loadConsultations(patient.getId());
                showInfo("Note psychologue enregistree.");
            } catch (SQLException e) {
                showError("Impossible d'enregistrer la note psychologue : " + e.getMessage());
            }
            return;
        }

        if (selectedConsultation == null) {
            showError("Selectionnez une consultation a modifier.");
            return;
        }

        selectedConsultation.setConsultationDate(consultationDatePicker.getValue());
        selectedConsultation.setNotesPatient(MedicalValidationService.normalize(consultationPatientNotesArea.getText()));
        selectedConsultation.setNotesPsychologue(MedicalValidationService.normalize(consultationPsychologueNotesArea.getText()));

        String validationError = MedicalValidationService.validateConsultationPatientData(selectedConsultation);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        if (!selectedConsultation.getNotesPsychologue().isBlank()) {
            validationError = MedicalValidationService.validatePsychologueNote(selectedConsultation.getNotesPsychologue());
            if (validationError != null) {
                showError(validationError);
                return;
            }
        }

        try {
            medicalDataService.saveConsultation(selectedConsultation);
            loadConsultations(patient.getId());
            showInfo("Consultation modifiee.");
        } catch (SQLException e) {
            showError("Impossible de modifier la consultation : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelConsultation() {
        resetConsultationForm();
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

    private void configureMode() {
        boolean adminMode = mode == Mode.ADMIN;
        titleLabel.setText(adminMode ? "Gestion medicale admin" : "Suivi medical psychologue");
        subtitleLabel.setText(adminMode
                ? "Consultation, modification et suppression des dossiers, antecedents et consultations."
                : "Consultation des dossiers patients et ajout de notes psychologue sur les consultations.");

        saveDossierButton.setVisible(adminMode);
        saveDossierButton.setManaged(adminMode);
        deleteDossierButton.setVisible(adminMode);
        deleteDossierButton.setManaged(adminMode);

        dossierReminderArea.setEditable(adminMode);
        dossierHistoryArea.setEditable(adminMode);

        saveAntecedentButton.setVisible(adminMode);
        saveAntecedentButton.setManaged(adminMode);
        cancelAntecedentButton.setVisible(adminMode);
        cancelAntecedentButton.setManaged(adminMode);
        antecedentTypeField.setEditable(adminMode);
        antecedentDescriptionArea.setEditable(adminMode);
        antecedentDatePicker.setDisable(!adminMode);

        consultationDatePicker.setDisable(!adminMode);
        consultationPatientNotesArea.setEditable(adminMode);
        consultationPsychologueNotesArea.setEditable(true);
        saveConsultationButton.setText(adminMode ? "Mettre a jour la consultation" : "Enregistrer la note psychologue");
    }

    private void loadPatients() {
        try {
            medicalDataService.ensureSchema();
            List<User> patients = medicalDataService.getAllPatients();
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
            dossierReminderArea.setText(dossierMedical == null ? "" : MedicalValidationService.normalize(dossierMedical.getReminderText()));
            dossierHistoryArea.setText(dossierMedical == null ? "" : MedicalValidationService.normalize(dossierMedical.getMedicalHistory()));
            loadAntecedents(patient.getId());
            loadConsultations(patient.getId());
            resetAntecedentForm();
            resetConsultationForm();
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

    private void loadConsultations(int patientId) throws SQLException {
        consultationListContainer.getChildren().clear();
        List<Consultation> consultations = medicalDataService.getConsultationsByPatient(patientId);
        if (consultations.isEmpty()) {
            Label empty = new Label("Aucune consultation.");
            empty.setStyle("-fx-text-fill: #64748b;");
            consultationListContainer.getChildren().add(empty);
            return;
        }

        for (Consultation consultation : consultations) {
            consultationListContainer.getChildren().add(buildConsultationItem(consultation));
        }
    }

    private VBox buildConsultationItem(Consultation consultation) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 12; "
                + "-fx-border-color: #cbd5e1; -fx-border-radius: 10;");

        Label date = new Label("Consultation du " + consultation.getConsultationDate().format(DATE_FORMATTER));
        date.setStyle("-fx-font-weight: bold;");

        Label patientNotes = new Label("Compte rendu patient : " + consultation.getNotesPatient());
        patientNotes.setWrapText(true);

        String psyNoteText = MedicalValidationService.normalize(consultation.getNotesPsychologue()).isBlank()
                ? "Aucune note psychologue."
                : consultation.getNotesPsychologue();
        Label psyNote = new Label("Note psychologue : " + psyNoteText);
        psyNote.setWrapText(true);

        Button selectButton = new Button(mode == Mode.ADMIN ? "Modifier" : "Ajouter / modifier note");
        selectButton.setStyle("-fx-background-color: #0d6efd; -fx-text-fill: white;");
        selectButton.setOnAction(event -> startConsultationEdit(consultation));

        card.getChildren().addAll(date, patientNotes, psyNote, selectButton);

        if (mode == Mode.ADMIN) {
            Button deleteButton = new Button("Supprimer");
            deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            deleteButton.setOnAction(event -> deleteConsultation(consultation));
            card.getChildren().add(deleteButton);
        }

        return card;
    }

    private void startConsultationEdit(Consultation consultation) {
        selectedConsultation = consultation;
        consultationDatePicker.setValue(consultation.getConsultationDate());
        consultationPatientNotesArea.setText(consultation.getNotesPatient());
        consultationPsychologueNotesArea.setText(MedicalValidationService.normalize(consultation.getNotesPsychologue()));
        cancelConsultationButton.setVisible(true);
        cancelConsultationButton.setManaged(true);
    }

    private void deleteConsultation(Consultation consultation) {
        try {
            medicalDataService.deleteConsultation(consultation.getId());
            loadConsultations(patientComboBox.getValue().getId());
            resetConsultationForm();
            showInfo("Consultation supprimee.");
        } catch (SQLException e) {
            showError("Impossible de supprimer la consultation : " + e.getMessage());
        }
    }

    private void resetAntecedentForm() {
        selectedAntecedent = null;
        antecedentTypeField.clear();
        antecedentDescriptionArea.clear();
        antecedentDatePicker.setValue(null);
        saveAntecedentButton.setText("Enregistrer l'antecedent");
        cancelAntecedentButton.setVisible(mode == Mode.ADMIN && selectedAntecedent != null);
        cancelAntecedentButton.setManaged(mode == Mode.ADMIN && selectedAntecedent != null);
    }

    private void resetConsultationForm() {
        selectedConsultation = null;
        consultationDatePicker.setValue(null);
        consultationPatientNotesArea.clear();
        consultationPsychologueNotesArea.clear();
        cancelConsultationButton.setVisible(false);
        cancelConsultationButton.setManaged(false);
    }

    private void clearAllPatientData() {
        emptyPatientLabel.setVisible(true);
        emptyPatientLabel.setManaged(true);
        dossierReminderArea.clear();
        dossierHistoryArea.clear();
        antecedentListContainer.getChildren().clear();
        consultationListContainer.getChildren().clear();
        resetAntecedentForm();
        resetConsultationForm();
    }

    private boolean ensurePatientSelected(User patient) {
        if (patient != null) {
            return true;
        }
        showError("Veuillez selectionner un patient.");
        return false;
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
