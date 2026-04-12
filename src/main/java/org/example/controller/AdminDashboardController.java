package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.*;
import org.example.entities.User;
import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label headerTitle;
    @FXML private Label adminEmailLabel;

    private User currentAdmin;

    public void setUserData(User user) {
        this.currentAdmin = user;
        if (adminEmailLabel != null) {
            adminEmailLabel.setText(user.getEmail());
        }
    }

    @FXML
    private void showHome() {
        headerTitle.setText("Dashboard");
        contentArea.getChildren().setAll(
                new Label("Bienvenue dans le panneau d'administration")
        );
    }

    @FXML
    private void showPsychologues() {
        loadTable("ROLE_PSYCHOLOGUE", "Gestion des Psychologues");
    }

    @FXML
    private void showPatients() {
        loadTable("ROLE_PATIENT", "Gestion des Patients");
    }

    @FXML
    private void showQuestionsReponses() {
        try {
            headerTitle.setText("Questions / Réponses");
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/test/QuestionsReponses.fxml")
            );
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTable(String role, String title) {
        try {
            headerTitle.setText(title);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/admin_users_table.fxml")
            );
            Parent tableRoot = loader.load();

            AdminTableController controller = loader.getController();
            controller.loadData(role);

            contentArea.getChildren().setAll(tableRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}