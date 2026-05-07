package org.example.controller;

import entities.RendezVousPsy;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.example.entities.User;
import org.example.entities.ConsultationPayment;
import org.example.entities.ConsultationQuestionnaire;
import org.example.service.ConsultationWorkflowService;
import org.example.service.MedicalValidationService;
import org.example.utils.UserSession;
import services.ServiceRendezVous;

import java.io.IOException;
import java.sql.SQLException;

public class ListeRendezVousPsyController {
    @FXML private TableView<RendezVousPsy> tableRdv;
    @FXML private TableColumn<RendezVousPsy, String> colNom;
    @FXML private TableColumn<RendezVousPsy, String> colPrenom;
    @FXML private TableColumn<RendezVousPsy, String> colType;
    @FXML private TableColumn<RendezVousPsy, String> colDate;
    @FXML private TableColumn<RendezVousPsy, String> colHeure;
    @FXML private TableColumn<RendezVousPsy, String> colStatut;
    @FXML private Label errorGlobal;
    @FXML private Button btnAccepter;
    @FXML private Button btnRejeter;
    @FXML private Button btnEnregistrerNote;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private TextField searchRendezVousField;
    @FXML private TextArea notesPatientArea;
    @FXML private TextArea notesPsychologueArea;
    @FXML private Label paymentStatusLabel;
    @FXML private Label paymentAmountLabel;
    @FXML private Label questionnaireRiskLabel;
    @FXML private Label questionnaireSummaryLabel;

    private final ServiceRendezVous serviceRendezVous = new ServiceRendezVous();
    private final ConsultationWorkflowService consultationWorkflowService = new ConsultationWorkflowService();
    private final ObservableList<RendezVousPsy> rendezVousItems = FXCollections.observableArrayList();
    private final FilteredList<RendezVousPsy> filteredRendezVous = new FilteredList<>(rendezVousItems, item -> true);

    private RendezVousPsy selectedRdv;
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance();
        colNom.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNom()));
        colPrenom.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPrenom()));
        colType.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTypeRdv()));
        colDate.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getDate()));
        colHeure.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getHeureDebut() + " - " + d.getValue().getHeureFin()));
        colStatut.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatut()));
        configureFilters();
        refreshTable();

        tableRdv.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRdv = newValue;
            errorGlobal.setText("");
            populateNotes(newValue);
            populateWorkflowDetails(newValue);
            updateButtons();
        });
        updateButtons();
        populateWorkflowDetails(null);
    }

    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) tableRdv.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                tableRdv.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard Psy : " + e.getMessage());
        }
    }

    @FXML
    public void accepterRendezVous() {
        if (selectedRdv == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }
        if (!"en attente".equalsIgnoreCase(selectedRdv.getStatut())) {
            showMessage("Seuls les rendez-vous en attente peuvent etre traites", "red");
            return;
        }
        if (serviceRendezVous.validerRendezVous(selectedRdv.getId(), getCurrentPsychologueId())) {
            String successMessage = serviceRendezVous.getLastSuccessMessage();
            showMessage(successMessage == null || successMessage.isBlank() ? "Rendez-vous acceptee" : successMessage, "green");
            refreshTable();
        } else {
            showMessage(resolveBusinessError("Acceptation impossible"), "red");
        }
    }

    @FXML
    public void rejeterRendezVous() {
        if (selectedRdv == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }
        if (!"en attente".equalsIgnoreCase(selectedRdv.getStatut())) {
            showMessage("Seuls les rendez-vous en attente peuvent etre traites", "red");
            return;
        }
        if (serviceRendezVous.rejeterRendezVous(selectedRdv.getId(), getCurrentPsychologueId())) {
            showMessage("Rendez-vous rejetee", "green");
            refreshTable();
        } else {
            showMessage(resolveBusinessError("Rejet impossible"), "red");
        }
    }

    @FXML
    public void enregistrerNotePsychologue() {
        if (selectedRdv == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }
        if (!"acceptee".equalsIgnoreCase(selectedRdv.getStatut())) {
            showMessage("La note psychologue est reservee aux rendez-vous acceptes", "red");
            return;
        }
        if (serviceRendezVous.updatePsychologueNote(
                selectedRdv.getId(),
                getCurrentPsychologueId(),
                MedicalValidationService.normalize(notesPsychologueArea.getText()))) {
            showMessage("Note psychologue enregistree", "green");
            refreshTable();
        } else {
            showMessage(resolveBusinessError("Enregistrement impossible"), "red");
        }
    }

    private void refreshTable() {
        rendezVousItems.setAll(serviceRendezVous.getRendezVousForPsy(getCurrentPsychologueId()));
        applyFilters();
    }

    private void updateButtons() {
        boolean disabled = selectedRdv == null;
        if (btnAccepter != null) {
            btnAccepter.setDisable(disabled);
        }
        if (btnRejeter != null) {
            btnRejeter.setDisable(disabled);
        }
        if (btnEnregistrerNote != null) {
            btnEnregistrerNote.setDisable(selectedRdv == null || !"acceptee".equalsIgnoreCase(selectedRdv.getStatut()));
        }
    }

    private void showMessage(String message, String color) {
        errorGlobal.setText(message);
        errorGlobal.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private int getCurrentPsychologueId() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        return currentUser.getId();
    }

    private void populateNotes(RendezVousPsy rendezVous) {
        if (notesPatientArea == null || notesPsychologueArea == null) {
            return;
        }
        if (rendezVous == null) {
            notesPatientArea.clear();
            notesPsychologueArea.clear();
            return;
        }
        notesPatientArea.setText(MedicalValidationService.normalize(rendezVous.getNotesPatient()));
        notesPsychologueArea.setText(MedicalValidationService.normalize(rendezVous.getNotesPsychologue()));
    }

    private void populateWorkflowDetails(RendezVousPsy rendezVous) {
        if (paymentStatusLabel == null || paymentAmountLabel == null || questionnaireRiskLabel == null || questionnaireSummaryLabel == null) {
            return;
        }
        if (rendezVous == null) {
            paymentStatusLabel.setText("Paiement: aucun rendez-vous selectionne");
            paymentAmountLabel.setText("Montant: -");
            questionnaireRiskLabel.setText("Score clinique: -");
            questionnaireSummaryLabel.setText("Le formulaire patient apparaîtra ici apres paiement.");
            return;
        }
        try {
            ConsultationPayment payment = consultationWorkflowService.getPaymentByRendezVousId(rendezVous.getId());
            if (payment == null) {
                paymentStatusLabel.setText("Paiement: non initialise");
                paymentAmountLabel.setText("Montant: -");
                questionnaireRiskLabel.setText("Score clinique: -");
                questionnaireSummaryLabel.setText("Le patient n'a pas encore de paiement cree pour cette consultation.");
                return;
            }
            paymentStatusLabel.setText("Paiement: " + payment.getStatus());
            paymentAmountLabel.setText("Montant: " + payment.getFormattedAmount());
            ConsultationQuestionnaire questionnaire = payment.getQuestionnaire();
            if (questionnaire == null) {
                questionnaireRiskLabel.setText("Score clinique: -");
                questionnaireSummaryLabel.setText(payment.isPaid()
                        ? "Paiement recu. Le patient n'a pas encore rempli le formulaire clinique."
                        : "En attente de paiement. Le formulaire sera debloque apres paiement.");
                return;
            }
            questionnaireRiskLabel.setText("Score clinique: " + questionnaire.getRiskScore());
            questionnaireSummaryLabel.setText(questionnaire.getPredictedState()
                    + "\nMotif: " + questionnaire.getChiefComplaint()
                    + "\nSymptomes: " + questionnaire.getSymptomSummary());
        } catch (SQLException e) {
            paymentStatusLabel.setText("Paiement: erreur");
            paymentAmountLabel.setText("Montant: -");
            questionnaireRiskLabel.setText("Score clinique: -");
            questionnaireSummaryLabel.setText("Impossible de charger les details du paiement: " + e.getMessage());
        }
    }

    private void configureFilters() {
        tableRdv.setItems(filteredRendezVous);
        if (filterStatutCombo != null) {
            filterStatutCombo.setItems(FXCollections.observableArrayList("Tous", "en attente", "acceptee"));
            filterStatutCombo.getSelectionModel().selectFirst();
            filterStatutCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (searchRendezVousField != null) {
            searchRendezVousField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
    }

    private void applyFilters() {
        String search = searchRendezVousField == null ? "" : searchRendezVousField.getText();
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        String statut = filterStatutCombo == null || filterStatutCombo.getValue() == null
                ? "Tous"
                : filterStatutCombo.getValue().trim().toLowerCase();

        filteredRendezVous.setPredicate(rendezVous -> {
            boolean matchesStatut = "tous".equals(statut)
                    || MedicalValidationService.normalize(rendezVous.getStatut()).equalsIgnoreCase(statut);
            if (!matchesStatut) {
                return false;
            }
            if (normalizedSearch.isBlank()) {
                return true;
            }
            return contains(rendezVous.getNom(), normalizedSearch)
                    || contains(rendezVous.getPrenom(), normalizedSearch)
                    || contains(rendezVous.getTypeRdv(), normalizedSearch)
                    || contains(rendezVous.getDate(), normalizedSearch)
                    || contains(rendezVous.getHeureDebut(), normalizedSearch)
                    || contains(rendezVous.getHeureFin(), normalizedSearch)
                    || contains(rendezVous.getStatut(), normalizedSearch);
        });
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String resolveBusinessError(String defaultMessage) {
        String serviceMessage = serviceRendezVous.getLastValidationError();
        return serviceMessage == null || serviceMessage.isBlank() ? defaultMessage : serviceMessage;
    }
}
