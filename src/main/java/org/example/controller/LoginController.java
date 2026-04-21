package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import org.example.entities.User;
import org.example.service.AuthService;
import org.example.service.EmailService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$");

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ToggleButton rolePatient;
    @FXML private ToggleButton rolePsy;
    @FXML private ToggleButton roleAdmin;
    @FXML private Label errorLabel;
    private ToggleGroup roleGroup;

    private final EmailService emailService = new EmailService();
    private final SecureRandom secureRandom = new SecureRandom();

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
                errorLabel.setText("Acces refuse : ce compte n'a pas le role " + selectedRole.getText() + ".");
            }
        } else {
            errorLabel.setText("Email ou mot de passe incorrect.");
        }
    }

    @FXML
    private void handleForgotPassword() {
        errorLabel.setText("");

        String email = requestResetEmail();
        if (email == null) {
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Le format de l'email est incorrect.");
            return;
        }

        User user;
        try {
            user = AuthService.findByEmail(email);
        } catch (RuntimeException e) {
            errorLabel.setText(e.getMessage());
            return;
        }

        if (user == null) {
            errorLabel.setText("Aucun compte n'est associe a cet email.");
            return;
        }

        String resetCode = generateResetCode();
        try {
            emailService.sendPasswordResetCode(email, resetCode);
        } catch (RuntimeException e) {
            errorLabel.setText(e.getMessage());
            return;
        }

        PasswordResetData resetData = requestResetValidation(email);
        if (resetData == null) {
            return;
        }
        if (!resetCode.equals(resetData.code())) {
            errorLabel.setText("Le code de verification est incorrect.");
            return;
        }
        if (!PASSWORD_PATTERN.matcher(resetData.newPassword()).matches()) {
            errorLabel.setText("Mot de passe invalide : 8 caracteres min, 1 majuscule, 1 chiffre, 1 caractere special.");
            return;
        }
        if (!resetData.newPassword().equals(resetData.confirmPassword())) {
            errorLabel.setText("La confirmation du mot de passe ne correspond pas.");
            return;
        }

        try {
            boolean updated = AuthService.updatePasswordByEmail(email, resetData.newPassword());
            if (updated) {
                errorLabel.setStyle("-fx-text-fill: green;");
                errorLabel.setText("Mot de passe reinitialise avec succes. Vous pouvez vous connecter.");
                passwordField.clear();
                emailField.setText(email);
            } else {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Impossible de mettre a jour le mot de passe.");
            }
        } catch (RuntimeException e) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(e.getMessage());
        }
    }

    private String requestResetEmail() {
        Dialog<String> dialog = createBaseDialog("Mot de passe oublie", "Recevoir un code de verification");
        ButtonType sendButton = new ButtonType("Envoyer le code", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, ButtonType.CANCEL);

        TextField resetEmailField = new TextField(emailField.getText().trim());
        resetEmailField.setPromptText("Votre email");
        resetEmailField.setStyle(fieldStyle());

        GridPane grid = createDialogGrid();
        grid.add(new Label("Email"), 0, 0);
        grid.add(resetEmailField, 0, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> buttonType == sendButton ? resetEmailField.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private PasswordResetData requestResetValidation(String email) {
        Dialog<PasswordResetData> dialog = createBaseDialog("Verification", "Saisissez le code recu et votre nouveau mot de passe");
        ButtonType validateButton = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(validateButton, ButtonType.CANCEL);

        Label helper = new Label("Un code a ete envoye a " + email);
        helper.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");

        TextField codeField = new TextField();
        codeField.setPromptText("Code a 6 chiffres");
        codeField.setStyle(fieldStyle());

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe");
        newPasswordField.setStyle(fieldStyle());

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.setStyle(fieldStyle());

        GridPane grid = createDialogGrid();
        grid.add(helper, 0, 0);
        grid.add(new Label("Code"), 0, 1);
        grid.add(codeField, 0, 2);
        grid.add(new Label("Nouveau mot de passe"), 0, 3);
        grid.add(newPasswordField, 0, 4);
        grid.add(new Label("Confirmation"), 0, 5);
        grid.add(confirmPasswordField, 0, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> buttonType == validateButton
                ? new PasswordResetData(codeField.getText().trim(), newPasswordField.getText(), confirmPasswordField.getText())
                : null);

        return dialog.showAndWait().orElse(null);
    }

    private <T> Dialog<T> createBaseDialog(String title, String header) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 18;");
        return dialog;
    }

    private GridPane createDialogGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));
        return grid;
    }

    private String fieldStyle() {
        return "-fx-background-radius: 15; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 15;";
    }

    private String generateResetCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void redirectUser(User user) {
        try {
            String fxmlPath;
            if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
                fxmlPath = "/admin_dashboard.fxml";
            } else if ("ROLE_PSYCHOLOGUE".equalsIgnoreCase(user.getRole())) {
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
                if (controller instanceof PatientDashboardController patCtrl) {
                    patCtrl.setUserData(user);
                } else if (controller instanceof PsyDashboardController psyCtrl) {
                    psyCtrl.setUserData(user);
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

    private record PasswordResetData(String code, String newPassword, String confirmPassword) {
    }
}
