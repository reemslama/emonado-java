package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
<<<<<<< Updated upstream
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.ProfilPsychologique;
import org.example.entities.RapportSessionJeu;
import org.example.entities.User;
import org.example.service.SessionJeuService;
import org.example.utils.UserSession;

=======
>>>>>>> Stashed changes
import java.io.IOException;
import java.util.Objects;

public class EspaceEnfantController {
<<<<<<< Updated upstream
    @FXML private BorderPane mainContainer;
    @FXML private VBox childHomePane;
    @FXML private BorderPane gamePane;
    @FXML private StackPane gameContentHost;
    @FXML private StackPane parentOverlay;
    @FXML private Label parentNameLabel;
    @FXML private Label profileLabel;
    @FXML private Label trendLabel;
    @FXML private Label anomalyLabel;
    @FXML private Label scoreLabel;
    @FXML private Label summaryLabel;
    @FXML private Label emailStatusLabel;
    @FXML private Label alertLabel;
    @FXML private TextArea reportArea;
    @FXML private ProgressBar sociabilityBar;
    @FXML private ProgressBar stressBar;
    @FXML private ProgressBar timidityBar;
    @FXML private ProgressBar curiosityBar;
    @FXML private StackPane heatmapContainer;

    private final SessionJeuService sessionJeuService = new SessionJeuService();
    private Parent gameView;
    private Parent heatmapView;
    private RapportSessionJeu currentReport;

    @FXML
    public void initialize() {
        showHome();
    }

    @FXML
    private void startGame() {
        childHomePane.setVisible(false);
        childHomePane.setManaged(false);
        gamePane.setVisible(true);
        gamePane.setManaged(true);
        loadGameView();
    }

    @FXML
    private void showHome() {
        parentOverlay.setVisible(false);
        parentOverlay.setManaged(false);
        childHomePane.setVisible(true);
        childHomePane.setManaged(true);
        gamePane.setVisible(false);
        gamePane.setManaged(false);
    }

    @FXML
    private void showParentDashboard() {
        refreshParentDashboard();
        parentOverlay.setVisible(true);
        parentOverlay.setManaged(true);
    }

    @FXML
    private void hideParentDashboard() {
        parentOverlay.setVisible(false);
        parentOverlay.setManaged(false);
    }

    @FXML
    private void sendReportByEmail() {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) {
            emailStatusLabel.setText("Aucun parent connecte.");
            return;
        }
        RapportSessionJeu rapport = getOrBuildReport(currentUser);
        sessionJeuService.envoyerRapportParent(currentUser, rapport);
        emailStatusLabel.setText("Rapport envoye a " + currentUser.getEmail());
    }

    @FXML
    private void goToDeconnexion() {
        try {
            UserSession.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Erreur lors de la deconnexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadGameView() {
        try {
            if (gameView == null) {
                gameView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/GestionParticipations.fxml")));
            }
            gameContentHost.getChildren().setAll(gameView);
        } catch (IOException e) {
            System.err.println("Erreur chargement jeu enfant: " + e.getMessage());
        }
    }

    private void refreshParentDashboard() {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) {
            parentNameLabel.setText("Parent non connecte");
            profileLabel.setText("Aucun profil disponible");
            trendLabel.setText("");
            anomalyLabel.setText("");
            scoreLabel.setText("0/100");
            summaryLabel.setText("Aucune session detectee.");
            reportArea.setText("");
            alertLabel.setText("Aucune donnee.");
            updateBars(null);
            loadHeatmapView();
            return;
        }

        RapportSessionJeu rapport = getOrBuildReport(currentUser);
        ProfilPsychologique profil = rapport.getProfilPsychologique();

        parentNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        profileLabel.setText(profil.getProfil());
        trendLabel.setText("Tendance: " + profil.getTendance());
        anomalyLabel.setText("Anomalie: " + profil.getAnomalie());
        scoreLabel.setText(profil.getScoreEmotionnel() + "/100");
        summaryLabel.setText(rapport.getResumeSession());
        reportArea.setText("Analyse:\n" + rapport.getAnalyseDetaillee() + "\n\nConseils:\n" + rapport.getConseilsParent());
        alertLabel.setText(profil.getAnomalie());
        emailStatusLabel.setText(currentUser.getEmail() == null || currentUser.getEmail().isBlank()
                ? "Aucun email parent configure."
                : "Email parent: " + currentUser.getEmail());
        updateBars(profil);
        loadHeatmapView();
    }

    private RapportSessionJeu getOrBuildReport(User currentUser) {
        currentReport = sessionJeuService.genererRapportSession(currentUser.getId());
        return currentReport;
    }

    private void updateBars(ProfilPsychologique profil) {
        if (profil == null) {
            sociabilityBar.setProgress(0);
            stressBar.setProgress(0);
            timidityBar.setProgress(0);
            curiosityBar.setProgress(0);
            return;
        }
        sociabilityBar.setProgress(normalizeScore(profil.getSociabilite()));
        stressBar.setProgress(normalizeScore(profil.getStress()));
        timidityBar.setProgress(normalizeScore(profil.getTimidite()));
        curiosityBar.setProgress(normalizeScore(profil.getCuriosite()));
    }

    private double normalizeScore(int value) {
        return Math.max(0.0, Math.min(1.0, value / 10.0));
    }

    private void loadHeatmapView() {
        try {
            if (heatmapView == null) {
                heatmapView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/Heatmap.fxml")));
            }
            heatmapContainer.getChildren().setAll(heatmapView);
        } catch (IOException e) {
            System.err.println("Erreur chargement heatmap: " + e.getMessage());
=======

    @FXML private BorderPane mainContainer;

    @FXML
    public void initialize() {
        openTest();
    }

    @FXML
    public void openTest() {
        loadCenter("/GestionParticipations.fxml");
    }

    @FXML
    public void openRapport() {
        loadCenter("/RapportParent.fxml");
    }

    private void loadCenter(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource(fxmlPath))
            );
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur chargement vue : " + e.getMessage());
>>>>>>> Stashed changes
        }
    }
}