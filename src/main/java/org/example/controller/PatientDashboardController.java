package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
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
        if (user == null) {
            return;
        }
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
        }
    }

    @FXML
    private void goToProfil() {
        openView("/profil_patient.fxml");
    }

    @FXML
    private void goToMedicalRecord() {
        openView("/medical_record.fxml");
    }

    @FXML
    private void goToConsultations() {
        openView("/consultations.fxml");
    }

    @FXML
    private void goToJournaux() {
        openView("/journal.fxml");
    }

    @FXML
    private void goToTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/ChoixCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Espace Patient");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        UserSession.setInstance(null);
        openSimpleView("/login.fxml");
    }

    private void openView(String fxmlPath) {
        if (currentUser == null) {
            System.err.println("Erreur: currentUser est NULL.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ProfilPatientController profilController) {
                profilController.setUserData(currentUser);
            } else if (controller instanceof MedicalRecordController medicalRecordController) {
                medicalRecordController.setUserData(currentUser);
            } else if (controller instanceof ConsultationController consultationController) {
                consultationController.setUserData(currentUser);
            } else if (controller instanceof JournalController journalController) {
                journalController.setUserData(currentUser);
            }

            welcomeLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openSimpleView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            welcomeLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}