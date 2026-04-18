package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.example.entities.User;
import org.example.utils.DataSource;
import org.example.utils.UserSession;
import java.io.IOException;
import java.sql.*;

public class ProfilPsyController {

    @FXML private TextField nomField, prenomField, emailField, phoneField, specialiteField;
    @FXML private PasswordField passwordField;
    @FXML private Label titleLabel;

    private User currentUser;

    @FXML
    public void initialize() {
        // Tentative de récupération depuis la session globale
        this.currentUser = UserSession.getInstance();

        if (this.currentUser != null) {
            displayUserData();
        } else {
            System.err.println("🚨 Erreur : currentUser est null dans initialize. Vérifiez le Login.");
        }
    }

    private void displayUserData() {
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getTelephone());
        specialiteField.setText(currentUser.getSpecialite());

        if (titleLabel != null) {
            titleLabel.setText("Profil de Dr. " + currentUser.getPrenom());
        }
    }

    public void setUserData(User user) {
        this.currentUser = user;
        if (nomField != null) displayUserData();
    }

    @FXML
    private void handleUpdate() {
        if (currentUser == null) return;

        String newPass = passwordField.getText();
        boolean updatePass = newPass != null && !newPass.trim().isEmpty();

        String query = "UPDATE user SET nom=?, prenom=?, telephone=?, specialite=?" +
                (updatePass ? ", password=?" : "") + " WHERE id=?";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, nomField.getText());
            pstmt.setString(2, prenomField.getText());
            pstmt.setString(3, phoneField.getText());
            pstmt.setString(4, specialiteField.getText());

            if (updatePass) {
                pstmt.setString(5, newPass);
                pstmt.setInt(6, currentUser.getId());
            } else {
                pstmt.setInt(5, currentUser.getId());
            }

            pstmt.executeUpdate();

            // Mise à jour de l'objet en session
            currentUser.setNom(nomField.getText());
            currentUser.setPrenom(prenomField.getText());

            new Alert(Alert.AlertType.INFORMATION, "Profil mis à jour !").show();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void returnToDashboard() {
        if (currentUser == null) {
            handleLogout();
            return;
        }
        try {
            String role = currentUser.getRole();
            String fxmlPath = (role != null && role.equals("ROLE_PSYCHOLOGUE")) ? "/psy_dashboard.fxml" : "/admin_dashboard.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            nomField.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce compte ?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE id=?")) {
                pstmt.setInt(1, currentUser.getId());
                pstmt.executeUpdate();
                handleLogout();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}