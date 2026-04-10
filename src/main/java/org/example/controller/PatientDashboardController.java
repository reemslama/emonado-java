package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.example.entities.User;
import org.example.utils.UserSession;

import java.io.IOException;

public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    private User currentUser;

    @FXML
    public void initialize() {
        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) return;
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
        }
    }

    @FXML
    private void goToProfil() {
        if (this.currentUser == null) {
            System.err.println("Erreur: currentUser est NULL. L'injection a echoue.");
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
    private void goToJournaux() {
        if (this.currentUser == null) {
            System.err.println("Erreur: currentUser est NULL. L'injection a echoue.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/journal.fxml"));
            Parent journalView = loader.load();

            JournalController controller = loader.getController();
            controller.setUserData(this.currentUser);

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(journalView);
            } else {
                welcomeLabel.getScene().setRoot(journalView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
