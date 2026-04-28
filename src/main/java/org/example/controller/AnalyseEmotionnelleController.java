package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;
import org.example.entities.AnalyseEmotionnelle;
import org.example.service.AnalyseEmotionnelleService;
import org.example.service.ClinicalLlmService;
import org.example.service.UserService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

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
    @FXML private HBox psyAlertsToolbar;
    @FXML private Button psyAlertsToggleBtn;
    @FXML private Label psyAlertsBadgeLabel;
    @FXML private VBox psyAlertBox;
    @FXML private Label psyAlertSummaryLabel;
    @FXML private VBox psyAlertListBox;
    @FXML private Button aiAnalyseBtn;
    @FXML private Button aiConseilBtn;
    @FXML private ProgressIndicator aiProgress;
    @FXML private TextArea aiAnalyseArea;
    @FXML private TextArea aiConseilArea;

    private final AnalyseEmotionnelleService analyseService = new AnalyseEmotionnelleService();
    private final ClinicalLlmService clinicalLlmService = new ClinicalLlmService();
    private final ObservableList<JournalAnalyseRow> allRows = FXCollections.observableArrayList();
    private final ObservableList<JournalAnalyseRow> visibleRows = FXCollections.observableArrayList();
    private User currentUser;
    private User viewerUser;
    private JournalAnalyseRow selectedRow;
    private boolean psyAlertsPanelExpanded;

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
        if (psyAlertsToolbar != null) {
            psyAlertsToolbar.setVisible(true);
            psyAlertsToolbar.setManaged(true);
        }
        collapsePsyAlertsPanel();
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
            updatePsychologistAlerts();
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
                || containsIgnoreCase(row.getStatut(), keyword)
                || containsIgnoreCase(row.getRisqueLabel(), keyword)
                || containsIgnoreCase(row.getRisqueDetails(), keyword);
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

    private void updatePsychologistAlerts() {
        if (psyAlertBox == null || psyAlertsToolbar == null || !psyAlertsToolbar.isManaged()) {
            return;
        }

        List<JournalAnalyseRow> alerts = allRows.stream()
                .filter(row -> row.getRisqueScore() > 0)
                .sorted(Comparator.comparingInt(JournalAnalyseRow::getRisqueScore).reversed())
                .toList();

        if (psyAlertsBadgeLabel != null) {
            if (alerts.isEmpty()) {
                psyAlertsBadgeLabel.setText("Aucun signal de risque pour ce patient.");
            } else {
                long criticalCount = alerts.stream().filter(row -> row.getRisqueScore() >= 10).count();
                psyAlertsBadgeLabel.setText(alerts.size() + " signalement(s), dont " + criticalCount + " critique(s).");
            }
        }

        psyAlertListBox.getChildren().clear();
        if (alerts.isEmpty()) {
            psyAlertSummaryLabel.setText("Aucune alerte critique detectee pour ce patient.");
            return;
        }

        long criticalCount = alerts.stream().filter(row -> row.getRisqueScore() >= 10).count();
        psyAlertSummaryLabel.setText(
                alerts.size() + " journal(x) signale(s), dont " + criticalCount + " critique(s)."
        );

        alerts.stream()
                .limit(5)
                .forEach(row -> psyAlertListBox.getChildren().add(buildAlertCard(row)));
    }

    @FXML
    private void togglePsyAlertsPanel() {
        setPsyAlertsPanelExpanded(!psyAlertsPanelExpanded);
    }

    private void collapsePsyAlertsPanel() {
        psyAlertsPanelExpanded = false;
        if (psyAlertBox != null) {
            psyAlertBox.setVisible(false);
            psyAlertBox.setManaged(false);
        }
        syncPsyAlertsToggleLabel();
    }

    private void setPsyAlertsPanelExpanded(boolean expanded) {
        psyAlertsPanelExpanded = expanded;
        if (psyAlertBox != null) {
            psyAlertBox.setVisible(expanded);
            psyAlertBox.setManaged(expanded);
        }
        syncPsyAlertsToggleLabel();
    }

    private void syncPsyAlertsToggleLabel() {
        if (psyAlertsToggleBtn == null) {
            return;
        }
        psyAlertsToggleBtn.setText(psyAlertsPanelExpanded ? "Masquer les alertes patient" : "Afficher les alertes patient");
    }

    private VBox buildAlertCard(JournalAnalyseRow row) {
        VBox card = new VBox(4);
        String borderColor = row.getRisqueScore() >= 10 ? "#d84343" : "#f39c45";
        String backgroundColor = row.getRisqueScore() >= 10 ? "#fff0f0" : "#fff7ec";
        card.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 12; -fx-padding: 12; -fx-border-color: " + borderColor + "; -fx-border-radius: 12;");

        Label title = new Label(row.getDateJournal() + " | " + safe(row.getRisqueLabel()) + " | " + safe(row.getHumeur()));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #7a2e2e;");

        Label content = new Label(safe(row.getContenuResume()));
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #4f5050;");

        Label details = new Label(safe(row.getRisqueDetails()));
        details.setWrapText(true);
        details.setStyle("-fx-text-fill: #7a5b5b;");

        card.getChildren().addAll(title, content, details);
        return card;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    @FXML
    private void handleAiAnalyse() {
        if (!ensureJournalSelectedForAi()) {
            return;
        }
        String prompt = buildClinicalAnalysisPrompt(selectedRow);
        runGeminiTask("Synthese clinique", prompt, aiAnalyseArea);
    }

    @FXML
    private void handleAiConseil() {
        if (!ensureJournalSelectedForAi()) {
            return;
        }
        String prompt = buildCompassionConseilPrompt(selectedRow);
        runGeminiTask("Conseils personnalises", prompt, aiConseilArea);
    }

    private boolean ensureJournalSelectedForAi() {
        if (currentUser == null) {
            showError("Aucun patient charge.");
            return false;
        }
        if (selectedRow == null) {
            showError("Selectionnez une ligne du tableau (un journal).");
            return false;
        }
        String text = selectedRow.getContenuComplet();
        if (text == null || text.isBlank()) {
            showError("Ce journal ne contient pas de texte exploitable.");
            return false;
        }
        return true;
    }

    private String buildClinicalAnalysisPrompt(JournalAnalyseRow row) {
        String patient = patientLabel();
        String contenu = row.getContenuComplet() == null ? "" : row.getContenuComplet().trim();
        String etat = row.getEtatEmotionnel();
        String niveau = row.getNiveau();
        String risque = row.getRisqueLabel() + (row.getRisqueDetails() != null ? " — " + row.getRisqueDetails() : "");
        AnalyseEmotionnelle a = row.getAnalyseEmotionnelle();
        String existant = a == null
                ? "Aucune analyse structuree en base pour cette entree."
                : ("Etat emotionnel note : " + safe(a.getEtatEmotionnel()) + ", niveau : " + safe(a.getNiveau())
                + ", declencheur : " + safe(a.getDeclencheur()) + ", conseil existant : " + safe(a.getConseil()));

        return """
                Tu es un psychologue superviseur clinique qui aide un collegue a preparer son prochain echange.
                Tu reponds UNIQUEMENT en francais. Pas de diagnostic DSM ni de prescription medicamenteuse.

                Patient : %s
                Date du journal : %s
                Humeur declaree dans l'app : %s
                Evaluation automatique des risques (outil interne, indicatif) : %s

                Texte brut du journal :
                ---
                %s
                ---

                Analyse deja saisie en base (peut etre incomplete) :
                %s

                Produit une SYNTHESE STRUCTUREE avec exactement ces parties (titres en ligne, puis contenu) :
                1) COHERENCE — alignement entre l'humeur choisie et le ton du texte
                2) NUANCES — emotions implicites, sous-texte, besoins non dits
                3) VIGILANCE — signaux a surveiller sans dramatiser (rappel : tu n'es pas medecin)
                4) PISTES POUR LE THERAPEUTE — 3 a 5 observations utiles pour la seance

                Style : professionnel, chaleureux, concis (environ 350 a 600 mots).
                """.formatted(patient, safe(row.getDateJournal()), safe(row.getHumeur()), safe(risque), contenu, existant);
    }

    private String buildCompassionConseilPrompt(JournalAnalyseRow row) {
        String patient = patientLabel();
        String contenu = row.getContenuComplet() == null ? "" : row.getContenuComplet().trim();
        return """
                Tu es un psychologue bienveillant dans l'application EmoNado (accompagnement emotionnel, pas de remplacement d'un suivi medical).
                Tu ecris en francais, ton doux et concret. Pas de medicaments, pas de diagnostic medical.

                Destinataire implicite du message : %s (tu peux utiliser le prenom si tu le deduis du contexte, sans inventer d'informations personnelles).

                Contexte : journal du %s, humeur declaree : %s.

                Texte du journal :
                ---
                %s
                ---

                Propose une reponse en 4 a 6 courts paragraphes :
                - une validation empathique du vecu ;
                - deux micro-exercices concrets (ancrage, respiration courte, ou restructuration cognitive tres legere) ;
                - une phrase d'encouragement pour les prochains jours ;
                - si le texte evoque une detresse grave ou des idees de mort, termine par un rappel clair d'aller vers les urgences (15 / 112) ou un proche de confiance.

                Evite le jargon. Pas de liste a puces trop technique.
                """.formatted(patient, safe(row.getDateJournal()), safe(row.getHumeur()), contenu);
    }

    private String patientLabel() {
        if (currentUser == null) {
            return "Patient";
        }
        String p = currentUser.getPrenom() != null ? currentUser.getPrenom() : "";
        String n = currentUser.getNom() != null ? currentUser.getNom() : "";
        String label = (p + " " + n).trim();
        return label.isEmpty() ? "Patient" : label;
    }

    private void runGeminiTask(String phaseLabel, String prompt, TextArea targetArea) {
        if (aiProgress != null) {
            aiProgress.setVisible(true);
            aiProgress.setManaged(true);
        }
        setAiButtonsDisabled(true);
        errorLabel.setText(phaseLabel + " — connexion a Gemini...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return clinicalLlmService.completeClinical(prompt);
            }
        };
        task.setOnSucceeded(e -> {
            targetArea.setText(task.getValue());
            errorLabel.setText("");
            fadeInArea(targetArea);
            finishAiTaskUi();
        });
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t != null && t.getMessage() != null && !t.getMessage().isBlank()
                    ? t.getMessage()
                    : "Erreur lors de l'appel a l'IA.";
            showError(msg);
            finishAiTaskUi();
        });
        new Thread(task, "emonado-gemini-journal").start();
    }

    private void finishAiTaskUi() {
        if (aiProgress != null) {
            aiProgress.setVisible(false);
            aiProgress.setManaged(false);
        }
        setAiButtonsDisabled(false);
    }

    private void setAiButtonsDisabled(boolean disabled) {
        if (aiAnalyseBtn != null) {
            aiAnalyseBtn.setDisable(disabled);
        }
        if (aiConseilBtn != null) {
            aiConseilBtn.setDisable(disabled);
        }
    }

    private void fadeInArea(TextArea area) {
        if (area == null) {
            return;
        }
        area.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(520), area);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}
