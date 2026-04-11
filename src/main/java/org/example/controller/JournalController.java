package org.example.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.entities.Journal;
import org.example.entities.User;
import org.example.service.JournalService;
import org.example.utils.JournalValidator;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class JournalController {
    @FXML private Label welcomeLabel;
    @FXML private Label errorLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> humeurCombo;
    @FXML private TextArea contenuArea;
    @FXML private TableView<Journal> journalTable;
    @FXML private TableColumn<Journal, String> humeurColumn;
    @FXML private TableColumn<Journal, String> contenuColumn;
    @FXML private TableColumn<Journal, String> dateColumn;
    @FXML private TableColumn<Journal, String> analyseColumn;
    @FXML private Label heureuxCountLabel;
    @FXML private Label calmeCountLabel;
    @FXML private Label sosCountLabel;
    @FXML private Label colereCountLabel;

    private final JournalService journalService = new JournalService();
    private User currentUser;
    private Journal selectedJournal;

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("Plus recent", "Plus ancien"));
        sortCombo.setValue("Plus recent");

        humeurCombo.setItems(FXCollections.observableArrayList("heureux", "calme", "SOS", "en colere"));

        humeurColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(capitalizeMood(cellData.getValue().getHumeur())));
        contenuColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getContenu()));
        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDateCreationFormatted()));
        analyseColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatAnalyse(cellData.getValue().getEtatAnalyse())));

        journalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedJournal = newValue;
            errorLabel.setText("");
        });

        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Mes journaux - " + user.getPrenom());
        }
        refreshData();
    }

    @FXML
    private void handleCreate() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        String validationError = validateForm();
        if (validationError != null) {
            showError(validationError);
            return;
        }

        String contenu = contenuArea.getText().trim();
        String humeur = humeurCombo.getValue();

        try {
            Journal journal = journalService.buildNewJournal(contenu, humeur, currentUser);
            journalService.create(journal);
            showInfo("Journal ajoute avec succes.");
            clearForm();
            refreshData();
        } catch (SQLException e) {
            showError("Erreur SQL: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        if (currentUser == null || selectedJournal == null) {
            showError("Selectionnez un journal a modifier.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/journal_edit.fxml"));
            Parent root = loader.load();

            JournalEditController controller = loader.getController();
            controller.setData(currentUser, selectedJournal);

            journalTable.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Ouverture de la page de modification impossible.");
        }
    }

    @FXML
    private void handleDelete() {
        Journal journalToDelete = journalTable.getSelectionModel().getSelectedItem();
        if (currentUser == null || journalToDelete == null) {
            showError("Selectionnez un journal a supprimer.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer ce journal ?",
                ButtonType.YES,
                ButtonType.NO
        );

        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            journalService.delete(journalToDelete.getId(), currentUser.getId());
            clearForm();
            refreshData();
            showInfo("Journal supprime avec succes.");
        } catch (SQLException e) {
            showError("Suppression impossible: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        refreshData();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        sortCombo.setValue("Plus recent");
        clearForm();
        refreshData();
    }

    @FXML
    private void goToDashboard() {
        loadView("/patient_dashboard.fxml", true);
    }

    @FXML
    private void goToProfil() {
        loadView("/profil_patient.fxml", true);
    }

    @FXML
    private void handleLogout() {
        UserSession.setInstance(null);
        loadView("/login.fxml", false);
    }

    private void refreshData() {
        if (currentUser == null || journalTable == null) {
            return;
        }

        try {
            String sortOrder = "Plus ancien".equals(sortCombo.getValue()) ? "old" : "recent";
            journalTable.setItems(FXCollections.observableArrayList(
                    journalService.findByUser(currentUser, searchField.getText(), sortOrder)
            ));
            updateStats(journalService.countByMood(currentUser));
            errorLabel.setText("");
        } catch (SQLException e) {
            showError("Chargement impossible: " + e.getMessage());
        }
    }

    private void updateStats(Map<String, Integer> stats) {
        heureuxCountLabel.setText(String.valueOf(stats.getOrDefault("heureux", 0)));
        calmeCountLabel.setText(String.valueOf(stats.getOrDefault("calme", 0)));
        sosCountLabel.setText(String.valueOf(stats.getOrDefault("SOS", 0)));
        colereCountLabel.setText(String.valueOf(stats.getOrDefault("en colere", 0)));
    }

    private void clearForm() {
        selectedJournal = null;
        journalTable.getSelectionModel().clearSelection();
        contenuArea.clear();
        humeurCombo.getSelectionModel().clearSelection();
        errorLabel.setText("");
    }

    private String validateForm() {
        String contenu = contenuArea.getText() == null ? "" : contenuArea.getText().trim();
        String humeur = humeurCombo.getValue();

        if (humeur == null || humeur.isBlank()) {
            return "Veuillez choisir une humeur.";
        }
        return JournalValidator.validateContent(contenu);
    }

    private void loadView(String fxmlPath, boolean injectUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (injectUser) {
                Object controller = loader.getController();
                if (controller instanceof PatientDashboardController patientController) {
                    patientController.setUserData(currentUser);
                } else if (controller instanceof ProfilPatientController profilPatientController) {
                    profilPatientController.setUserData(currentUser);
                } else if (controller instanceof JournalController journalController) {
                    journalController.setUserData(currentUser);
                }
            }

            journalTable.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Navigation impossible: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    private String capitalizeMood(String mood) {
        if (mood == null || mood.isBlank()) {
            return "";
        }
        return switch (mood) {
            case "SOS" -> "SOS";
            case "en colere" -> "En colere";
            default -> mood.substring(0, 1).toUpperCase() + mood.substring(1);
        };
    }

    private String formatAnalyse(String analyse) {
        return analyse == null || analyse.isBlank() ? "Aucune" : analyse;
    }
}
