package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class EspaceEnfantController {
    @FXML
    private BorderPane mainContainer;

    @FXML
    public void initialize() {
        openJeux();
    }

    @FXML
    private void openJeux() {
        loadCenter("/GestionJeux.fxml");
    }

    @FXML
    private void openParticipations() {
        loadCenter("/GestionParticipations.fxml");
    }

    @FXML
    private void openHeatmap() {
        loadCenter("/Heatmap.fxml");
    }

    @FXML
    private void goToDeconnexion() {
        try {
            // Clear user session
            org.example.utils.UserSession.clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Erreur lors de la deconnexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCenter(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur chargement vue enfant: " + e.getMessage());
        }
    }
}
