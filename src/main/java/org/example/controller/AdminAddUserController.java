package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.example.entities.User;
import org.example.service.AuthService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class AdminAddUserController {

    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField passwordField;
    @FXML private TextField specialiteField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private ComboBox<String> roleCombo;
    @FXML private DatePicker datePicker;
    @FXML private Label specialiteLabel;
    @FXML private Label errorLabel;

    private String selectedRole = "ROLE_PATIENT";

    @FXML
    public void initialize() {
        sexeCombo.getItems().addAll("Homme", "Femme");
        roleCombo.getItems().addAll("Patient", "Psychologue");
        roleCombo.setOnAction(event -> updateRoleState());
        setDefaultRole(selectedRole);
    }

    public void setDefaultRole(String role) {
        selectedRole = "ROLE_PSYCHOLOGUE".equals(role) ? "ROLE_PSYCHOLOGUE" : "ROLE_PATIENT";
        roleCombo.setValue("ROLE_PSYCHOLOGUE".equals(selectedRole) ? "Psychologue" : "Patient");
        updateRoleState();
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String phone = phoneField.getText().trim();
        String sexe = sexeCombo.getValue();
        String role = "Psychologue".equals(roleCombo.getValue()) ? "ROLE_PSYCHOLOGUE" : "ROLE_PATIENT";
        String specialite = specialiteField.getText().trim();
        LocalDate dateNaissance = datePicker.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || sexe == null || dateNaissance == null) {
            errorLabel.setText("Tous les champs obligatoires doivent etre remplis.");
            return;
        }
        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            errorLabel.setText("Format d'email invalide.");
            return;
        }
        if (!Pattern.matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!/\\*\\-]).{8,}$", password)) {
            errorLabel.setText("MDP: 8 caracteres min, 1 majuscule, 1 chiffre, 1 special.");
            return;
        }
        if (!phone.matches("\\d{8}")) {
            errorLabel.setText("Le numero de telephone doit contenir 8 chiffres.");
            return;
        }
        if ("ROLE_PSYCHOLOGUE".equals(role) && specialite.isEmpty()) {
            errorLabel.setText("La specialite est obligatoire pour un psychologue.");
            return;
        }

        User user = new User();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setPassword(password);
        user.setTelephone(phone);
        user.setSexe(sexe);
        user.setDateNaissance(dateNaissance);
        user.setRole(role);
        user.setSpecialite("ROLE_PSYCHOLOGUE".equals(role) ? specialite : null);

        try {
            AuthService.addUser(user);
            new Alert(Alert.AlertType.INFORMATION, "Utilisateur ajoute avec succes.").showAndWait();
            loadUserTable(role);
        } catch (RuntimeException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        loadUserTable("Psychologue".equals(roleCombo.getValue()) ? "ROLE_PSYCHOLOGUE" : "ROLE_PATIENT");
    }

    private void updateRoleState() {
        boolean isPsychologue = "Psychologue".equals(roleCombo.getValue());
        specialiteField.setVisible(isPsychologue);
        specialiteField.setManaged(isPsychologue);
        specialiteLabel.setVisible(isPsychologue);
        specialiteLabel.setManaged(isPsychologue);
        formTitle.setText(isPsychologue ? "Ajouter un psychologue" : "Ajouter un patient");
        if (!isPsychologue) {
            specialiteField.clear();
        }
    }

    private void loadUserTable(String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin_users_table.fxml"));
            Parent root = loader.load();

            AdminTableController controller = loader.getController();
            controller.loadData(role);

            StackPane contentArea = (StackPane) nomField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }

            Label headerTitle = (Label) nomField.getScene().lookup("#headerTitle");
            if (headerTitle != null) {
                headerTitle.setText("ROLE_PSYCHOLOGUE".equals(role) ? "Gestion des Psychologues" : "Gestion des Patients");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
