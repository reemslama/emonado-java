package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.service.PasswordResetService;

import java.io.IOException;

public class ForgotPasswordCodeController {
    @FXML private Label emailInfoLabel;
    @FXML private TextField codeField;
    @FXML private Label feedbackLabel;

    private String email;

    public void setEmail(String email) {
        this.email = email;
        if (emailInfoLabel != null) {
            emailInfoLabel.setText("Code envoye a " + email);
        }
    }

    @FXML
    private void verifyCode() {
        String code = codeField.getText().trim();
        feedbackLabel.setStyle("-fx-text-fill: #dc2626;");

        if (code.isEmpty()) {
            feedbackLabel.setText("Veuillez saisir le code.");
            return;
        }

        if (!PasswordResetService.getInstance().verifyCode(email, code)) {
            feedbackLabel.setText("Le code est incorrect.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reset_password.fxml"));
            Parent root = loader.load();
            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);
            codeField.getScene().setRoot(root);
        } catch (IOException e) {
            feedbackLabel.setText("Impossible de charger l'interface du nouveau mot de passe.");
        }
    }

    @FXML
    private void back() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password.fxml"));
            Parent root = loader.load();
            ForgotPasswordController controller = loader.getController();
            controller.setEmail(email);
            codeField.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger l'interface : " + e.getMessage(), e);
        }
    }
}
