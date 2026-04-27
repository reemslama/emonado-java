package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.entities.User;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label headerTitle;
    @FXML private Label adminEmailLabel;

    private User currentAdmin;

    @FXML
    public void initialize() {
        if (headerTitle != null && contentArea != null) {
            showHome();
        }
    }

    public void setUserData(User user) {
        this.currentAdmin = user;
        if (adminEmailLabel != null && user != null) {
            adminEmailLabel.setText(user.getEmail());
        }
    }

    @FXML
    private void showHome() {
        headerTitle.setText("Dashboard");
        VBox box = new VBox(15);
        box.setStyle("-fx-alignment: center-left; -fx-padding: 20; -fx-background-color: white; -fx-background-radius: 10;");
        Label welcome = new Label("Bienvenue dans le panneau d'administration");
        welcome.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Label description = new Label("Accedez a la gestion medicale pour consulter, modifier et supprimer les dossiers, antecedents et consultations des patients.");
        description.setWrapText(true);
        Button medicalButton = new Button("Gestion medicale");
        medicalButton.setStyle("-fx-background-color: #198754; -fx-text-fill: white; -fx-font-weight: bold;");
        medicalButton.setOnAction(event -> showMedicalManagement());
        box.getChildren().addAll(welcome, description, medicalButton);
        contentArea.getChildren().setAll(box);
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
            headerTitle.setText("Questions / Reponses");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/QuestionsReponses.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showGameManagement() {
        try {
            headerTitle.setText("Gestion des jeux");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionJeux.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  NOUVEAU — Scénarios de tests psychologiques
    // ════════════════════════════════════════════════════════
    @FXML
    private void showScenariosTests() {
        try {
            headerTitle.setText("Scénarios Tests Psychologiques");
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/GestionJeux.fxml")
            );
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ════════════════════════════════════════════════════════

    private void loadTable(String role, String title) {
        try {
            headerTitle.setText(title);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin_users_table.fxml"));
            Parent tableRoot = loader.load();

            AdminTableController controller = loader.getController();
            controller.loadData(role);

            contentArea.getChildren().setAll(tableRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showMedicalManagement() {
        try {
            headerTitle.setText("Gestion medicale");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/medical_management.fxml"));
            Parent view = loader.load();
            MedicalManagementController controller = loader.getController();
            controller.initForAdmin(currentAdmin);
            contentArea.getChildren().setAll(view);
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