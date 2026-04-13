package org.example.controllers.test;

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

    public void setResultat(int score, String categorie, int nbQuestions) {
        this.categorie = categorie;
        this.scoreActuel = score;
        this.scoreMax = nbQuestions * 3;

        lblCategorie.setText("Catégorie : " + categorie.toUpperCase());
        lblScore.setText(score + " / " + scoreMax);

        String[] resultat = interpreter(score, scoreMax, categorie);
        String niveau = resultat[0];
        Color couleur = Color.web(resultat[1]);
        String description = resultat[2];

        lblNiveau.setText(niveau);
        lblNiveau.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + resultat[1] + ";");
        lblDescription.setText(description);
        dotNiveau.setFill(couleur);
        dotCategorie.setFill(couleur);

        // Fond de la pill selon niveau
        String bgPill = niveau.contains("Faible") || niveau.contains("Positif") || niveau.contains("Excellent")
                ? "#e8f8ef" : niveau.contains("Modéré") || niveau.contains("Moyen") || niveau.contains("Signes Modérés")
                              ? "#fff8e6" : "#fde8e8";
        pillNiveau.setStyle("-fx-background-radius: 20; -fx-padding: 6 14 6 14; -fx-background-color: " + bgPill + ";");

        // Dimensions calculées selon score
        double pct = (double) score / scoreMax;
        final int emo = Math.min((int) Math.round(pct * 10 * 1.1), 10);
        final int phy = Math.min((int) Math.round(pct * 10 * 0.8), 10);
        final int cog = Math.min((int) Math.round(pct * 10 * 1.05), 10);

        lblEmo.setText(emo + "/10");
        lblPhy.setText(phy + "/10");
        lblCog.setText(cog + "/10");

        // Variables final pour le lambda
        final int finalScore = score;
        final int finalScoreMax = this.scoreMax;
        final String finalColor = resultat[1];

        // Dessin après layout
        canvasThermo.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) {
                drawThermometre(finalScore, finalScoreMax);
                drawDimensionBar(barEmo, emo, 10, finalColor);
                drawDimensionBar(barPhy, phy, 10, phy <= 4 ? "#27ae60" : finalColor);
                drawDimensionBar(barCog, cog, 10, finalColor);
            }
        });

        // Liens intelligents selon niveau
        if (categorie.equals("stress") || categorie.equals("depression")) {
            double pourcentage = (double) score / scoreMax * 100;
            if (pourcentage > 66) {
                banniereConsultation.setVisible(true);
                banniereConsultation.setManaged(true);
            } else if (pourcentage > 33) {
                suggestionConsultation.setVisible(true);
                suggestionConsultation.setManaged(true);
            }
        }
    }

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

        // Fond tube
        gc.setFill(Color.web("#e8edf2"));
        gc.fillRoundRect(tubeX, tubeTop, tubeW, tubeH, tubeW, tubeW);

        // Zones colorées (fond)
        double zoneH = tubeH / 3;
        gc.setFill(Color.web("#fde8e8"));
        gc.fillRect(tubeX, tubeTop, tubeW, zoneH);
        gc.setFill(Color.web("#fff8e6"));
        gc.fillRect(tubeX, tubeTop + zoneH, tubeW, zoneH);
        gc.setFill(Color.web("#e8f8ef"));
        gc.fillRect(tubeX, tubeTop + zoneH * 2, tubeW, zoneH);

        // Remplissage selon score
        double pct = (double) score / scoreMax;
        double fillH = tubeH * pct;
        String fillColor = pct > 0.66 ? "#e74c3c" : pct > 0.33 ? "#f39c12" : "#27ae60";
        gc.setFill(Color.web(fillColor));
        gc.fillRoundRect(tubeX, tubeBottom - fillH, tubeW, fillH + 4, 4, 4);

        // Bulbe
        gc.setFill(Color.web(fillColor));
        gc.fillOval(w / 2 - bulbR, bulbCY - bulbR, bulbR * 2, bulbR * 2);

        // Contour tube
        gc.setStroke(Color.web("#d1d9e0"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(tubeX, tubeTop, tubeW, tubeH, tubeW, tubeW);

        // Graduations
        gc.setStroke(Color.web("#c8d0d8"));
        gc.setLineWidth(0.8);
        for (int i = 1; i <= 3; i++) {
            double y = tubeTop + (tubeH / 3) * i;
            gc.strokeLine(tubeX + tubeW, y, tubeX + tubeW + 5, y);
        }
    }

    private void drawDimensionBar(Canvas bar, int value, int max, String color) {
        double w = bar.getWidth();
        double h = bar.getHeight();
        GraphicsContext gc = bar.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#e8edf2"));
        gc.fillRoundRect(0, 0, w, h, h, h);
        double fillW = w * ((double) value / max);
        gc.setFill(Color.web(color));
        gc.fillRoundRect(0, 0, fillW, h, h, h);
    }

    private String[] interpreter(int score, int scoreMax, String categorie) {
        double pourcentage = (double) score / scoreMax * 100;

        switch (categorie) {
            case "stress":
                if (pourcentage <= 33) return new String[]{
                        "Stress faible", "#27ae60",
                        "Votre niveau de stress est faible. Continuez à prendre soin de vous !"
                };
                else if (pourcentage <= 66) return new String[]{
                        "Stress modéré", "#f39c12",
                        "Votre niveau est dans la zone orange.\nQuelques ajustements simples peuvent\nfaire une grande différence."
                };
                else return new String[]{
                            "Stress élevé", "#e74c3c",
                            "Votre niveau de stress est élevé.\nUne consultation professionnelle\nest fortement conseillée."
                    };

            case "depression":
                if (pourcentage <= 33) return new String[]{
                        "État positif", "#27ae60",
                        "Votre état mental semble bon. Continuez à maintenir un mode de vie sain !"
                };
                else if (pourcentage <= 66) return new String[]{
                        "Signes modérés", "#f39c12",
                        "Quelques signes détectés. Parlez-en à un proche ou un professionnel."
                };
                else return new String[]{
                            "Signes importants", "#e74c3c",
                            "Des signes importants sont détectés. Consultez un professionnel de santé."
                    };

            case "iq":
                if (pourcentage <= 33) return new String[]{
                        "Score faible", "#e74c3c",
                        "Continuez à vous exercer, la pratique améliore les capacités cognitives !"
                };
                else if (pourcentage <= 66) return new String[]{
                        "Score moyen", "#f39c12",
                        "Bon résultat ! Vous avez de bonnes capacités de raisonnement logique."
                };
                else return new String[]{
                            "Excellent score", "#27ae60",
                            "Excellent ! Vous avez de très bonnes capacités cognitives et logiques."
                    };

            default:
                return new String[]{"Résultat", "#2980b9", ""};
        }
    }

    @FXML
    private void ouvrirJournal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/journal.fxml")
            );
            Parent root = loader.load();
            naviguerVers(root);
        } catch (Exception e) {
            System.out.println("Erreur ouverture journal : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirConsultation() {
        try {
            java.net.URL url = getClass().getResource("/consultations.fxml");
            System.out.println("URL consultations : " + url);
            if (url == null) {
                System.out.println("INTROUVABLE — vérifiez que consultations.fxml est bien dans src/main/resources/");
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ConsultationController) {
                ((ConsultationController) controller).setContexteDepuisTest(
                        scoreActuel, scoreMax, categorie
                );
            }

            naviguerVers(root);
        } catch (Exception e) {
            System.out.println("Erreur ouverture consultation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void naviguerVers(Parent root) {
        Stage stage = (Stage) lblScore.getScene().getWindow();
        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
    }

    @FXML
    private void refaire() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/test/PasserTest.fxml")
            );
            Parent root = loader.load();
            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);
            naviguerVers(root);
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void accueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/test/ChoixCategorie.fxml")
            );
            Parent root = loader.load();
            naviguerVers(root);
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    /**
     * Interface optionnelle à implémenter dans votre ConsultationController
     * pour recevoir le contexte du test.
     */
    public interface ConsultationController {
        void setContexteDepuisTest(int score, int scoreMax, String categorie);
    }
}