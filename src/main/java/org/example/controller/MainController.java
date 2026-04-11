package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.util.Objects;

public class MainController {

    @FXML
    private BorderPane mainContainer;

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            // Charge le formulaire d'inscription dans le centre de la vue actuelle
            Parent registerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/register.fxml")));
            mainContainer.setCenter(registerView);
        } catch (IOException e) {
            System.err.println("Erreur de chargement register.fxml : " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void goToLogin() throws Exception {
        Parent loginView = FXMLLoader.load(getClass().getResource("/login.fxml"));
        mainContainer.setCenter(loginView);
    }
}