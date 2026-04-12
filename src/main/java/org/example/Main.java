package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Scene scene = new Scene(loader.load());

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/template.css")).toExternalForm());

        stage.setTitle("EmoNado");
        stage.setScene(scene);
        stage.setMaximized(true);        // ← démarre maximisé
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}