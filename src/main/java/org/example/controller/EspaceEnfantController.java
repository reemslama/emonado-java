package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

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

    private void loadCenter(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur chargement vue enfant: " + e.getMessage());
        }
    }
}
