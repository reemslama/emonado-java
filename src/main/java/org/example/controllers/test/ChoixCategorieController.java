package org.example.controllers.test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class ChoixCategorieController {

    @FXML private Button btnStress;
    @FXML private Button btnDepression;
    @FXML private Button btnAnxiete;
    @FXML private Button btnIQ;

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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/test/PasserTest.fxml")
            );
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            Stage stage = (Stage) btnStress.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Test - " + categorie);
        } catch (Exception e) {
            System.out.println("Erreur navigation : " + e.getMessage());
        }
    }
}