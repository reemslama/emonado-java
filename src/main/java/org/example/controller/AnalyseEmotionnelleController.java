package org.example.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;
import org.example.service.AnalyseEmotionnelleService;
import org.example.service.UserService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;

public class AnalyseEmotionnelleController {
    @FXML private Label welcomeLabel;
    @FXML private Label todayCountLabel;
    @FXML private Label analysedCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label errorLabel;
    @FXML private TableView<JournalAnalyseRow> analyseTable;
    @FXML private TableColumn<JournalAnalyseRow, String> dateColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> humeurColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> contenuColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> etatColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> statutColumn;
    @FXML private ComboBox<User> patientCombo;
    @FXML private HBox patientSelectorBox;
    @FXML private Button navJournauxBtn;

    private final AnalyseEmotionnelleService analyseService = new AnalyseEmotionnelleService();
    /** Patient dont les journaux sont analyses (requetes SQL inchangées). */
    private User currentUser;
    /** Compte connecte (psychologue) pour la navigation Accueil / profil. */
    private User viewerUser;
    private JournalAnalyseRow selectedRow;

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDateJournal()));
        humeurColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getHumeur()));
        contenuColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getContenuResume()));
        etatColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEtatEmotionnel()));
        statutColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatut()));

        analyseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRow = newValue;
            errorLabel.setText("");
        });
    }

    /**
     * A appeler depuis le tableau de bord psychologue : charge la liste des patients
     * et affiche les journaux du patient selectionne (meme logique metier qu'avant).
     */
    public void initForPsychologueView() {
        viewerUser = UserSession.getInstance();
        if (patientSelectorBox != null) {
            patientSelectorBox.setVisible(true);
            patientSelectorBox.setManaged(true);
        }
        if (navJournauxBtn != null) {
            navJournauxBtn.setVisible(false);
            navJournauxBtn.setManaged(false);
        }
        if (patientCombo == null) {
            return;
        }
        patientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                if (u == null) {
                    return "";
                }
                String p = u.getPrenom() != null ? u.getPrenom() : "";
                String n = u.getNom() != null ? u.getNom() : "";
                return (p + " " + n).trim();
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });
        var patients = UserService.getByRole("ROLE_PATIENT");
        patientCombo.setItems(patients);
        patientCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                setUserData(newVal);
            }
        });
        if (patients.isEmpty()) {
            showError("Aucun patient enregistre. Les analyses portent sur les journaux d'un patient.");
            return;
        }
        patientCombo.getSelectionModel().select(0);
    }

    /** Apres retour depuis l'edition : reselectionner le meme patient dans la liste. */
    public void focusPatient(User patient) {
        if (patient == null) {
            return;
        }
        if (patientCombo != null && patientCombo.getItems() != null) {
            for (User u : patientCombo.getItems()) {
                if (u.getId() == patient.getId()) {
                    patientCombo.getSelectionModel().select(u);
                    setUserData(u);
                    return;
                }
            }
        }
        setUserData(patient);
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Analyse emotionnelle - " + user.getPrenom());
        }
        refreshData();
    }

    @FXML
    private void handleAdd() {
        if (currentUser == null || selectedRow == null) {
            showError("Selectionnez un journal pour ajouter son analyse.");
            return;
        }
        openEditPage(selectedRow);
    }

    @FXML
    private void handleEdit() {
        if (currentUser == null || selectedRow == null) {
            showError("Selectionnez un journal a modifier.");
            return;
        }
        openEditPage(selectedRow);
    }

    @FXML
    private void handleDelete() {
        if (selectedRow == null || selectedRow.getAnalyseEmotionnelle() == null) {
            showError("Selectionnez une analyse a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette analyse emotionnelle ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            analyseService.delete(selectedRow.getAnalyseEmotionnelle().getId(), selectedRow.getJournalId());
            clearSelection();
            refreshData();
            showInfo("Analyse emotionnelle supprimee.");
        } catch (SQLException e) {
            showError("Suppression impossible: " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        clearSelection();
    }

    @FXML
    private void goToDashboard() {
        if (viewerUser != null && "ROLE_PSYCHOLOGUE".equalsIgnoreCase(viewerUser.getRole())) {
            loadView("/psy_dashboard.fxml");
        } else {
            loadView("/patient_dashboard.fxml");
        }
    }

    @FXML
    private void goToProfil() {
        if (viewerUser != null && "ROLE_PSYCHOLOGUE".equalsIgnoreCase(viewerUser.getRole())) {
            loadView("/profil_psy.fxml");
        } else {
            loadView("/profil_patient.fxml");
        }
    }

    @FXML
    private void goToJournaux() {
        loadView("/journal.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.setInstance(null);
        loadView("/login.fxml");
    }

    private void refreshData() {
        if (currentUser == null) {
            return;
        }
        try {
            var rows = FXCollections.observableArrayList(analyseService.findRowsByUser(currentUser));
            analyseTable.setItems(rows);
            int today = analyseService.countTodayJournals(currentUser);
            int analysed = analyseService.countAnalysed(currentUser);
            todayCountLabel.setText(String.valueOf(today));
            analysedCountLabel.setText(String.valueOf(analysed));
            pendingCountLabel.setText(String.valueOf(Math.max(rows.size() - analysed, 0)));
            errorLabel.setText("");
        } catch (SQLException e) {
            showError("Chargement impossible: " + e.getMessage());
        }
    }

    private void clearSelection() {
        selectedRow = null;
        if (analyseTable != null) {
            analyseTable.getSelectionModel().clearSelection();
        }
        errorLabel.setText("");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof PatientDashboardController patientController) {
                patientController.setUserData(currentUser);
            } else if (controller instanceof ProfilPatientController profilController) {
                profilController.setUserData(currentUser);
            } else if (controller instanceof JournalController journalController) {
                journalController.setUserData(currentUser);
            } else if (controller instanceof PsyDashboardController psyController) {
                psyController.setUserData(viewerUser);
            } else if (controller instanceof ProfilPsyController profilPsyController) {
                profilPsyController.setUserData(viewerUser);
            }

            analyseTable.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Navigation impossible.");
        }
    }

    private void openEditPage(JournalAnalyseRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/analyse_emotionnelle_edit.fxml"));
            Parent root = loader.load();
            AnalyseEmotionnelleEditController controller = loader.getController();
            controller.setData(viewerUser, currentUser, row);
            analyseTable.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Ouverture de la page de modification impossible.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }
}
