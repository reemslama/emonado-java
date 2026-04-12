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

        lblCategorie.setText("Catégorie : " + categorie.toUpperCase());
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
                if (pourcentage <= 33) return new String[]{
                        "😊 Stress Faible", "#27ae60",
                        "Votre niveau de stress est faible. Continuez à prendre soin de vous !"
                };
                else if (pourcentage <= 66) return new String[]{
                        "😐 Stress Modéré", "#f39c12",
                        "Vous présentez un stress modéré. Pensez à pratiquer des activités relaxantes."
                };
                else return new String[]{
                            "😰 Stress Élevé", "#e74c3c",
                            "Votre niveau de stress est élevé. Il est conseillé de consulter un professionnel."
                    };

            case "depression":
                if (pourcentage <= 33) return new String[]{
                        "😊 État Positif", "#27ae60",
                        "Votre état mental semble bon. Continuez à maintenir un mode de vie sain !"
                };
                else if (pourcentage <= 66) return new String[]{
                        "😐 Signes Modérés", "#f39c12",
                        "Vous présentez quelques signes. Parlez-en à un proche ou un professionnel."
                };
                else return new String[]{
                            "😔 Signes Importants", "#e74c3c",
                            "Des signes importants sont détectés. Consultez un professionnel de santé."
                    };

            case "iq":
                if (pourcentage <= 33) return new String[]{
                        "🧩 Score Faible", "#e74c3c",
                        "Continuez à vous exercer, la pratique améliore les capacités cognitives !"
                };
                else if (pourcentage <= 66) return new String[]{
                        "🧠 Score Moyen", "#f39c12",
                        "Bon résultat ! Vous avez de bonnes capacités de raisonnement logique."
                };
                else return new String[]{
                            "🌟 Excellent Score", "#27ae60",
                            "Excellent ! Vous avez de très bonnes capacités cognitives et logiques."
                    };

            default:
                return new String[]{"Résultat", "#2980b9", ""};
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
}