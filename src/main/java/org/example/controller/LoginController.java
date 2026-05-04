package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import org.example.service.AppNavigationService;
import org.example.entities.User;
import org.example.service.AuthService;
import org.example.service.QrLoginService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.util.regex.Pattern;

public class LoginController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ToggleButton rolePatient;
    @FXML private ToggleButton rolePsy;
    @FXML private ToggleButton roleAdmin;
    @FXML private Label errorLabel;

    private ToggleGroup roleGroup;

    @FXML
    public void initialize() {
        roleGroup = new ToggleGroup();
        rolePatient.setToggleGroup(roleGroup);
        rolePsy.setToggleGroup(roleGroup);
        roleAdmin.setToggleGroup(roleGroup);
        rolePatient.setSelected(true);
    }

    @FXML
    private void handleLogin() {
        errorLabel.setStyle("-fx-text-fill: red;");
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        ToggleButton selectedRole = (ToggleButton) roleGroup.getSelectedToggle();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez saisir votre email et mot de passe.");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Le format de l'email est incorrect.");
            return;
        }

        if (selectedRole == null) {
            rolePatient.setSelected(true);
            selectedRole = rolePatient;
        }

        User user = AuthService.authenticate(email, password);

        if (user == null) {
            errorLabel.setText("Email ou mot de passe incorrect.");
            return;
        }

        String roleAttendu = resolveExpectedRole(selectedRole);
        String roleTrouve = normalizeRole(user.getRole());

        if (!roleTrouve.equalsIgnoreCase(roleAttendu)) {
            errorLabel.setText("Acces refuse : role du compte = " + displayRole(roleTrouve)
                    + " (vous avez choisi " + displayRole(roleAttendu) + ").");
            return;
        }

        try {
            QrLoginService.QrChallenge challenge = QrLoginService.getInstance().createChallenge(user);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qr_login.fxml"));
            Parent view = loader.load();
            QrLoginController controller = loader.getController();
            controller.setChallenge(challenge);
            emailField.getScene().setRoot(view);
        } catch (RuntimeException | IOException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password.fxml"));
            Parent view = loader.load();
            ForgotPasswordController controller = loader.getController();
            controller.setEmail(emailField.getText().trim());
            emailField.getScene().setRoot(view);
        } catch (IOException e) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Impossible d'ouvrir l'interface de reinitialisation.");
        }
    }

    private String resolveExpectedRole(ToggleButton selectedRole) {
        if (selectedRole == rolePsy) {
            return "ROLE_PSYCHOLOGUE";
        }
        if (selectedRole == roleAdmin) {
            return "ROLE_ADMIN";
        }
        return "ROLE_PATIENT";
    }

    private String normalizeRole(String role) {
        return AppNavigationService.normalizeRole(role);
    }

    private String displayRole(String normalizedRole) {
        if (normalizedRole == null) {
            return "Patient";
        }
        return switch (normalizedRole.toUpperCase()) {
            case "ROLE_ADMIN" -> "Admin";
            case "ROLE_PSYCHOLOGUE" -> "Psychologue";
            default -> "Patient";
        };
    }

    @FXML
    private void goToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent view = loader.load();
            emailField.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prefillEmail(String email) {
        if (emailField != null) {
            emailField.setText(email);
            passwordField.clear();
            UserSession.clear();
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Pret pour la connexion.");
        }
    }
}
