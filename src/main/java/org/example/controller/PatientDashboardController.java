package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
<<<<<<< HEAD
=======
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44
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
<<<<<<< HEAD
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
    private void handleLogout() {
        UserSession.setInstance(null);
        openSimpleView("/login.fxml");
    }

    private void openView(String fxmlPath) {
        if (currentUser == null) {
            System.err.println("Erreur: currentUser est NULL.");
=======
        if (this.currentUser == null) {
            System.err.println("Erreur: currentUser est NULL. L'injection a echoue.");
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

<<<<<<< HEAD
            Object controller = loader.getController();
            if (controller instanceof ProfilPatientController profilController) {
                profilController.setUserData(currentUser);
=======
            ProfilPatientController controller = loader.getController();
            controller.setUserData(this.currentUser);

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(profilView);
            } else {
                welcomeLabel.getScene().setRoot(profilView);
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44
            }
            if (controller instanceof MedicalRecordController medicalRecordController) {
                medicalRecordController.setUserData(currentUser);
            }
            if (controller instanceof ConsultationController consultationController) {
                consultationController.setUserData(currentUser);
            }

            welcomeLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

<<<<<<< HEAD
    private void openSimpleView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            welcomeLabel.getScene().setRoot(view);
=======
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

    @FXML
    private void goToTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/ChoixCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
