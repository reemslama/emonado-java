package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.entities.User;
import java.io.IOException;

public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    private User currentUser;

    public void setUserData(User user) {
        if (user == null) return;
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
    }

    @FXML
    private void goToProfil() {
        if (this.currentUser == null) {
            System.err.println("Erreur: currentUser est NULL. L'injection a échoué.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil_patient.fxml"));
            Parent profilView = loader.load();

            ProfilPatientController controller = loader.getController();
            controller.setUserData(this.currentUser);

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(profilView);
            } else {
                welcomeLabel.getScene().setRoot(profilView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/ChoixCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}