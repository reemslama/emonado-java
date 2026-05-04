package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ChoixCategorieController {

    @FXML
    private void choisirTestIA(MouseEvent event) {
        // Pour l'IA, on va d'abord charger une catégorie par défaut, puis activer l'IA
        naviguerVersTestAvecIA("stress"); // On commence par stress, l'utilisateur pourra changer
    }

    @FXML
    private void choisirStress(MouseEvent event) {
        naviguerVersTest("stress");
    }

    @FXML
    private void choisirDepression(MouseEvent event) {
        naviguerVersTest("depression");
    }

    @FXML
    private void choisirAnxiete(MouseEvent event) {
        naviguerVersTest("anxiete");
    }

    @FXML
    private void choisirIQ(MouseEvent event) {
        naviguerVersTest("iq");
    }

    /**
     * Navigation vers test classique (sans IA)
     */
    private void naviguerVersTest(String categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);
            // Pas d'activation IA, mode classique par défaut

            Stage stage = (Stage) Stage.getWindows().filtered(w -> w.isFocused()).get(0);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigation vers test avec IA activée
     */
    private void naviguerVersTestAvecIA(String categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/PasserTest.fxml"));
            Parent root = loader.load();

            PasserTestController controller = loader.getController();
            controller.setCategorie(categorie);

            // Appeler directement la méthode d'activation IA après un petit délai
            // pour laisser le temps à l'interface de se charger
            javafx.application.Platform.runLater(() -> {
                controller.activerTestAdaptatifIA();
            });

            Stage stage = (Stage) Stage.getWindows().filtered(w -> w.isFocused()).get(0);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/patient_dashboard.fxml"));
            Stage stage = (Stage) Stage.getWindows().filtered(w -> w.isFocused()).get(0);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}