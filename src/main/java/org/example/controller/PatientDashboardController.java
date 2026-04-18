package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.utils.UserSession;

import java.io.IOException;

public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private VBox espaceEnfantBtn;
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

        if (espaceEnfantBtn != null && user.isHasChild()) {
            espaceEnfantBtn.setVisible(true);
            espaceEnfantBtn.setManaged(true);
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
        } catch (IOException e) {
            System.err.println("Erreur chargement test : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRendezVous() {
        loadView("/AjouterRendezVous.fxml", "Rendez-vous");
    }

    @FXML
    private void goToMedicalRecord() {
        loadView("/medical_record.fxml", "Dossier medical");
    }

    @FXML
    private void goToConsultations() {
        loadView("/consultations.fxml", "Consultations");
    }

    @FXML
    private void openEspaceEnfant() {
        loadView("/EspaceEnfant.fxml", "Espace Enfant");
    }

    private void loadView(String fxmlPath, String viewName) {
        if (this.currentUser == null) {
            System.err.println("Erreur: User est NULL.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass()
                            .getMethod("setUserData", User.class)
                            .invoke(controller, this.currentUser);
                } catch (NoSuchMethodException e) {
                    System.out.println("Note: " + viewName + " ne necessite pas setUserData.");
                } catch (Exception e) {
                    System.err.println("Erreur injection user dans " + viewName + " : " + e.getMessage());
                }
            }

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(view);
            } else {
                welcomeLabel.getScene().setRoot(view);
            }

        } catch (IOException e) {
            System.err.println("Erreur chargement vue [" + viewName + "] : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
