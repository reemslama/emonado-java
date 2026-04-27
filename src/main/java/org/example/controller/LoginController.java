package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.entities.User;
import org.example.service.AuthService;
import org.example.utils.UserSession; // Import ajouté
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

        // --- CONTRÔLE DE SAISIE (Inchangé) ---
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez saisir votre email et mot de passe.");
            return;
        }

        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            errorLabel.setText("Le format de l'email est incorrect.");
            return;
        }
        // -------------------------------------

        User user = AuthService.authenticate(email, password);

        if (user != null) {
            String roleAttendu = resolveExpectedRole(selectedRole);
            String roleTrouve = normalizeRole(user.getRole());

            if (roleTrouve.equalsIgnoreCase(roleAttendu)) {
                // 1. ON REMPLIT LA SESSION STATIQUE (Crucial pour éviter le NULL plus tard)
                UserSession.setInstance(user);

                // 2. ON REDIRIGE
                redirectUser(user);
            } else {
                errorLabel.setText("Accès refusé : rôle du compte = " + displayRole(roleTrouve)
                        + " (vous avez choisi " + displayRole(roleAttendu) + ").");
            }
        } else {
            errorLabel.setText("Email ou mot de passe incorrect.");
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
        if (role == null) {
            return "ROLE_PATIENT";
        }
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "ROLE_ADMIN", "ADMIN" -> "ROLE_ADMIN";
            case "ROLE_PSYCHOLOGUE", "PSYCHOLOGUE", "PSY", "ROLE_PSY" -> "ROLE_PSYCHOLOGUE";
            case "ROLE_PATIENT", "PATIENT", "USER" -> "ROLE_PATIENT";
            default -> r.startsWith("ROLE_") ? r : "ROLE_" + r;
        };
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

    private void redirectUser(User user) {
        try {
            String fxmlPath;
            String role = normalizeRole(user.getRole());
            if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
                fxmlPath = "/admin_dashboard.fxml";
            } else if ("ROLE_PSYCHOLOGUE".equalsIgnoreCase(role)) {
                fxmlPath = "/psy_dashboard.fxml";
            } else if (user.isHasChild()) {
                fxmlPath = "/EspaceEnfant.fxml";
            } else {
                fxmlPath = "/patient_dashboard.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            Object controller = loader.getController();

            if (controller != null) {
                // INJECTION DE L'USER SELON LE TYPE DE CONTROLEUR
                if (controller instanceof PatientDashboardController patCtrl) {
                    patCtrl.setUserData(user);
                }
                else if (controller instanceof PsyDashboardController psyCtrl) {
                    psyCtrl.setUserData(user); // Ajout pour le Psychologue
                    System.out.println("Injection réussie pour le Psychologue !");
                }
                else if (controller instanceof AdminDashboardController adminCtrl) {
                    adminCtrl.setUserData(user);
                }
            }

            // CHANGEMENT DE VUE
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
