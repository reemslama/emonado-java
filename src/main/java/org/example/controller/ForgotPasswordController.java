package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.service.PasswordResetService;

import java.io.IOException;
import java.util.regex.Pattern;

public class ForgotPasswordController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @FXML private TextField emailField;
    @FXML private Label feedbackLabel;

    @FXML
    private void sendCode() {
        String email = emailField.getText().trim();
        feedbackLabel.setStyle("-fx-text-fill: #dc2626;");

        if (email.isEmpty()) {
            feedbackLabel.setText("Veuillez saisir votre email.");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            feedbackLabel.setText("Le format de l'email est incorrect.");
            return;
        }

        try {
            PasswordResetService.getInstance().prepareReset(email);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password_code.fxml"));
            Parent root = loader.load();
            ForgotPasswordCodeController controller = loader.getController();
            controller.setEmail(email);
            emailField.getScene().setRoot(root);
        } catch (RuntimeException e) {
            feedbackLabel.setText(e.getMessage());
        } catch (IOException e) {
            feedbackLabel.setText("Impossible de charger l'interface de verification.");
        }
    }

    @FXML
    private void backToLogin() {
        loadView("/login.fxml");
    }

    public void setEmail(String email) {
        if (emailField != null) {
            emailField.setText(email);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            emailField.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger l'interface : " + e.getMessage(), e);
        }
    }
}
