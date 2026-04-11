package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Logique de tes amis (EmoNado)
        // Note : Pour tester TON interface (AjouterDisponibilite.fxml),
        // il te suffira de changer "/main.fxml" par "/AjouterDisponibilite.fxml" ici plus tard.

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 600);

        // Ajout du CSS de l'équipe
        if (getClass().getResource("/styles/template.css") != null) {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/template.css")).toExternalForm());
        }

        stage.setTitle("EmoNado");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}