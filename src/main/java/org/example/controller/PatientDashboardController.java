package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.utils.UserSession;

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
        loadView("/profil_patient.fxml", "Profil");
    }

    @FXML
    private void goToJournaux() {
        loadView("/journal.fxml", "Journal");
    }

    @FXML
    private void goToTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/ChoixCategorie.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRendezVous() {
        loadView("/AjouterRendezVous.fxml", "Rendez-vous");
    }

    @FXML
    private void goToMedicalRecord() {
        loadView("/medical_record.fxml", "Dossier Medical");
    }

    @FXML
    private void goToConsultations() {
        loadView("/consultations.fxml", "Consultations");
    }

    private void loadView(String fxmlPath, String viewName) {
        if (currentUser == null) {
            System.err.println("Erreur: utilisateur introuvable pour " + viewName);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            injectUserData(loader.getController());

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(view);
            } else {
                welcomeLabel.getScene().setRoot(view);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la vue " + viewName);
            e.printStackTrace();
        }
    }

    private void injectUserData(Object controller) {
        if (controller instanceof ProfilPatientController profilPatientController) {
            profilPatientController.setUserData(currentUser);
        } else if (controller instanceof JournalController journalController) {
            journalController.setUserData(currentUser);
        } else if (controller instanceof MedicalRecordController medicalRecordController) {
            medicalRecordController.setUserData(currentUser);
        } else if (controller instanceof ConsultationController consultationController) {
            consultationController.setUserData(currentUser);
        }
    }
}
