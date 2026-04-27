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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.example.entities.Journal;
import org.example.entities.User;
import org.example.service.ContentValidationService;
import org.example.service.JournalService;
import org.example.service.MoodPdfExportService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
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

    private final JournalService journalService = new JournalService();
    private final MoodPdfExportService moodPdfExportService = new MoodPdfExportService();
    private final ObservableList<Journal> allJournals = FXCollections.observableArrayList();
    private final ObservableList<Journal> visibleJournals = FXCollections.observableArrayList();
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

        journalTable.setItems(visibleJournals);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

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
    private void handleExportPdf() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }
        if (allJournals.isEmpty()) {
            showError("Aucun journal a exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le rapport PDF des humeurs");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        chooser.setInitialFileName("rapport_humeurs_" + currentUser.getPrenom() + "_" + currentUser.getNom() + ".pdf");

        File targetFile = chooser.showSaveDialog(journalTable.getScene().getWindow());
        if (targetFile == null) {
            return;
        }

        try {
            moodPdfExportService.exportMoodReport(targetFile.toPath(), currentUser, List.copyOf(allJournals));
            showInfo("Rapport PDF exporte avec succes.");
        } catch (IOException e) {
            showError("Export PDF impossible: " + e.getMessage());
        }
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
            allJournals.setAll(journalService.findByUser(currentUser, "", "recent"));
            applyFilters();
            updateStats(journalService.countByMood(currentUser));
            errorLabel.setText("");
        } catch (SQLException e) {
            showError("Chargement impossible: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        Comparator<Journal> comparator = Comparator.comparing(Journal::getDateCreation,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (!"Plus ancien".equals(sortCombo.getValue())) {
            comparator = comparator.reversed();
        }

        visibleJournals.setAll(
                allJournals.stream()
                        .filter(journal -> matchesKeyword(journal, keyword))
                        .sorted(comparator)
                        .toList()
        );
    }

    private boolean matchesKeyword(Journal journal, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(journal.getHumeur(), keyword)
                || containsIgnoreCase(journal.getContenu(), keyword)
                || containsIgnoreCase(journal.getEtatAnalyse(), keyword)
                || containsIgnoreCase(journal.getDateCreationFormatted(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void updateStats(Map<String, Integer> stats) {
        int heureux = stats.getOrDefault("heureux", 0);
        int calme = stats.getOrDefault("calme", 0);
        int sos = stats.getOrDefault("SOS", 0);
        int colere = stats.getOrDefault("en colere", 0);

        heureuxCountLabel.setText(String.valueOf(heureux));
        calmeCountLabel.setText(String.valueOf(calme));
        sosCountLabel.setText(String.valueOf(sos));
        colereCountLabel.setText(String.valueOf(colere));

        int total = allJournals.size();
        int analysed = countAnalysedJournals();
        int pending = Math.max(total - analysed, 0);

        if (totalJournauxStatLabel != null) {
            totalJournauxStatLabel.setText(String.valueOf(total));
        }
        if (totalAnalysesStatLabel != null) {
            totalAnalysesStatLabel.setText(String.valueOf(analysed));
        }
        if (totalPendingStatLabel != null) {
            totalPendingStatLabel.setText(String.valueOf(pending));
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

    private int countAnalysedJournals() {
        int analysed = 0;
        for (Journal journal : allJournals) {
            if (journal.getEtatAnalyse() != null && !journal.getEtatAnalyse().isBlank()) {
                analysed++;
            }
        }
        return analysed;
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
        return ContentValidationService.validateContent(contenu);
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
