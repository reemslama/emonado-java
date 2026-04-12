package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.entities.User;
import org.example.service.AuthService;
import org.example.utils.UserSession;
import java.io.IOException;
import java.util.regex.Pattern;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ToggleButton rolePatient, rolePsy, roleAdmin;
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
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        ToggleButton selectedRole = (ToggleButton) roleGroup.getSelectedToggle();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez saisir votre email et mot de passe.");
            return;
        }

        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            errorLabel.setText("Le format de l'email est incorrect.");
            return;
        }

        if (selectedRole == null) {
            errorLabel.setText("Veuillez sélectionner un rôle.");
            return;
        }

        User user = AuthService.authenticate(email, password);

        if (user != null) {
            String roleAttendu = switch (selectedRole.getText().toUpperCase()) {
                case "PSYCHOLOGUE" -> "ROLE_PSYCHOLOGUE";
                case "ADMIN" -> "ROLE_ADMIN";
                default -> "ROLE_PATIENT";
            };

            if (user.getRole().equalsIgnoreCase(roleAttendu)) {
                UserSession.setInstance(user);
                redirectUser(user);
            } else {
                errorLabel.setText("Accès refusé : Ce compte n'a pas le rôle " + selectedRole.getText());
            }
        } else {
            errorLabel.setText("Email ou mot de passe incorrect.");
        }
    }

    private void redirectUser(User user) {
        try {
            String fxmlPath = switch (user.getRole().toUpperCase()) {
                case "ROLE_ADMIN" -> "/admin_dashboard.fxml";
                case "ROLE_PSYCHOLOGUE" -> "/psy_dashboard.fxml";
                default -> "/patient_dashboard.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            Object controller = loader.getController();

            if (controller != null) {
                if (controller instanceof PatientDashboardController patCtrl) {
                    patCtrl.setUserData(user);
                } else if (controller instanceof PsyDashboardController psyCtrl) {
                    psyCtrl.setUserData(user);
                    System.out.println("Injection réussie pour le Psychologue !");
                } else if (controller instanceof AdminDashboardController adminCtrl) {
                    adminCtrl.setUserData(user);
                }
            }

            emailField.getScene().setRoot(view);

        } catch (IOException e) {
            System.err.println("Erreur de chargement du dashboard : " + e.getMessage());
            e.printStackTrace();
        }
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
}