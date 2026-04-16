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

    // --- NAVIGATION EXISTANTE ---

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
        // Ta logique spécifique pour le test (changement de Scene complète)
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

    // --- NOUVELLES MÉTHODES POUR LES ICÔNES AJOUTÉES ---

    @FXML
    private void goToRendezVous() {
        loadView("/AjouterRendezVous.fxml", "Rendez-vous");
    }


    /**
     * Méthode utilitaire pour charger une vue dans le centre du BorderPane principal
     */
    private void loadView(String fxmlPath, String viewName) {
        if (this.currentUser == null) {
            System.err.println("Erreur: User est NULL.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Tenter de passer les données utilisateur si le contrôleur le supporte
            Object controller = loader.getController();
            // On vérifie si le contrôleur a une méthode setUserData (via réflexion ou interface si tu en as une)
            try {
                controller.getClass().getMethod("setUserData", User.class).invoke(controller, this.currentUser);
            } catch (Exception e) {
                System.out.println("Note: Le contrôleur " + viewName + " ne nécessite pas de setUserData.");
            }

            BorderPane mainContainer = (BorderPane) welcomeLabel.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(view);
            } else {
                welcomeLabel.getScene().setRoot(view);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue " + viewName);
            e.printStackTrace();
        }
    }
}