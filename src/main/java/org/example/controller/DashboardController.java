package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.entities.ResultatTest;
import org.example.service.ResultatTestService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // =========================================================================
    // FXML
    // =========================================================================

    @FXML private Label  lblTendance, lblMeilleurScore, lblPireScore,
            lblMoyenne,  lblTotalTests;
    @FXML private Canvas canvasCourbe, canvasRadar;
    @FXML private VBox   vboxHistorique;

    // =========================================================================
    // ÉTAT
    // =========================================================================

    private final ResultatTestService service = new ResultatTestService();
    private List<ResultatTest> resultats;

    // =========================================================================
    // INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerDonnees();
    }

    private void chargerDonnees() {
        resultats = service.getDerniersResultats(5);

        Platform.runLater(() -> {
            if (resultats.isEmpty()) {
                afficherEtatVide();
                return;
            }
            mettreAJourStats();
            dessinerCourbeEvolution();
            dessinerRadar();
            afficherCartesHistorique();
        });
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    /**
     * Bouton "← Retour" — navigue vers Resultat.fxml dans la même fenêtre.
     * Si le Dashboard a été ouvert dans une fenêtre secondaire (Stage séparé),
     * on ferme simplement cette fenêtre.
     */
    @FXML
    private void retourAccueil() {
        try {
            Stage stage = obtenirStage();
            if (stage == null) return;

            // Dashboard ouvert en fenêtre secondaire → simple fermeture
            if (stage.getTitle() != null && stage.getTitle().contains("Tableau de bord")) {
                stage.close();
                return;
            }

            // Dashboard dans la même fenêtre → naviguer vers Resultat.fxml
            URL url = getClass().getResource("/fxml.test/Resultat.fxml");
            if (url == null) {
                afficherAlerte("Navigation",
                        "Fichier Resultat.fxml introuvable dans /fxml.test/",
                        Alert.AlertType.ERROR);
                return;
            }
            Parent root = FXMLLoader.load(url);
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Navigation",
                    "Impossible de naviguer vers Resultat.fxml : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    /** Récupère le Stage depuis le premier nœud disponible dans la scène. */
    private Stage obtenirStage() {
        if (lblTendance    != null && lblTendance.getScene()    != null)
            return (Stage) lblTendance.getScene().getWindow();
        if (canvasCourbe   != null && canvasCourbe.getScene()   != null)
            return (Stage) canvasCourbe.getScene().getWindow();
        if (vboxHistorique != null && vboxHistorique.getScene() != null)
            return (Stage) vboxHistorique.getScene().getWindow();
        return null;
    }

    @FXML
    private void rafraichir() {
        chargerDonnees();
    }

    // =========================================================================
    // STATS RÉSUMÉES
    // =========================================================================

    private void mettreAJourStats() {
        if (lblTotalTests != null)
            lblTotalTests.setText(resultats.size() + " test(s)");

        double meilleur = resultats.stream()
                .mapToDouble(ResultatTest::getPourcentage).min().orElse(0);
        double pire     = resultats.stream()
                .mapToDouble(ResultatTest::getPourcentage).max().orElse(0);
        double moyenne  = resultats.stream()
                .mapToDouble(ResultatTest::getPourcentage).average().orElse(0);

        if (lblMeilleurScore != null)
            lblMeilleurScore.setText(String.format("%.0f%%", meilleur * 100));
        if (lblPireScore != null)
            lblPireScore.setText(String.format("%.0f%%", pire * 100));
        if (lblMoyenne != null)
            lblMoyenne.setText(String.format("%.0f%%", moyenne * 100));

        if (resultats.size() >= 2 && lblTendance != null) {
            double recent = resultats.get(0).getPourcentage();
            double ancien = resultats.get(resultats.size() - 1).getPourcentage();
            if (recent < ancien - 0.05) {
                lblTendance.setText("↗ Amélioration");
                lblTendance.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else if (recent > ancien + 0.05) {
                lblTendance.setText("↘ Dégradation");
                lblTendance.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                lblTendance.setText("→ Stable");
                lblTendance.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            }
        }
    }

    // =========================================================================
    // COURBE D'ÉVOLUTION
    // =========================================================================

    private void dessinerCourbeEvolution() {
        if (canvasCourbe == null) return;
        GraphicsContext gc = canvasCourbe.getGraphicsContext2D();
        double w = canvasCourbe.getWidth();
        double h = canvasCourbe.getHeight();
        gc.clearRect(0, 0, w, h);

        double padL = 50, padR = 20, padT = 20, padB = 40;
        double graphW = w - padL - padR;
        double graphH = h - padT - padB;

        gc.setFill(Color.web("#F8F9FA"));
        gc.fillRoundRect(0, 0, w, h, 12, 12);

        // Grilles horizontales
        gc.setStroke(Color.web("#ECF0F1"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double gy = padT + graphH * i / 4;
            gc.strokeLine(padL, gy, padL + graphW, gy);
            gc.setFill(Color.web("#95A5A6"));
            gc.fillText(String.format("%d%%", 100 - i * 25), 5, gy + 4);
        }

        // Labels axe X
        int n = resultats.size();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 0; i < n; i++) {
            ResultatTest r = resultats.get(n - 1 - i);
            double x = padL + i * graphW / Math.max(n - 1, 1);
            String label = r.getDateTest() != null ? r.getDateTest().format(fmt) : "—";
            gc.setFill(Color.web("#7F8C8D"));
            gc.fillText(label, x - 15, h - 8);
        }

        // Coordonnées des points
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            ResultatTest r = resultats.get(n - 1 - i);
            xs[i] = padL + i * graphW / Math.max(n - 1, 1);
            ys[i] = padT + graphH * (1 - r.getPourcentage());
        }

        // Zone sous la courbe
        gc.setFill(Color.web("#3498DB22"));
        gc.beginPath();
        gc.moveTo(xs[0], padT + graphH);
        for (int i = 0; i < n; i++) gc.lineTo(xs[i], ys[i]);
        gc.lineTo(xs[n - 1], padT + graphH);
        gc.closePath();
        gc.fill();

        // Ligne principale
        gc.setStroke(Color.web("#3498DB"));
        gc.setLineWidth(2.5);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) gc.lineTo(xs[i], ys[i]);
        gc.stroke();

        // Points
        for (int i = 0; i < n; i++) {
            ResultatTest r = resultats.get(n - 1 - i);
            Color c = niveauColor(r.getNiveau());
            gc.setFill(c);
            gc.fillOval(xs[i] - 6, ys[i] - 6, 12, 12);
            gc.setFill(Color.WHITE);
            gc.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
            gc.setFill(Color.web("#2C3E50"));
            gc.fillText(r.getScoreActuel() + "/" + r.getScoreMax(), xs[i] - 12, ys[i] - 10);
        }
    }

    // =========================================================================
    // SPIDER CHART (RADAR)
    // =========================================================================

    private void dessinerRadar() {
        if (canvasRadar == null || resultats.isEmpty()) return;
        GraphicsContext gc = canvasRadar.getGraphicsContext2D();
        double w = canvasRadar.getWidth();
        double h = canvasRadar.getHeight();
        gc.clearRect(0, 0, w, h);

        ResultatTest dernier = resultats.get(0);
        double pct = dernier.getPourcentage();
        double[] vals   = {
                Math.min(pct * 1.1, 1.0),
                Math.min(pct * 0.7, 1.0),
                Math.min(pct * 0.9, 1.0)
        };
        String[] labels = {"Émotionnel", "Physique", "Cognitif"};

        double cx = w / 2, cy = h / 2;
        double maxR = Math.min(w, h) / 2 - 30;
        int axes = 3;

        // Grilles
        for (int ring = 1; ring <= 4; ring++) {
            double r = maxR * ring / 4;
            gc.setStroke(Color.web("#ECF0F1"));
            gc.setLineWidth(1);
            double[] px = new double[axes];
            double[] py = new double[axes];
            for (int a = 0; a < axes; a++) {
                double angle = Math.toRadians(-90 + a * 360.0 / axes);
                px[a] = cx + r * Math.cos(angle);
                py[a] = cy + r * Math.sin(angle);
            }
            gc.strokePolygon(px, py, axes);
        }

        // Axes + labels
        for (int a = 0; a < axes; a++) {
            double angle = Math.toRadians(-90 + a * 360.0 / axes);
            gc.setStroke(Color.web("#BDC3C7"));
            gc.setLineWidth(1);
            gc.strokeLine(cx, cy,
                    cx + maxR * Math.cos(angle),
                    cy + maxR * Math.sin(angle));
            double lx = cx + (maxR + 15) * Math.cos(angle);
            double ly = cy + (maxR + 15) * Math.sin(angle);
            gc.setFill(Color.web("#2C3E50"));
            gc.fillText(labels[a], lx - 25, ly + 4);
        }

        // Polygone des valeurs
        double[] vx = new double[axes];
        double[] vy = new double[axes];
        for (int a = 0; a < axes; a++) {
            double angle = Math.toRadians(-90 + a * 360.0 / axes);
            vx[a] = cx + maxR * vals[a] * Math.cos(angle);
            vy[a] = cy + maxR * vals[a] * Math.sin(angle);
        }
        gc.setFill(Color.web("#3498DB44"));
        gc.fillPolygon(vx, vy, axes);
        gc.setStroke(Color.web("#3498DB"));
        gc.setLineWidth(2);
        gc.strokePolygon(vx, vy, axes);

        for (int a = 0; a < axes; a++) {
            gc.setFill(Color.web("#3498DB"));
            gc.fillOval(vx[a] - 4, vy[a] - 4, 8, 8);
        }
    }

    // =========================================================================
    // CARTES HISTORIQUE
    // =========================================================================

    private void afficherCartesHistorique() {
        if (vboxHistorique == null) return;
        vboxHistorique.getChildren().clear();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (ResultatTest r : resultats) {
            HBox card = new HBox(12);
            card.setStyle("""
                    -fx-background-color: white;
                    -fx-background-radius: 10;
                    -fx-padding: 12;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);
                    """);

            Label pastille = new Label(r.getNiveau());
            pastille.setStyle("-fx-background-color: " + niveauHex(r.getNiveau()) + ";"
                    + "-fx-background-radius: 5; -fx-text-fill: white;"
                    + "-fx-padding: 4 8; -fx-font-weight: bold; -fx-font-size: 10;");

            Label infos = new Label(r.getCategorie().toUpperCase() + "  |  "
                    + r.getScoreActuel() + "/" + r.getScoreMax()
                    + "  (" + String.format("%.0f%%", r.getPourcentage() * 100) + ")");
            infos.setStyle("-fx-font-size: 12; -fx-text-fill: #2C3E50;");

            Label date = new Label(r.getDateTest() != null ? r.getDateTest().format(fmt) : "");
            date.setStyle("-fx-font-size: 10; -fx-text-fill: #95A5A6;");

            VBox details = new VBox(3, infos, date);
            card.getChildren().addAll(pastille, details);
            vboxHistorique.getChildren().add(card);
        }
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private void afficherEtatVide() {
        if (lblTendance != null) {
            lblTendance.setText("Aucun test effectué");
            lblTendance.setStyle("-fx-text-fill: #95A5A6;");
        }
    }

    private Color niveauColor(String niveau) {
        if (niveau == null) return Color.web("#2ecc71");
        return switch (niveau.toUpperCase()) {
            case "ÉLEVÉ",  "ELEVE"  -> Color.web("#e74c3c");
            case "MODÉRÉ", "MODERE" -> Color.web("#f39c12");
            default                  -> Color.web("#2ecc71");
        };
    }

    private String niveauHex(String niveau) {
        if (niveau == null) return "#2ecc71";
        return switch (niveau.toUpperCase()) {
            case "ÉLEVÉ",  "ELEVE"  -> "#e74c3c";
            case "MODÉRÉ", "MODERE" -> "#f39c12";
            default                  -> "#2ecc71";
        };
    }

    private void afficherAlerte(String titre, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}