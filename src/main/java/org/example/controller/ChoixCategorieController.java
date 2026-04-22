package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChoixCategorieController {

    @FXML
    private VBox rootBox;

    // ===== Constantes catégories =====
    private static final String STRESS = "stress";
    private static final String DEPRESSION = "depression";
    private static final String ANXIETE = "anxiete";
    private static final String IQ = "iq";

    // ===== Choix des tests =====
    @FXML
    private void choisirStress() {
        ouvrirTest(STRESS);
    }

    @FXML
    private void choisirDepression() {
        ouvrirTest(DEPRESSION);
    }

    @FXML
    private void choisirAnxiete() {
        ouvrirTest(ANXIETE);
    }

    @FXML
    private void choisirIQ() {
        ouvrirTest(IQ);
    }

    // ===== Navigation vers le test =====
    private void ouvrirTest(String categorie) {
        try {
            var url = getClass().getResource("/fxml/test/PasserTest.fxml");

            if (url == null) {
                throw new RuntimeException("FXML introuvable : PasserTest.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            changerScene(root, "Test - " + categorie);

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture du test");
            e.printStackTrace();
        }
    }

    // ===== Retour dashboard =====
    @FXML
    private void retour() {
        try {
            var url = getClass().getResource("/fxml/patient_dashboard.fxml");

            if (url == null) {
                throw new RuntimeException("FXML introuvable : patient_dashboard.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            changerScene(root, "Espace Patient");

        } catch (Exception e) {
            System.err.println("Erreur retour dashboard");
            e.printStackTrace();
        }
    }

    // ===== Méthode centralisée de changement de scène =====
    private void changerScene(Parent root, String titre) {
        Stage stage = (Stage) rootBox.getScene().getWindow();

        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        stage.setScene(scene);

        stage.setTitle(titre);
        stage.setMaximized(true);
    }
}