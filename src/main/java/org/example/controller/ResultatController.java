package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.ResultatTest;
import org.example.entities.TestAdaptatif;
import org.example.service.GrokAIService;
import org.example.service.PdfExportService;
import org.example.service.ResultatTestService;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultatController implements Initializable {

    @FXML private Label  lblCategorie, lblScore, lblNiveau, lblDescription,
            lblAnalyseIA, lblStatutAnalyse, lblEmo, lblPhy, lblCog;
    @FXML private Circle dotNiveau;
    @FXML private HBox   pillNiveau;
    @FXML private Canvas canvasScore, canvasThermo, barEmo, barPhy, barCog;
    @FXML private VBox   cardAnalyseIA, vboxThemes;

    private final GrokAIService       grokAIService       = new GrokAIService();
    private final ResultatTestService resultatTestService = new ResultatTestService();
    private final PdfExportService    pdfExportService    = new PdfExportService();

    private String        categorie;
    private int           scoreActuel, scoreMax;
    private TestAdaptatif testAdaptatif;
    private ResultatTest  resultatSauvegarde;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ia-thread");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (cardAnalyseIA != null) {
            cardAnalyseIA.setVisible(false);
            cardAnalyseIA.setManaged(false);
        }
        if (lblStatutAnalyse != null) lblStatutAnalyse.setText("");
    }

    // ── Points d'entrée publics ──────────────────────────────────────────────

    public void setResultat(int score, String categorie, int nbQuestions) {
        this.categorie   = categorie;
        this.scoreActuel = score;
        this.scoreMax    = nbQuestions * 3;
        refreshUI();
        sauvegarderResultat(null);
    }

    public void setResultatAvecTestAdaptatif(int score, int scoreMax, String categorie,
                                             TestAdaptatif testAdaptatif) {
        this.categorie     = categorie;
        this.scoreActuel   = score;
        this.scoreMax      = scoreMax;
        this.testAdaptatif = testAdaptatif;
        refreshUI();
        sauvegarderResultat(null);
        if (grokAIService.isConfigured()) lancerAnalyseIAAsynchrone();
    }

    // ── Sauvegarde MySQL ─────────────────────────────────────────────────────

    private void sauvegarderResultat(String analyseIA) {
        ResultatInterpretation interp = interpreterScore(scoreActuel, scoreMax);
        ResultatTest r = new ResultatTest(
                categorie, scoreActuel, scoreMax,
                interp.niveau, analyseIA, LocalDateTime.now());
        resultatTestService.sauvegarder(r);
        this.resultatSauvegarde = r;
    }

    // ── Refresh UI ───────────────────────────────────────────────────────────

    private void refreshUI() {
        Platform.runLater(() -> {
            if (lblCategorie != null) lblCategorie.setText(categorie.toUpperCase());
            ResultatInterpretation interp = interpreterScore(scoreActuel, scoreMax);
            lblNiveau.setText(interp.niveau);
            lblNiveau.setStyle("-fx-text-fill: " + interp.couleur + ";");
            lblDescription.setText(interp.description);
            dotNiveau.setFill(Color.web(interp.couleur));
            pillNiveau.setStyle("-fx-background-color: " + interp.couleur + "22; -fx-background-radius: 20;");
            double pct = (double) scoreActuel / scoreMax;
            drawScoreArc(canvasScore, scoreActuel, scoreMax, interp.couleur);
            drawThermometre(canvasThermo, pct);
            double e = Math.min(pct * 1.1, 1.0);
            double p = Math.min(pct * 0.7, 1.0);
            double c = Math.min(pct * 0.9, 1.0);
            drawBar(barEmo, e, "#3498db");
            drawBar(barPhy, p, "#2ecc71");
            drawBar(barCog, c, "#f1c40f");
            lblEmo.setText((int)(e * 10) + "/10");
            lblPhy.setText((int)(p * 10) + "/10");
            lblCog.setText((int)(c * 10) + "/10");
        });
    }

    // ── Analyse IA asynchrone ────────────────────────────────────────────────

    private void lancerAnalyseIAAsynchrone() {
        cardAnalyseIA.setManaged(true);
        cardAnalyseIA.setVisible(true);
        lblStatutAnalyse.setText("✨ Analyse IA en cours...");
        lblStatutAnalyse.setStyle("-fx-text-fill: #3498db;");
        executor.submit(() -> {
            try {
                String analyseFull = grokAIService.genererAnalyse(
                        categorie, testAdaptatif.getQuestionsReponses(), scoreActuel, scoreMax / 3);
                Platform.runLater(() -> {
                    lblAnalyseIA.setText(analyseFull);
                    lblStatutAnalyse.setText("✅ Analyse complétée");
                    lblStatutAnalyse.setStyle("-fx-text-fill: #2ecc71;");
                    if (resultatSauvegarde != null) {
                        resultatSauvegarde.setAnalyseIA(analyseFull);
                        resultatTestService.sauvegarder(resultatSauvegarde);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatutAnalyse.setText("⚠️ Erreur de connexion IA");
                    lblStatutAnalyse.setStyle("-fx-text-fill: #e74c3c;");
                });
            }
        });
    }

    // ── ACTION : Exporter PDF ────────────────────────────────────────────────

    @FXML
    private void exporterPDF() {
        if (resultatSauvegarde == null) {
            afficherAlerte("Export PDF", "Aucun résultat à exporter.", Alert.AlertType.WARNING);
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("rapport_" + categorie + "_"
                + LocalDateTime.now().toString().replace(":", "-").substring(0, 16) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        Stage stage = (Stage) lblScore.getScene().getWindow();
        File dest = fc.showSaveDialog(stage);
        if (dest == null) return;
        executor.submit(() -> {
            try {
                if (lblAnalyseIA != null && lblAnalyseIA.getText() != null
                        && !lblAnalyseIA.getText().isBlank())
                    resultatSauvegarde.setAnalyseIA(lblAnalyseIA.getText());
                pdfExportService.exporterResultat(resultatSauvegarde, dest);
                Platform.runLater(() -> afficherAlerte("Export réussi",
                        "Rapport PDF généré :\n" + dest.getAbsolutePath(),
                        Alert.AlertType.INFORMATION));
            } catch (Exception ex) {
                Platform.runLater(() -> afficherAlerte("Erreur PDF",
                        "Impossible de générer le PDF : " + ex.getMessage(),
                        Alert.AlertType.ERROR));
            }
        });
    }

    // ── ACTION : Dashboard (nouvelle fenêtre) ────────────────────────────────

    @FXML
    private void ouvrirDashboard() {
        try {
            URL url = getClass().getResource("/Dashboard.fxml");
            if (url == null) {
                afficherAlerte("Dashboard",
                        "Fichier Dashboard.fxml introuvable dans resources.",
                        Alert.AlertType.ERROR);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = new Stage();
            stage.setTitle("📊 Tableau de bord — Évolution");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            afficherAlerte("Dashboard",
                    "Impossible d'ouvrir le tableau de bord : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ── NAVIGATION — même fenêtre ────────────────────────────────────────────

    @FXML
    private void accueil() {
        naviguerVers("/patient_dashboard.fxml");
    }

    @FXML
    private void ouvrirJournal() {
        naviguerVers("/journal.fxml");
    }

    @FXML
    private void ouvrirConsultation() {
        naviguerVers("/consultations.fxml");
    }

    @FXML
    private void refaire() {
        naviguerVers("/fxml.test/passer_test.fxml");
    }

    /**
     * Remplace le contenu de la scène courante par le FXML indiqué.
     * Le Stage (fenêtre) reste le même — seule la page change.
     */
    private void naviguerVers(String cheminFxml) {
        try {
            URL url = getClass().getResource(cheminFxml);
            if (url == null) {
                afficherAlerte("Navigation",
                        "Page introuvable : " + cheminFxml,
                        Alert.AlertType.ERROR);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) lblScore.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            afficherAlerte("Navigation",
                    "Impossible d'ouvrir la page : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ── Graphiques ───────────────────────────────────────────────────────────

    private void drawScoreArc(Canvas canvas, double s, double m, String color) {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        double cx = w / 2, cy = h - 20, r = 80;
        gc.setLineWidth(16);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.web("#ECF0F1"));
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 180, -180, ArcType.OPEN);
        gc.setStroke(Color.web(color));
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 180, -(s / m) * 180, ArcType.OPEN);
        lblScore.setText((int) s + " / " + (int) m);
    }

    private void drawThermometre(Canvas canvas, double pct) {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        double tx = w / 2 - 6, tw = 12, th = h - 60;
        gc.setFill(Color.web("#ECF0F1"));
        gc.fillRoundRect(tx, 10, tw, th, tw, tw);
        String color = pct > 0.66 ? "#e74c3c" : pct > 0.33 ? "#f39c12" : "#2ecc71";
        gc.setFill(Color.web(color));
        double mH = th * pct;
        gc.fillRoundRect(tx, 10 + (th - mH), tw, mH + 5, tw, tw);
        gc.fillOval(w / 2 - 15, h - 35, 30, 30);
    }

    private void drawBar(Canvas canvas, double pct, String color) {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#ECF0F1"));
        gc.fillRoundRect(0, 0, w, h, 10, 10);
        gc.setFill(Color.web(color));
        gc.fillRoundRect(0, 0, w * pct, h, 10, 10);
    }

    // ── Interprétation ───────────────────────────────────────────────────────

    private ResultatInterpretation interpreterScore(int s, int m) {
        double p = (double) s / m;
        if (p < 0.33) return new ResultatInterpretation("FAIBLE",  "#2ecc71", "Votre état semble stable et maîtrisé.");
        if (p < 0.66) return new ResultatInterpretation("MODÉRÉ",  "#f39c12", "Quelques points de vigilance ont été identifiés.");
        return          new ResultatInterpretation("ÉLEVÉ",  "#e74c3c", "Il est fortement recommandé de consulter un spécialiste.");
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private void afficherAlerte(String titre, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private static class ResultatInterpretation {
        String niveau, couleur, description;
        ResultatInterpretation(String n, String c, String d) {
            this.niveau = n; this.couleur = c; this.description = d;
        }
    }
}