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
import org.example.controller.JournalController;
import org.example.entities.TestResultMedical;
import org.example.entities.User;
import org.example.service.MedicalDataService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

public class ResultatController {

    @FXML
    private Circle dotCategorie;
    @FXML
    private Label lblCategorie;
    @FXML
    private Canvas canvasThermo;
    @FXML
    private Label lblScore;
    @FXML
    private HBox pillNiveau;
    @FXML
    private Circle dotNiveau;
    @FXML
    private Label lblNiveau;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblEmo;
    @FXML
    private Label lblPhy;
    @FXML
    private Label lblCog;
    @FXML
    private Canvas barEmo;
    @FXML
    private Canvas barPhy;
    @FXML
    private Canvas barCog;
    @FXML
    private VBox banniereConsultation;
    @FXML
    private VBox suggestionConsultation;

    private String categorie;
    private int score;
    private int scoreMax;
    private final MedicalDataService medicalDataService = new MedicalDataService();
    private boolean resultSaved;

    public void setResultat(int score, int scoreMax, String categorie) {
        this.score = score;
        this.scoreMax = Math.max(scoreMax, 1);
        this.categorie = categorie;
        saveResultIfPossible();

        ResultData data = interpreter(score, this.scoreMax, categorie);
        lblCategorie.setText("Categorie : " + data.categorieLabel);
        lblScore.setText(score + " / " + this.scoreMax);
        lblNiveau.setText(data.niveau);
        lblDescription.setText(data.description);

        Color accent = Color.web(data.couleur);
        dotCategorie.setFill(accent);
        dotNiveau.setFill(accent);
        pillNiveau.setStyle(
                "-fx-background-color: " + rgba(accent, 0.14) + ";" +
                "-fx-background-radius: 20; -fx-padding: 6 14 6 14;"
        );
        lblNiveau.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + data.couleur + ";");

        configurerDimensions(accent);
        dessinerThermometre(accent);

        banniereConsultation.setVisible(false);
        banniereConsultation.setManaged(false);
        suggestionConsultation.setVisible(false);
        suggestionConsultation.setManaged(false);
    }

    private void saveResultIfPossible() {
        if (resultSaved) {
            return;
        }
        User currentUser = UserSession.getInstance();
        if (currentUser == null) {
            return;
        }

        TestResultMedical result = new TestResultMedical();
        result.setPatientId(currentUser.getId());
        result.setCategorie(formatCategorie(categorie));
        result.setScore(score);
        result.setScoreMax(scoreMax);

        try {
            medicalDataService.ensureSchema();
            medicalDataService.saveTestResult(result);
            resultSaved = true;
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde resultat test : " + e.getMessage());
        }
    }

    @FXML
    private void refaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();
            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);
            changerScene(root, "Test - " + categorie);
        } catch (IOException e) {
            System.err.println("Erreur relance test : " + e.getMessage());
        }
    }

    @FXML
    private void accueil() {
        changerScene("/fxml/test/ChoixCategorie.fxml", "Choix du test");
    }

    @FXML
    private void ouvrirJournal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/journal.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            User user = UserSession.getInstance();
            if (controller instanceof JournalController journalController && user != null) {
                journalController.setUserData(user);
            }

            changerScene(root, "Journal");
        } catch (IOException e) {
            System.err.println("Erreur ouverture journal : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirConsultation() {
        changerScene("/patient_dashboard.fxml", "Espace Patient");
    }

    private void configurerDimensions(Color accent) {
        double ratio = (double) score / scoreMax;
        int emo = (int) Math.round(Math.min(10, Math.max(0, ratio * 10)));
        int phy = (int) Math.round(Math.min(10, Math.max(0, ratio * 8 + 1)));
        int cog = (int) Math.round(Math.min(10, Math.max(0, ratio * 9)));

        lblEmo.setText(emo + "/10");
        lblPhy.setText(phy + "/10");
        lblCog.setText(cog + "/10");

        dessinerBarre(barEmo, emo / 10.0, accent);
        dessinerBarre(barPhy, phy / 10.0, accent);
        dessinerBarre(barCog, cog / 10.0, accent);
    }

    private void dessinerBarre(Canvas canvas, double ratio, Color accent) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.web("#e8edf2"));
        gc.fillRoundRect(0, 0, canvas.getWidth(), canvas.getHeight(), 4, 4);
        gc.setFill(accent);
        gc.fillRoundRect(0, 0, canvas.getWidth() * Math.max(0, Math.min(1, ratio)), canvas.getHeight(), 4, 4);
    }

    private void dessinerThermometre(Color accent) {
        GraphicsContext gc = canvasThermo.getGraphicsContext2D();
        double width = canvasThermo.getWidth();
        double height = canvasThermo.getHeight();
        double ratio = Math.max(0, Math.min(1, (double) score / scoreMax));

        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#eef2f7"));
        gc.fillRoundRect(width / 2 - 10, 10, 20, height - 35, 10, 10);
        gc.setFill(accent);
        double fillHeight = (height - 35) * ratio;
        gc.fillRoundRect(width / 2 - 10, height - 25 - fillHeight, 20, fillHeight, 10, 10);
        gc.fillOval(width / 2 - 16, height - 32, 32, 32);
    }

    private ResultData interpreter(int score, int scoreMax, String categorie) {
        double pourcentage = (double) score / scoreMax;
        String cle = normaliserCategorie(categorie);

        if ("iq".equals(cle)) {
            if (pourcentage < 0.34) {
                return new ResultData("IQ", "Score faible", "#ef4444",
                        "Continuez a vous entrainer. La logique et l'attention progressent avec la pratique.");
            }
            if (pourcentage < 0.67) {
                return new ResultData("IQ", "Score moyen", "#f59e0b",
                        "Bon resultat. Vos capacites de raisonnement sont dans une zone correcte.");
            }
            return new ResultData("IQ", "Excellent score", "#22c55e",
                    "Tres bon resultat. Vous avez montre de solides capacites cognitives.");
        }

        if ("depression".equals(cle)) {
            if (pourcentage < 0.34) {
                return new ResultData("Depression", "Faible", "#22c55e",
                        "Peu d'indicateurs ressortent sur ce test. Continuez a surveiller votre bien-etre.");
            }
            if (pourcentage < 0.67) {
                return new ResultData("Depression", "Modere", "#f59e0b",
                        "Quelques signes meritent votre attention. Parler a un proche ou a un professionnel peut aider.");
            }
            return new ResultData("Depression", "Eleve", "#ef4444",
                    "Le score est important. Un accompagnement professionnel est recommande.");
        }

        if ("anxiete".equals(cle)) {
            if (pourcentage < 0.34) {
                return new ResultData("Anxiete", "Faible", "#22c55e",
                        "Le niveau d'anxiete observe reste faible.");
            }
            if (pourcentage < 0.67) {
                return new ResultData("Anxiete", "Modere", "#f59e0b",
                        "Un niveau d'anxiete modere est detecte. Des exercices de respiration peuvent aider.");
            }
            return new ResultData("Anxiete", "Eleve", "#ef4444",
                    "Le niveau d'anxiete semble eleve. Un suivi plus attentif est conseille.");
        }

        if (pourcentage < 0.34) {
            return new ResultData("Stress", "Faible", "#22c55e",
                    "Votre niveau de stress parait faible. Continuez vos habitudes positives.");
        }
        if (pourcentage < 0.67) {
            return new ResultData("Stress", "Modere", "#f59e0b",
                    "Votre score montre un stress modere. Pensez a integrer des temps de recuperation.");
        }
        return new ResultData("Stress", "Eleve", "#ef4444",
                "Le score indique un stress eleve. Priorisez le repos et un accompagnement adapte si besoin.");
    }

    private String formatCategorie(String categorie) {
        return switch (normaliserCategorie(categorie)) {
            case "stress" -> "Stress";
            case "depression" -> "Depression";
            case "anxiete" -> "Anxiete";
            case "iq" -> "IQ";
            default -> categorie == null || categorie.isBlank() ? "Test" : categorie;
        };
    }

    private String normaliserCategorie(String valeur) {
        if (valeur == null) {
            return "";
        }

        return valeur.toLowerCase(Locale.ROOT).trim()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ù", "u")
                .replace("ï", "i")
                .replace("î", "i")
                .replace("ç", "c");
    }

    private String rgba(Color color, double alpha) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgba(" + red + "," + green + "," + blue + "," + alpha + ")";
    }

    private void changerScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            changerScene(root, title);
        } catch (IOException e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }

    private void changerScene(Parent root, String title) {
        Stage stage = (Stage) lblScore.getScene().getWindow();
        Scene currentScene = lblScore.getScene();
        Scene scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
    }

    private static final class ResultData {
        private final String categorieLabel;
        private final String niveau;
        private final String couleur;
        private final String description;

        private ResultData(String categorieLabel, String niveau, String couleur, String description) {
            this.categorieLabel = categorieLabel;
            this.niveau = niveau;
            this.couleur = couleur;
            this.description = description;
        }
    }
}
