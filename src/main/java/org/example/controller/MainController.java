package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainController {

    @FXML
    private BorderPane mainContainer;
    // ✅ Acceuil
    @FXML
    private void goToHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/main.fxml"))
            );
            Stage stage = (Stage) mainContainer.getScene().getWindow();
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
        try {
            Parent view = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource(path))
            );
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur chargement page : " + path);
            e.printStackTrace();
        }
    }
}