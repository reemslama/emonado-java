package org.example.controllers.test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChoixCategorieController {

    @FXML private VBox rootBox;

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

    private void ouvrirTest(String categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            Stage stage = (Stage) rootBox.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Test - " + categorie);
        } catch (Exception e) {
            System.out.println("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    private void retourDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootBox.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Espace Patient");
        } catch (Exception e) {
            System.out.println("Erreur retour : " + e.getMessage());
        }
    }
}
