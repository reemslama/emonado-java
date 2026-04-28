package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.Main;

import java.io.IOException;
import java.net.URL;

public class MainController {

    @FXML
    private StackPane mainContent;
    // ✅ Acceuil
    @FXML
    private void goToHome(ActionEvent event) {
        URL url = Main.class.getResource("/main.fxml");
        if (url == null) {
            System.err.println("Ressource introuvable : /main.fxml");
            return;
        }
        try {
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) mainContent.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur chargement main.fxml");
            e.printStackTrace();
        }
    }

    // ✅ Register
    @FXML
    private void goToRegister(ActionEvent event) {
        loadPage("/register.fxml");
    }

    // ✅ Login
    @FXML
    private void goToLogin(ActionEvent event) {
        loadPage("/login.fxml");
    }

    // 🔥 Méthode centralisée
    private void loadPage(String path) {
        URL url = Main.class.getResource(path);
        if (url == null) {
            System.err.println("Ressource introuvable sur le classpath : " + path);
            return;
        }
        try {
            Parent view = FXMLLoader.load(url);
            mainContent.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Erreur chargement page : " + path);
            e.printStackTrace();
        }
    }
}