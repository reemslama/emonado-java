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

    // Redirige vers la page d'accueil (recharge la vue initiale)
    @FXML
    private void goToHome(ActionEvent event) {
        try {
            // On recharge le contenu initial du main.fxml
            // Note: Assure-toi que le nom du fichier correspond à ton fichier FXML principal
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/main.fxml")));
            // On remplace toute la scène par la racine pour réinitialiser l'état
            mainContainer.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur retour accueil : " + e.getMessage());
        }
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            Parent registerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/register.fxml")));
            mainContainer.setCenter(registerView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin() {
        try {
            Parent loginView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/login.fxml")));
            mainContainer.setCenter(loginView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}