package org.example.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
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
    @FXML private TextField searchField;
    @FXML private TableView<JournalAnalyseRow> analyseTable;
    @FXML private TableColumn<JournalAnalyseRow, String> dateColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> humeurColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> contenuColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> etatColumn;
    @FXML private TableColumn<JournalAnalyseRow, String> statutColumn;
    @FXML private ComboBox<User> patientCombo;
    @FXML private HBox patientSelectorBox;
    @FXML private Button navJournauxBtn;
    @FXML private Label totalJournauxStatLabel;
    @FXML private Label totalAnalysesStatLabel;
    @FXML private Label totalPendingStatLabel;
    @FXML private Label heureuxPercentLabel;
    @FXML private Label calmePercentLabel;
    @FXML private Label sosPercentLabel;
    @FXML private Label colerePercentLabel;
    @FXML private ProgressBar heureuxProgress;
    @FXML private ProgressBar calmeProgress;
    @FXML private ProgressBar sosProgress;
    @FXML private ProgressBar colereProgress;

    private final AnalyseEmotionnelleService analyseService = new AnalyseEmotionnelleService();
    private final ObservableList<JournalAnalyseRow> allRows = FXCollections.observableArrayList();
    private final ObservableList<JournalAnalyseRow> visibleRows = FXCollections.observableArrayList();
    private User currentUser;
    private User viewerUser;
    private JournalAnalyseRow selectedRow;

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDateJournal()));
        humeurColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getHumeur()));
        contenuColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getContenuResume()));
        etatColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEtatEmotionnel()));
        statutColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatut()));

        analyseTable.setItems(visibleRows);
        analyseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRow = newValue;
            errorLabel.setText("");
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
    }

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
            public String toString(User user) {
                if (user == null) {
                    return "";
                }
                String prenom = user.getPrenom() != null ? user.getPrenom() : "";
                String nom = user.getNom() != null ? user.getNom() : "";
                return (prenom + " " + nom).trim();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        var patients = UserService.getByRole("ROLE_PATIENT");
        patientCombo.setItems(patients);
        patientCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                setUserData(newValue);
            }
        });

        if (patients.isEmpty()) {
            showError("Aucun patient enregistre. Les analyses portent sur les journaux d'un patient.");
            return;
        }

        patientCombo.getSelectionModel().select(0);
    }

    public void focusPatient(User patient) {
        if (patient == null) {
            return;
        }
        if (patientCombo != null && patientCombo.getItems() != null) {
            for (User user : patientCombo.getItems()) {
                if (user.getId() == patient.getId()) {
                    patientCombo.getSelectionModel().select(user);
                    setUserData(user);
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
            allRows.setAll(analyseService.findRowsByUser(currentUser));
            applyFilters();

            int today = analyseService.countTodayJournals(currentUser);
            int analysed = analyseService.countAnalysed(currentUser);
            todayCountLabel.setText(String.valueOf(today));
            analysedCountLabel.setText(String.valueOf(analysed));
            pendingCountLabel.setText(String.valueOf(Math.max(allRows.size() - analysed, 0)));
            updateProfessionalStats(analysed);
            errorLabel.setText("");
        } catch (SQLException e) {
            showError("Chargement impossible: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        visibleRows.setAll(
                allRows.stream()
                        .filter(row -> matchesKeyword(row, keyword))
                        .toList()
        );
    }

    private boolean matchesKeyword(JournalAnalyseRow row, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(row.getDateJournal(), keyword)
                || containsIgnoreCase(row.getHumeur(), keyword)
                || containsIgnoreCase(row.getContenuResume(), keyword)
                || containsIgnoreCase(row.getEtatEmotionnel(), keyword)
                || containsIgnoreCase(row.getNiveau(), keyword)
                || containsIgnoreCase(row.getStatut(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void clearSelection() {
        selectedRow = null;
        if (analyseTable != null) {
            analyseTable.getSelectionModel().clearSelection();
        }
        errorLabel.setText("");
    }

    private void updateProfessionalStats(int analysedCount) {
        int total = allRows.size();
        int pending = Math.max(total - analysedCount, 0);

        if (totalJournauxStatLabel != null) {
            totalJournauxStatLabel.setText(String.valueOf(total));
        }
        if (totalAnalysesStatLabel != null) {
            totalAnalysesStatLabel.setText(String.valueOf(analysedCount));
        }
        if (totalPendingStatLabel != null) {
            totalPendingStatLabel.setText(String.valueOf(pending));
        }

        int heureux = 0;
        int calme = 0;
        int sos = 0;
        int colere = 0;
        for (JournalAnalyseRow row : allRows) {
            String mood = row.getHumeur() == null ? "" : row.getHumeur().trim().toLowerCase();
            switch (mood) {
                case "heureux" -> heureux++;
                case "calme" -> calme++;
                case "sos" -> sos++;
                case "en colere" -> colere++;
                default -> {
                }
            }
        }

        animateProgress(heureuxProgress, toPercent(heureux, total));
        animateProgress(calmeProgress, toPercent(calme, total));
        animateProgress(sosProgress, toPercent(sos, total));
        animateProgress(colereProgress, toPercent(colere, total));

        setPercentLabel(heureuxPercentLabel, heureux, total);
        setPercentLabel(calmePercentLabel, calme, total);
        setPercentLabel(sosPercentLabel, sos, total);
        setPercentLabel(colerePercentLabel, colere, total);
    }

    private double toPercent(int value, int total) {
        return total <= 0 ? 0.0 : (double) value / total;
    }

    private void animateProgress(ProgressBar bar, double targetProgress) {
        if (bar == null) {
            return;
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), bar.getProgress())),
                new KeyFrame(Duration.millis(700), new KeyValue(bar.progressProperty(), targetProgress))
        );
        timeline.play();
    }

    private void setPercentLabel(Label label, int value, int total) {
        if (label == null) {
            return;
        }
        if (total <= 0) {
            label.setText("0% (0)");
            return;
        }
        int percent = (int) Math.round((value * 100.0) / total);
        label.setText(percent + "% (" + value + ")");
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
