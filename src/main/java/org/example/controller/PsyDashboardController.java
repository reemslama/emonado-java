package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.example.entities.User;
import org.example.utils.UserSession;

import java.io.IOException;

public class PsyDashboardController {

    @FXML private Label welcomeLabel;
    private User currentUser;

    @FXML
    public void initialize() {
        if (this.currentUser == null) {
            this.currentUser = UserSession.getInstance();
        }

        if (this.currentUser != null) {
            welcomeLabel.setText("Bienvenue Dr. " + currentUser.getNom());
        }
    }

    public void setUserData(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Bienvenue Dr. " + user.getNom());
        }
    }

    @FXML
    private void goToProfil() {
        if (this.currentUser == null) {
            this.currentUser = UserSession.getInstance();
        }

        if (this.currentUser == null) {
            System.err.println("Impossible d'ouvrir le profil : aucun utilisateur en session.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil_psy.fxml"));
            Parent profilView = loader.load();

            ProfilPsyController controller = loader.getController();
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
    private void goToAnalyses() {
        if (this.currentUser == null) {
            this.currentUser = UserSession.getInstance();
        }
        if (this.currentUser == null) {
            System.err.println("Erreur: aucun psychologue en session.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/analyse_emotionnelle.fxml"));
            Parent analyseView = loader.load();

            AnalyseEmotionnelleController controller = loader.getController();
            controller.initForPsychologueView();

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(analyseView);
            } else {
                welcomeLabel.getScene().setRoot(analyseView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToListeRendezVous() {
        loadView("/ListeRendezVousPsy.fxml", "Liste des Rendez-vous");
    }

    @FXML
    private void goToDisponibilites() {
        loadView("/AjouterDisponibilite.fxml", "Gestion des Disponibilites");
    }

    @FXML
    private void goToTypeRdv() {
        loadView("/AjouterType.fxml", "Types de Rendez-vous");
    }

    @FXML
    private void goToMedicalManagement() {
        loadView("/medical_management.fxml", "Suivi medical");
    }

    @FXML
    private void openChat() {
        loadView("/chat_dashboard.fxml", "Messagerie");
    }

    private void loadView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(view);
            } else {
                welcomeLabel.getScene().setRoot(view);
            }
        } catch (IOException e) {
            System.err.println("Erreur chargement " + title + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
