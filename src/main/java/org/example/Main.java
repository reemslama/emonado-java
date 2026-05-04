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
        Scene scene = new Scene(loader.load(), 900, 600);

        // Ajouter CSS
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/template.css")).toExternalForm());

        stage.setTitle("EmoNado");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(); // lance JavaFX
    }
}
