package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class ResultatController {

    @FXML private Label lblCategorie;
    @FXML private Label lblScore;
    @FXML private Label lblNiveau;
    @FXML private Label lblDescription;

    @FXML private Circle dotCategorie;
    @FXML private Circle dotNiveau;
    @FXML private HBox pillNiveau;

    @FXML private Canvas canvasThermo;
    @FXML private Canvas barEmo;
    @FXML private Canvas barPhy;
    @FXML private Canvas barCog;

    @FXML private Label lblEmo;
    @FXML private Label lblPhy;
    @FXML private Label lblCog;

    @FXML private VBox banniereConsultation;
    @FXML private VBox suggestionConsultation;
    @FXML private VBox cardJournal;

    private String categorie;
    private int scoreActuel;
    private int scoreMax;

    // =========================
    // SET RESULTAT
    // =========================
    public void setResultat(int score, String categorie, int nbQuestions) {

        this.categorie = categorie;
        this.scoreActuel = score;
        this.scoreMax = nbQuestions * 3;

        lblCategorie.setText("Catégorie : " + categorie.toUpperCase());
        lblScore.setText(score + " / " + scoreMax);

        String[] resultat = interpreter(score, scoreMax, categorie);

        String niveau = resultat[0];
        String couleurHex = resultat[1];
        String description = resultat[2];

        Color couleur = Color.web(couleurHex);

        lblNiveau.setText(niveau);
        lblNiveau.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + couleurHex + ";");

        lblDescription.setText(description);

        dotNiveau.setFill(couleur);
        dotCategorie.setFill(couleur);

        // Couleur fond pill
        String bgPill =
                niveau.contains("Faible") || niveau.contains("Positif") || niveau.contains("Excellent")
                        ? "#e8f8ef"
                        : niveau.contains("Modéré") || niveau.contains("Moyen")
                          ? "#fff8e6"
                          : "#fde8e8";

        pillNiveau.setStyle(
                "-fx-background-radius: 20;" +
                        "-fx-padding: 6 14 6 14;" +
                        "-fx-background-color: " + bgPill + ";"
        );

        // =========================
        // DIMENSIONS
        // =========================
        double pct = (double) score / scoreMax;

        final int emo = Math.min((int) Math.round(pct * 10 * 1.1), 10);
        final int phy = Math.min((int) Math.round(pct * 10 * 0.8), 10); // FIX BUG
        final int cog = Math.min((int) Math.round(pct * 10 * 1.05), 10);

        lblEmo.setText(emo + "/10");
        lblPhy.setText(phy + "/10");
        lblCog.setText(cog + "/10");

        final int finalScore = score;
        final int finalScoreMax = scoreMax;
        final String finalColor = couleurHex;

        // =========================
        // DRAW SAFE (JavaFX Thread)
        // =========================
        Platform.runLater(() -> {
            drawThermometre(finalScore, finalScoreMax);
            drawDimensionBar(barEmo, emo, 10, finalColor);
            drawDimensionBar(barPhy, phy, 10, phy <= 4 ? "#27ae60" : finalColor);
            drawDimensionBar(barCog, cog, 10, finalColor);
        });

        // =========================
        // LOGIQUE CONSULTATION
        // =========================
        if (categorie.equals("stress") || categorie.equals("depression")) {

            double pourcentage = pct * 100;

            if (pourcentage > 66) {
                banniereConsultation.setVisible(true);
                banniereConsultation.setManaged(true);

            } else if (pourcentage > 33) {
                suggestionConsultation.setVisible(true);
                suggestionConsultation.setManaged(true);
            }
        }
    }

    // =========================
    // THERMOMETRE
    // =========================
    private void drawThermometre(int score, int scoreMax) {

        GraphicsContext gc = canvasThermo.getGraphicsContext2D();

        double w = canvasThermo.getWidth();
        double h = canvasThermo.getHeight();

        gc.clearRect(0, 0, w, h);

        double tubeX = w / 2 - 5;
        double tubeW = 10;
        double tubeTop = 12;
        double tubeBottom = h - 26;
        double tubeH = tubeBottom - tubeTop;
        double bulbR = 13;
        double bulbCY = h - bulbR - 2;

        // fond
        gc.setFill(Color.web("#e8edf2"));
        gc.fillRoundRect(tubeX, tubeTop, tubeW, tubeH, tubeW, tubeW);

        // zones
        double zoneH = tubeH / 3;
        gc.setFill(Color.web("#fde8e8"));
        gc.fillRect(tubeX, tubeTop, tubeW, zoneH);

        gc.setFill(Color.web("#fff8e6"));
        gc.fillRect(tubeX, tubeTop + zoneH, tubeW, zoneH);

        gc.setFill(Color.web("#e8f8ef"));
        gc.fillRect(tubeX, tubeTop + 2 * zoneH, tubeW, zoneH);

        // remplissage
        double pct = (double) score / scoreMax;
        double fillH = tubeH * pct;

        String fillColor = pct > 0.66 ? "#e74c3c"
                : pct > 0.33 ? "#f39c12"
                  : "#27ae60";

        gc.setFill(Color.web(fillColor));
        gc.fillRoundRect(tubeX, tubeBottom - fillH, tubeW, fillH, 4, 4);

        // bulbe
        gc.setFill(Color.web(fillColor));
        gc.fillOval(w / 2 - bulbR, bulbCY - bulbR, bulbR * 2, bulbR * 2);

        // contour
        gc.setStroke(Color.web("#d1d9e0"));
        gc.strokeRoundRect(tubeX, tubeTop, tubeW, tubeH, tubeW, tubeW);
    }

    // =========================
    // BARRE DIMENSION
    // =========================
    private void drawDimensionBar(Canvas bar, int value, int max, String color) {

        GraphicsContext gc = bar.getGraphicsContext2D();

        double w = bar.getWidth();
        double h = bar.getHeight();

        gc.clearRect(0, 0, w, h);

        gc.setFill(Color.web("#e8edf2"));
        gc.fillRoundRect(0, 0, w, h, h, h);

        double fillW = w * ((double) value / max);

        gc.setFill(Color.web(color));
        gc.fillRoundRect(0, 0, fillW, h, h, h);
    }

    // =========================
    // INTERPRETATION
    // =========================
    private String[] interpreter(int score, int scoreMax, String categorie) {

        double pourcentage = (double) score / scoreMax * 100;

        switch (categorie) {

            case "stress":
                if (pourcentage <= 33)
                    return new String[]{"Stress faible", "#27ae60",
                            "Votre niveau de stress est faible."};

                else if (pourcentage <= 66)
                    return new String[]{"Stress modéré", "#f39c12",
                            "Zone orange : ajustements recommandés."};

                else
                    return new String[]{"Stress élevé", "#e74c3c",
                            "Consultation recommandée."};

            case "depression":
                if (pourcentage <= 33)
                    return new String[]{"État positif", "#27ae60",
                            "Bon état mental."};

                else if (pourcentage <= 66)
                    return new String[]{"Signes modérés", "#f39c12",
                            "Surveillance recommandée."};

                else
                    return new String[]{"Signes importants", "#e74c3c",
                            "Consultez un professionnel."};

            case "iq":
                if (pourcentage <= 33)
                    return new String[]{"Score faible", "#e74c3c",
                            "Continuez à pratiquer."};

                else if (pourcentage <= 66)
                    return new String[]{"Score moyen", "#f39c12",
                            "Bon raisonnement logique."};

                else
                    return new String[]{"Excellent score", "#27ae60",
                            "Très bonnes capacités cognitives."};

            default:
                return new String[]{"Résultat", "#2980b9", ""};
        }
    }

    // =========================
    // NAVIGATION
    // =========================
    private void naviguerVers(Parent root) {

        Stage stage = (Stage) lblScore.getScene().getWindow();

        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());

        stage.setScene(scene);
        stage.setMaximized(true);
    }

    // =========================
    // ACTIONS UI
    // =========================
    @FXML
    private void ouvrirJournal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/journal.fxml"));
            Parent root = loader.load();
            naviguerVers(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void ouvrirConsultation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/consultations.fxml"));
            Parent root = loader.load();

            ConsultationController controller = loader.getController();
            controller.setContexteDepuisTest(scoreActuel, scoreMax, categorie);

            naviguerVers(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            naviguerVers(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void accueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/ChoixCategorie.fxml"));
            Parent root = loader.load();
            naviguerVers(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // INTERFACE
    // =========================
    public interface ConsultationController {
        void setContexteDepuisTest(int score, int scoreMax, String categorie);
    }
}