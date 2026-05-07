package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.entities.User;
import org.example.service.PasswordHashService;
import org.example.utils.DataSource;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ProfilPsyController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField specialiteField;
    @FXML private PasswordField passwordField;
    @FXML private Label titleLabel;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance();

        if (currentUser != null) {
            displayUserData();
        } else {
            System.err.println("Erreur : currentUser est null dans initialize. Verifiez le login.");
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
        currentUser = user;
        if (nomField != null) {
            displayUserData();
        }
    }

    @FXML
    private void handleUpdate() {
        if (currentUser == null) {
            return;
        }

        String newPassword = passwordField.getText();
        boolean updatePassword = newPassword != null && !newPassword.trim().isEmpty();

        String query = "UPDATE user SET nom=?, prenom=?, email=?, telephone=?, specialite=?"
                + (updatePassword ? ", password=?" : "")
                + " WHERE id=?";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, nomField.getText().trim());
            pstmt.setString(2, prenomField.getText().trim());
            pstmt.setString(3, emailField.getText().trim());
            pstmt.setString(4, phoneField.getText().trim());
            pstmt.setString(5, specialiteField.getText().trim());

            int parameterIndex = 6;
            if (updatePassword) {
                String hashedPassword = PasswordHashService.ensureHashed(newPassword);
                pstmt.setString(parameterIndex++, hashedPassword);
                currentUser.setPassword(hashedPassword);
            }
            pstmt.setInt(parameterIndex, currentUser.getId());

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows == 0) {
                new Alert(Alert.AlertType.WARNING, "Aucune modification en base.").show();
                return;
            }

            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setTelephone(phoneField.getText().trim());
            currentUser.setSpecialite(specialiteField.getText().trim());
            UserSession.setInstance(currentUser);
            passwordField.clear();

            new Alert(Alert.AlertType.INFORMATION, "Profil mis a jour !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            String message = e.getMessage() != null && e.getMessage().contains("Duplicate entry")
                    ? "Cet email existe deja."
                    : "Erreur lors de la mise a jour du profil.";
            new Alert(Alert.AlertType.ERROR, message).show();
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
