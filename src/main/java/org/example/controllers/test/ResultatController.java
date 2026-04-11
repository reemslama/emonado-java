package org.example.controllers.test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ResultatController {

    @FXML private Label lblCategorie;
    @FXML private Label lblScore;
    @FXML private Label lblNiveau;
    @FXML private Label lblDescription;

    private String categorie;

    public void setResultat(int score, String categorie, int nbQuestions) {
        this.categorie = categorie;
        int scoreMax = nbQuestions * 3;

        lblCategorie.setText("Categorie : " + categorie.toUpperCase());
        lblScore.setText(score + " / " + scoreMax);

        String[] resultat = interpreter(score, scoreMax, categorie);
        lblNiveau.setText(resultat[0]);
        lblNiveau.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + resultat[1] + ";");
        lblDescription.setText(resultat[2]);
    }

    private String[] interpreter(int score, int scoreMax, String categorie) {
        double pourcentage = (double) score / scoreMax * 100;

        switch (categorie) {
            case "stress":
                if (pourcentage <= 33) return new String[]{"Stress Faible", "#27ae60", "Votre niveau de stress est faible. Continuez a prendre soin de vous."};
                else if (pourcentage <= 66) return new String[]{"Stress Modere", "#f39c12", "Vous presentez un stress modere. Pensez a pratiquer des activites relaxantes."};
                else return new String[]{"Stress Eleve", "#e74c3c", "Votre niveau de stress est eleve. Il est conseille de consulter un professionnel."};
            case "depression":
                if (pourcentage <= 33) return new String[]{"Etat Positif", "#27ae60", "Votre etat mental semble bon. Continuez a maintenir un mode de vie sain."};
                else if (pourcentage <= 66) return new String[]{"Signes Moderes", "#f39c12", "Vous presentez quelques signes. Parlez-en a un proche ou un professionnel."};
                else return new String[]{"Signes Importants", "#e74c3c", "Des signes importants sont detectes. Consultez un professionnel de sante."};
            case "iq":
                if (pourcentage <= 33) return new String[]{"Score Faible", "#e74c3c", "Continuez a vous exercer, la pratique ameliore les capacites cognitives."};
                else if (pourcentage <= 66) return new String[]{"Score Moyen", "#f39c12", "Bon resultat. Vous avez de bonnes capacites de raisonnement logique."};
                else return new String[]{"Excellent Score", "#27ae60", "Excellent. Vous avez de tres bonnes capacites cognitives et logiques."};
            default:
                return new String[]{"Resultat", "#2980b9", ""};
        }
    }

    @FXML
    private void refaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();
            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            Stage stage = (Stage) lblScore.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void accueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/ChoixCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblScore.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void retourDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblScore.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Espace Patient");
        } catch (Exception e) {
            System.out.println("Erreur retour : " + e.getMessage());
        }
    }
}
