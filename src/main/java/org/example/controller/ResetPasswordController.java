package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.example.service.PasswordResetService;

import java.io.IOException;
import java.util.regex.Pattern;

public class ResetPasswordController {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$");

    @FXML private Label emailInfoLabel;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label feedbackLabel;

    private String email;

    public void setEmail(String email) {
        this.email = email;
        if (emailInfoLabel != null) {
            emailInfoLabel.setText("Nouveau mot de passe pour " + email);
        }
    }

    @FXML
    private void savePassword() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        feedbackLabel.setStyle("-fx-text-fill: #dc2626;");

        if (password.isBlank() || confirmPassword.isBlank()) {
            feedbackLabel.setText("Veuillez remplir les deux champs.");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            feedbackLabel.setText("8 caracteres min, 1 majuscule, 1 chiffre et 1 caractere special.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            feedbackLabel.setText("La confirmation du mot de passe ne correspond pas.");
            return;
        }

        try {
            boolean updated = PasswordResetService.getInstance().resetPassword(email, password);
            if (!updated) {
                feedbackLabel.setText("Impossible de mettre a jour le mot de passe.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.prefillEmail(email);
            passwordField.getScene().setRoot(root);
        } catch (RuntimeException e) {
            feedbackLabel.setText(e.getMessage());
        } catch (IOException e) {
            feedbackLabel.setText("Impossible de revenir a l'interface de connexion.");
        }
    }

    @FXML
    private void back() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password_code.fxml"));
            Parent root = loader.load();
            ForgotPasswordCodeController controller = loader.getController();
            controller.setEmail(email);
            passwordField.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger l'interface : " + e.getMessage(), e);
        }
    }
}
