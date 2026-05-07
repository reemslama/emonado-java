package org.example.controllers.test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ChoixCategorieController {

    @FXML
    private VBox rootBox;

    @FXML
    private void choisirStress() {
        ouvrirTest("stress");
    }

    @FXML
    private void choisirDepression() {
        ouvrirTest("depression");
    }

    @FXML
    private void choisirAnxiete() {
        ouvrirTest("anxiete");
    }

    @FXML
    private void choisirIQ() {
        ouvrirTest("iq");
    }

    @FXML
    private void retour() {
        changerScene("/patient_dashboard.fxml", "Espace Patient");
    }

    private void ouvrirTest(String categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            changerScene(root, "Test - " + categorie.toUpperCase());
        } catch (IOException e) {
            System.err.println("Erreur chargement test : " + e.getMessage());
        }
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
        Stage stage = (Stage) rootBox.getScene().getWindow();
        Scene currentScene = rootBox.getScene();
        Scene scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
    }
}
