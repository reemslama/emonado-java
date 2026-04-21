package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.Consultation;
import org.example.entities.User;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConsultationController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");

    @FXML private Label pageTitleLabel;
    @FXML private Label historyCountLabel;
    @FXML private DatePicker consultationDatePicker;
    @FXML private TextArea consultationNotesArea;
    @FXML private TextArea psychologueNotesArea;
    @FXML private VBox consultationHistoryBox;
    @FXML private MenuButton filterMenuButton;
    @FXML private ToggleButton writeModeButton;
    @FXML private ToggleButton speakModeButton;
    @FXML private Button saveConsultationButton;
    @FXML private Button cancelEditButton;

    private final MedicalDataService medicalDataService = new MedicalDataService();

    private User currentUser;
    private FilterMode currentFilter = FilterMode.ALL;
    private Consultation selectedConsultation;

    @FXML
    public void initialize() {
        ToggleGroup modeGroup = new ToggleGroup();
        writeModeButton.setToggleGroup(modeGroup);
        speakModeButton.setToggleGroup(modeGroup);
        writeModeButton.setSelected(true);
        consultationDatePicker.setValue(LocalDate.now());
        psychologueNotesArea.setEditable(false);

        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        pageTitleLabel.setText("Mes Consultations");

        try {
            medicalDataService.ensureSchema();
            resetForm();
            loadConsultations();
        } catch (SQLException e) {
            showError("Impossible d'initialiser les consultations : " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveConsultation() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        if (selectedConsultation == null) {
            showError("Une consultation est creee automatiquement a partir d'un rendez-vous accepte. Selectionnez ensuite la consultation pour completer vos notes.");
            return;
        }

        Consultation consultation = selectedConsultation;
        consultation.setPatientId(currentUser.getId());
        consultation.setConsultationDate(consultationDatePicker.getValue());
        consultation.setNotesPatient(MedicalValidationService.normalize(consultationNotesArea.getText()));

        String validationError = MedicalValidationService.validateConsultationPatientData(consultation);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            medicalDataService.saveConsultation(consultation);
            showInfo("Consultation modifiee avec succes.");
            resetForm();
            loadConsultations();
        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement de la consultation : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelEdit() {
        resetForm();
    }

    @FXML
    private void activateWriteMode() {
        writeModeButton.setSelected(true);
    }

    @FXML
    private void activateSpeechMode() {
        speakModeButton.setSelected(false);
        writeModeButton.setSelected(true);
        showInfo("La saisie vocale n'est pas encore disponible. Utilisez la saisie ecrite.");
    }

    @FXML
    private void showAllConsultations() {
        currentFilter = FilterMode.ALL;
        filterMenuButton.setText("Filtrer : toutes");
        loadConsultations();
    }

    @FXML
    private void showUpcomingConsultations() {
        currentFilter = FilterMode.UPCOMING;
        filterMenuButton.setText("Filtrer : a venir");
        loadConsultations();
    }

    @FXML
    private void showPastConsultations() {
        currentFilter = FilterMode.PAST;
        filterMenuButton.setText("Filtrer : passees");
        loadConsultations();
    }

    @FXML
    private void returnToDashboard() {
        navigateTo("/patient_dashboard.fxml", PatientDashboardController.class);
    }

    @FXML
    private void goToMedicalRecord() {
        navigateTo("/medical_record.fxml", MedicalRecordController.class);
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent view = FXMLLoader.load(getClass().getResource("/login.fxml"));
            pageTitleLabel.getScene().setRoot(view);
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
            } else if (controller instanceof MedicalRecordController medicalRecordController) {
                medicalRecordController.setUserData(UserSession.getInstance());
            } else if (controller instanceof ProfilPatientController profilPatientController) {
                profilPatientController.setUserData(UserSession.getInstance());
            } else {
                controllerClass.getMethod("setUserData", User.class).invoke(controller, UserSession.getInstance());
            }
            pageTitleLabel.getScene().setRoot(view);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de charger la vue demandee.");
        }
    }

    private void loadConsultations() {
        consultationHistoryBox.getChildren().clear();

        try {
            List<Consultation> consultations = medicalDataService.getConsultationsByPatient(currentUser.getId());
            List<Consultation> filteredItems = new ArrayList<>();
            for (Consultation consultation : consultations) {
                if (matchesFilter(consultation)) {
                    filteredItems.add(consultation);
                }
            }

            historyCountLabel.setText(String.valueOf(filteredItems.size()));

            if (filteredItems.isEmpty()) {
                Label emptyLabel = new Label("Aucune consultation a afficher pour ce filtre.");
                emptyLabel.setWrapText(true);
                emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-font-size: 14px; -fx-padding: 18;");
                consultationHistoryBox.getChildren().add(emptyLabel);
                return;
            }

            for (Consultation consultation : filteredItems) {
                consultationHistoryBox.getChildren().add(buildConsultationCard(consultation));
            }
        } catch (SQLException e) {
            showError("Impossible de charger les consultations : " + e.getMessage());
        }
    }

    private boolean matchesFilter(Consultation consultation) {
        return switch (currentFilter) {
            case ALL -> true;
            case UPCOMING -> !consultation.getConsultationDate().isBefore(LocalDate.now());
            case PAST -> consultation.getConsultationDate().isBefore(LocalDate.now());
        };
    }

    private VBox buildConsultationCard(Consultation consultation) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #35a853; -fx-border-width: 0 0 0 4; -fx-border-radius: 12;");

        Label titleLabel = new Label("Consultation du " + consultation.getConsultationDate().format(DATE_FORMATTER));
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateBadge = new Label(consultation.getConsultationDate().format(DATE_FORMATTER));
        dateBadge.setStyle("-fx-background-color: #198754; -fx-background-radius: 8; "
                + "-fx-text-fill: white; -fx-padding: 6 12; -fx-font-weight: bold;");

        HBox titleRow = new HBox(10, titleLabel, spacer, dateBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label notesTitle = new Label("Compte rendu patient :");
        notesTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label notesLabel = buildWrappedBlock(consultation.getNotesPatient());

        Label psyTitle = new Label("Note psychologue :");
        psyTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label psyLabel = buildWrappedBlock(MedicalValidationService.normalize(consultation.getNotesPsychologue()).isBlank()
                ? "Aucune note psychologue pour le moment."
                : consultation.getNotesPsychologue());

        String createdAtText = consultation.getCreatedAt() == null ? "-" : consultation.getCreatedAt().format(DATE_TIME_FORMATTER);
        String updatedAtText = consultation.getUpdatedAt() == null ? "-" : consultation.getUpdatedAt().format(DATE_TIME_FORMATTER);
        Label createdAtLabel = new Label("Creee le " + createdAtText + " | Derniere modification le " + updatedAtText);
        createdAtLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Label rendezVousLabel = new Label(consultation.getRendezVousId() == null
                ? "Consultation non liee a un rendez-vous."
                : "Liee au rendez-vous #" + consultation.getRendezVousId());
        rendezVousLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-font-weight: bold;");

        Button editButton = new Button("Completer");
        editButton.setStyle("-fx-background-color: #0d6efd; -fx-text-fill: white; -fx-background-radius: 8;");
        editButton.setOnAction(event -> populateConsultationForm(consultation));

        Button deleteButton = new Button("Supprimer");
        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 8;");
        deleteButton.setOnAction(event -> handleDeleteConsultation(consultation));

        HBox actions = new HBox(10, editButton, deleteButton);

        card.getChildren().addAll(titleRow, rendezVousLabel, notesTitle, notesLabel, psyTitle, psyLabel, createdAtLabel, actions);
        return card;
    }

    private Label buildWrappedBlock(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #f5f8fc; -fx-background-radius: 10; -fx-padding: 14; "
                + "-fx-text-fill: #334155; -fx-font-size: 14px;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private void populateConsultationForm(Consultation consultation) {
        selectedConsultation = consultation;
        consultationDatePicker.setValue(consultation.getConsultationDate());
        consultationDatePicker.setDisable(true);
        consultationNotesArea.setText(consultation.getNotesPatient());
        psychologueNotesArea.setText(MedicalValidationService.normalize(consultation.getNotesPsychologue()));
        saveConsultationButton.setText("Mettre a jour la consultation");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
    }

    private void handleDeleteConsultation(Consultation consultation) {
        try {
            medicalDataService.deleteConsultation(consultation.getId());
            if (selectedConsultation != null && selectedConsultation.getId() == consultation.getId()) {
                resetForm();
            }
            loadConsultations();
            showInfo("Consultation supprimee avec succes.");
        } catch (SQLException e) {
            showError("Impossible de supprimer la consultation : " + e.getMessage());
        }
    }

    private void resetForm() {
        selectedConsultation = null;
        consultationDatePicker.setValue(LocalDate.now());
        consultationDatePicker.setDisable(true);
        consultationNotesArea.clear();
        psychologueNotesArea.clear();
        saveConsultationButton.setText("Mettre a jour la consultation");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }

    private enum FilterMode {
        ALL,
        UPCOMING,
        PAST
    }
}
