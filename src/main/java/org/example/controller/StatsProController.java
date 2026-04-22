package org.example.controller;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.util.Duration;
import org.example.entities.Journal;
import org.example.entities.User;
import org.example.service.JournalService;
import org.example.service.MoodStatsService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsProController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label totalLabel;
    @FXML private Label dominantMoodLabel;
    @FXML private Label stabilityLabel;
    @FXML private Label generatedAtLabel;
    @FXML private Label heureuxPercentLabel;
    @FXML private Label calmePercentLabel;
    @FXML private Label sosPercentLabel;
    @FXML private Label colerePercentLabel;
    @FXML private Label heureuxDetailLabel;
    @FXML private Label calmeDetailLabel;
    @FXML private Label sosDetailLabel;
    @FXML private Label colereDetailLabel;
    @FXML private Arc heureuxArc;
    @FXML private Arc calmeArc;
    @FXML private Arc sosArc;
    @FXML private Arc colereArc;
    @FXML private Pane heureuxOrbitPane;
    @FXML private Pane calmeOrbitPane;
    @FXML private Pane sosOrbitPane;
    @FXML private Pane colereOrbitPane;
    @FXML private BarChart<String, Number> distributionChart;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private VBox distributionInsightBox;
    @FXML private VBox anomalyBox;

    private final JournalService journalService = new JournalService();
    private User currentUser;

    @FXML
    public void initialize() {
        startDecorativeAnimations();
        setUserData(UserSession.getInstance());
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        if (pageTitleLabel != null) {
            pageTitleLabel.setText("Stat pro complet - " + user.getPrenom());
        }
        if (pageSubtitleLabel != null) {
            pageSubtitleLabel.setText("Analyse detaillee des journaux, tendances et alertes pour " + user.getPrenom() + ".");
        }
        loadStats();
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
    private void goToJournaux() {
        loadView("/journal.fxml", true);
    }

    @FXML
    private void handleLogout() {
        UserSession.setInstance(null);
        loadView("/login.fxml", false);
    }

    private void loadStats() {
        if (currentUser == null) {
            return;
        }

        try {
            List<Journal> journals = journalService.findByUser(currentUser, "", "recent");
            MoodStatsService.ProfessionalMoodStats stats = MoodStatsService.computeProfessionalStats(journals);
            renderSummary(stats);
            renderMoodRings(stats);
            renderDistributionChart(stats);
            renderTrendChart(stats);
            renderInsights(stats);
        } catch (Exception e) {
            if (pageSubtitleLabel != null) {
                pageSubtitleLabel.setText("Chargement des statistiques impossible : " + e.getMessage());
            }
        }
    }

    private void renderSummary(MoodStatsService.ProfessionalMoodStats stats) {
        totalLabel.setText(String.valueOf(stats.total));
        dominantMoodLabel.setText(formatMood(stats.dominantMood));
        stabilityLabel.setText(stats.stability == null ? "Inconnue" : stats.stability);
        generatedAtLabel.setText(stats.generatedAt == null ? "-" : stats.generatedAt.format(DATE_TIME_FORMAT));
    }

    private void renderMoodRings(MoodStatsService.ProfessionalMoodStats stats) {
        updateMoodRing("heureux", heureuxArc, heureuxPercentLabel, heureuxDetailLabel, stats);
        updateMoodRing("calme", calmeArc, calmePercentLabel, calmeDetailLabel, stats);
        updateMoodRing("sos", sosArc, sosPercentLabel, sosDetailLabel, stats);
        updateMoodRing("en colere", colereArc, colerePercentLabel, colereDetailLabel, stats);
    }

    private void updateMoodRing(String key, Arc arc, Label percentLabel, Label detailLabel, MoodStatsService.ProfessionalMoodStats stats) {
        int count = stats.counts.getOrDefault(key, 0);
        double percent = stats.percentages.getOrDefault(key, 0.0);
        animateRing(arc, percent);
        int rounded = (int) Math.round(percent * 100);
        percentLabel.setText(rounded + "%");
        detailLabel.setText(count + (count > 1 ? " journaux" : " journal"));
    }

    private void renderDistributionChart(MoodStatsService.ProfessionalMoodStats stats) {
        distributionChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Heureux", stats.counts.getOrDefault("heureux", 0)));
        series.getData().add(new XYChart.Data<>("Calme", stats.counts.getOrDefault("calme", 0)));
        series.getData().add(new XYChart.Data<>("SOS", stats.counts.getOrDefault("sos", 0)));
        series.getData().add(new XYChart.Data<>("En colere", stats.counts.getOrDefault("en colere", 0)));
        distributionChart.getData().add(series);
    }

    private void renderTrendChart(MoodStatsService.ProfessionalMoodStats stats) {
        trendChart.getData().clear();
        trendChart.setLegendVisible(true);

        trendChart.getData().add(buildTrendSeries("Heureux", "heureux", stats.timeSeries));
        trendChart.getData().add(buildTrendSeries("Calme", "calme", stats.timeSeries));
        trendChart.getData().add(buildTrendSeries("SOS", "sos", stats.timeSeries));
        trendChart.getData().add(buildTrendSeries("En colere", "en colere", stats.timeSeries));
    }

    private XYChart.Series<String, Number> buildTrendSeries(String label, String moodKey, Map<LocalDate, Map<String, Integer>> timeSeries) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(label);
        if (timeSeries == null) {
            return series;
        }
        for (Map.Entry<LocalDate, Map<String, Integer>> entry : timeSeries.entrySet()) {
            String day = entry.getKey().format(DAY_FORMAT);
            int value = entry.getValue().getOrDefault(moodKey, 0);
            series.getData().add(new XYChart.Data<>(day, value));
        }
        return series;
    }

    private void renderInsights(MoodStatsService.ProfessionalMoodStats stats) {
        distributionInsightBox.getChildren().clear();
        anomalyBox.getChildren().clear();

        distributionInsightBox.getChildren().add(buildInfoLabel("Dominante actuelle : " + formatMood(stats.dominantMood)));
        distributionInsightBox.getChildren().add(buildInfoLabel("Lecture generale : " + (stats.stability == null ? "Inconnue" : stats.stability)));

        String intenseMood = findHighestPercentageMood(stats.percentages);
        if (intenseMood != null) {
            distributionInsightBox.getChildren().add(buildInfoLabel("Humeur la plus presente : " + formatMood(intenseMood)));
        }

        List<String> anomalyLines = new ArrayList<>();
        if (stats.anomalies != null) {
            for (Map.Entry<String, List<LocalDate>> entry : stats.anomalies.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    LocalDate lastDate = entry.getValue().get(entry.getValue().size() - 1);
                    anomalyLines.add(formatMood(entry.getKey()) + " en pic le " + lastDate.format(DAY_FORMAT));
                }
            }
        }

        if (anomalyLines.isEmpty()) {
            anomalyBox.getChildren().add(buildInfoLabel("Aucune anomalie nette detectee sur la periode."));
        } else {
            for (String line : anomalyLines) {
                anomalyBox.getChildren().add(buildInfoLabel(line));
            }
        }
    }

    private Label buildInfoLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: rgba(255,255,255,0.72); -fx-background-radius: 12; -fx-padding: 10 12; -fx-text-fill: #36515d;");
        return label;
    }

    private String findHighestPercentageMood(Map<String, Double> percentages) {
        String bestMood = null;
        double bestValue = -1;
        if (percentages == null) {
            return null;
        }
        for (Map.Entry<String, Double> entry : percentages.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > bestValue) {
                bestValue = entry.getValue();
                bestMood = entry.getKey();
            }
        }
        return bestMood;
    }

    private void animateRing(Arc arc, double targetProgress) {
        if (arc == null) {
            return;
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(arc.lengthProperty(), arc.getLength(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(900), new KeyValue(arc.lengthProperty(), -360 * targetProgress, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private void startDecorativeAnimations() {
        startOrbitAnimation(heureuxOrbitPane, 16);
        startOrbitAnimation(calmeOrbitPane, -18);
        startOrbitAnimation(sosOrbitPane, 14);
        startOrbitAnimation(colereOrbitPane, -15);
    }

    private void startOrbitAnimation(Pane pane, double seconds) {
        if (pane == null) {
            return;
        }
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(Math.abs(seconds)), pane);
        rotateTransition.setByAngle(seconds > 0 ? 360 : -360);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
    }

    private String formatMood(String mood) {
        if (mood == null || mood.isBlank()) {
            return "Aucune";
        }
        return switch (mood.toLowerCase()) {
            case "heureux" -> "Heureux";
            case "calme" -> "Calme";
            case "sos" -> "SOS";
            case "en colere", "colere" -> "En colere";
            default -> mood;
        };
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
                } else if (controller instanceof StatsProController statsProController) {
                    statsProController.setUserData(currentUser);
                }
            }

            totalLabel.getScene().setRoot(root);
        } catch (IOException e) {
            if (pageSubtitleLabel != null) {
                pageSubtitleLabel.setText("Navigation impossible : " + e.getMessage());
            }
        }
    }
}
