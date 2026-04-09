package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // On charge le fichier FXML que tu viens de créer
        Parent root = FXMLLoader.load(getClass().getResource("/AjouterDisponibilite.fxml"));

        primaryStage.setTitle("Mon Application Psychologue");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}