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

public class MedicalManagementController {

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
    @FXML private Label subtitleLabel;
    @FXML private ComboBox<User> patientComboBox;
    @FXML private TextArea dossierReminderArea;
    @FXML private TextArea dossierHistoryArea;
    @FXML private Button saveDossierButton;
    @FXML private Button deleteDossierButton;
    @FXML private ComboBox<String> antecedentTypeField;
    @FXML private TextArea antecedentDescriptionArea;
    @FXML private DatePicker antecedentDatePicker;
    @FXML private Button saveAntecedentButton;
    @FXML private Button cancelAntecedentButton;
    @FXML private VBox antecedentListContainer;
    @FXML private VBox journalListContainer;
    @FXML private VBox testResultListContainer;
    @FXML private DatePicker consultationDatePicker;
    @FXML private ComboBox<Consultation> consultationComboBox;
    @FXML private TextArea consultationPatientNotesArea;
    @FXML private TextArea consultationPsychologueNotesArea;
    @FXML private Button saveConsultationButton;
    @FXML private Button cancelConsultationButton;
    @FXML private VBox consultationListContainer;
    @FXML private Label emptyPatientLabel;

    private final MedicalDataService medicalDataService = new MedicalDataService();
    private final JournalService journalService = new JournalService();

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
        configureAntecedentInputs();
        if (consultationComboBox != null) {
            consultationComboBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(Consultation consultation) {
                    if (consultation == null) {
                        return "";
                    }
                    String rendezVousText = consultation.getRendezVousId() == null
                            ? "sans RDV"
                            : "RDV #" + consultation.getRendezVousId();
                    return consultation.getConsultationDate().format(DATE_FORMATTER) + " - " + rendezVousText;
                }

                @Override
                public Consultation fromString(String string) {
                    return null;
                }
            });
            consultationComboBox.setOnAction(event -> {
                Consultation consultation = consultationComboBox.getValue();
                if (consultation != null) {
                    startConsultationEdit(consultation);
                }
            });
        }
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
        antecedentMedical.setType(MedicalValidationService.normalize(antecedentTypeField.getValue()));
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
        if (consultationPsychologueNotesArea == null) {
            showError("L'interface consultation n'est plus disponible.");
            return;
        }
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
        antecedentTypeField.setEditable(false);
        antecedentTypeField.setDisable(!adminMode);
        antecedentDescriptionArea.setEditable(adminMode);
        antecedentDatePicker.setDisable(!adminMode);

        if (consultationDatePicker != null) {
            consultationDatePicker.setDisable(!adminMode);
        }
        if (consultationPatientNotesArea != null) {
            consultationPatientNotesArea.setEditable(adminMode);
        }
        if (consultationPsychologueNotesArea != null) {
            consultationPsychologueNotesArea.setEditable(true);
        }
        if (saveConsultationButton != null) {
            saveConsultationButton.setText(adminMode ? "Mettre a jour la consultation" : "Enregistrer la note psychologue");
        }
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
            loadJournals(patient.getId());
            loadTestResults(patient.getId());
            if (consultationListContainer != null) {
                loadConsultations(patient.getId());
            }
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
        antecedentTypeField.getSelectionModel().select(antecedentMedical.getType());
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
        if (consultationListContainer == null) {
            return;
        }
        consultationListContainer.getChildren().clear();
        List<Consultation> consultations = mode == Mode.PSYCHOLOGUE && currentUser != null
                ? medicalDataService.getConsultationsByPsychologue(currentUser.getId()).stream()
                .filter(consultation -> consultation.getPatientId() == patientId)
                .toList()
                : medicalDataService.getConsultationsByPatient(patientId);
        if (consultationComboBox != null) {
            consultationComboBox.setItems(FXCollections.observableArrayList(consultations));
            consultationComboBox.getSelectionModel().clearSelection();
        }
        if (consultations.isEmpty()) {
            Label empty = new Label("Aucune consultation.");
            empty.setStyle("-fx-text-fill: #64748b;");
            consultationListContainer.getChildren().add(empty);
            return;
        }

        if (consultations.size() == 1) {
            startConsultationEdit(consultations.getFirst());
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

        Label rendezVousLabel = new Label(consultation.getRendezVousId() == null
                ? "Sans rendez-vous lie"
                : "Rendez-vous associe #" + consultation.getRendezVousId());
        rendezVousLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: bold;");

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

        card.getChildren().addAll(date, rendezVousLabel, patientNotes, psyNote, selectButton);

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
        if (consultationComboBox != null) {
            consultationComboBox.getSelectionModel().select(consultation);
        }
        if (consultationDatePicker != null) {
            consultationDatePicker.setValue(consultation.getConsultationDate());
        }
        if (consultationPatientNotesArea != null) {
            consultationPatientNotesArea.setText(consultation.getNotesPatient());
        }
        if (consultationPsychologueNotesArea != null) {
            consultationPsychologueNotesArea.setText(MedicalValidationService.normalize(consultation.getNotesPsychologue()));
        }
        if (cancelConsultationButton != null) {
            cancelConsultationButton.setVisible(true);
            cancelConsultationButton.setManaged(true);
        }
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
        antecedentTypeField.getSelectionModel().clearSelection();
        antecedentDescriptionArea.clear();
        antecedentDatePicker.setValue(null);
        saveAntecedentButton.setText("Enregistrer l'antecedent");
        cancelAntecedentButton.setVisible(mode == Mode.ADMIN && selectedAntecedent != null);
        cancelAntecedentButton.setManaged(mode == Mode.ADMIN && selectedAntecedent != null);
    }

    private void resetConsultationForm() {
        selectedConsultation = null;
        if (consultationComboBox != null) {
            consultationComboBox.getSelectionModel().clearSelection();
        }
        if (consultationDatePicker != null) {
            consultationDatePicker.setValue(null);
            consultationDatePicker.setDisable(true);
        }
        if (consultationPatientNotesArea != null) {
            consultationPatientNotesArea.clear();
        }
        if (consultationPsychologueNotesArea != null) {
            consultationPsychologueNotesArea.clear();
        }
        if (cancelConsultationButton != null) {
            cancelConsultationButton.setVisible(false);
            cancelConsultationButton.setManaged(false);
        }
    }

    private void clearAllPatientData() {
        emptyPatientLabel.setVisible(true);
        emptyPatientLabel.setManaged(true);
        dossierReminderArea.clear();
        dossierHistoryArea.clear();
        antecedentListContainer.getChildren().clear();
        if (journalListContainer != null) {
            journalListContainer.getChildren().clear();
        }
        if (testResultListContainer != null) {
            testResultListContainer.getChildren().clear();
        }
        if (consultationListContainer != null) {
            consultationListContainer.getChildren().clear();
        }
        resetAntecedentForm();
        resetConsultationForm();
    }

    private void loadJournals(int patientId) throws SQLException {
        if (journalListContainer == null) {
            return;
        }
        journalListContainer.getChildren().clear();
        List<Journal> journals = journalService.findByUserId(patientId);
        if (journals.isEmpty()) {
            journalListContainer.getChildren().add(buildEmptyLabel("Aucun journal."));
            return;
        }
        for (Journal journal : journals) {
            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 12;");
            Label header = new Label(journal.getDateCreationFormatted() + " | Humeur : " + journal.getHumeur());
            header.setStyle("-fx-font-weight: bold;");
            Label analyse = new Label("Analyse emotionnelle : " +
                    (MedicalValidationService.normalize(journal.getEtatAnalyse()).isBlank() ? "Non analysee" : journal.getEtatAnalyse()));
            analyse.setStyle("-fx-text-fill: #475569;");
            Label content = new Label(journal.getContenu());
            content.setWrapText(true);
            card.getChildren().addAll(header, analyse, content);
            journalListContainer.getChildren().add(card);
        }
    }

    private void loadTestResults(int patientId) throws SQLException {
        if (testResultListContainer == null) {
            return;
        }
        testResultListContainer.getChildren().clear();
        List<TestResultMedical> results = medicalDataService.getTestResultsByPatient(patientId);
        if (results.isEmpty()) {
            testResultListContainer.getChildren().add(buildEmptyLabel("Aucun resultat de test."));
            return;
        }
        for (TestResultMedical result : results) {
            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 12;");
            Label title = new Label(formatCategorie(result.getCategorie()) + " - " + result.getScore() + " / " + result.getScoreMax());
            title.setStyle("-fx-font-weight: bold;");
            Label date = new Label("Passe le " + formatDateTime(result.getCreatedAt()));
            date.setStyle("-fx-text-fill: #475569;");
            card.getChildren().addAll(title, date);
            testResultListContainer.getChildren().add(card);
        }
    }

    private void configureAntecedentInputs() {
        antecedentTypeField.setItems(FXCollections.observableArrayList(ANTECEDENT_TYPES));
        antecedentDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                boolean disabled = empty || item == null || !item.isBefore(LocalDate.now());
                setDisable(disabled);
            }
        });
    }

    private Label buildEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748b;");
        return label;
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
