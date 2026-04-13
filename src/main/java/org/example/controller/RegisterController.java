package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.entities.User;
import org.example.service.AuthService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField nomField, prenomField, emailField, phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker datePicker;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        if (sexeCombo != null) {
            sexeCombo.getItems().addAll("Homme", "Femme");
        }
    }

    @FXML
    private void handleRegister() {
        errorLabel.setText(""); // Reset erreur

        // --- CONTROLE DE SAISIE ---

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String phone = phoneField.getText().trim();
        String sexe = sexeCombo.getValue();
        LocalDate dateNais = datePicker.getValue();

        // 1. Vérification des champs vides
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || sexe == null || dateNais == null) {
            errorLabel.setText("Tous les champs sont obligatoires.");
            return;
        }

        // 2. Format Email
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!Pattern.matches(emailRegex, email)) {
            errorLabel.setText("Format d'email invalide (ex: exemple@mail.com).");
            return;
        }

        // 3. Complexité Mot de passe (8 carac, 1 Maj, 1 Chiffre, 1 Spécial)
        // Regex : (?=.*[0-9]) (un chiffre) (?=.*[a-z]) (minuscule) (?=.*[A-Z]) (Majuscule) (?=.*[@#$%^&+=!]) (Spécial)
        String pwdRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!/\\*\\-]).{8,}$";
        if (!Pattern.matches(pwdRegex, password)) {
            errorLabel.setText("MDP: 8 caractères min, 1 Majuscule, 1 Chiffre et 1 Caractère spécial.");
            return;
        }

        // 4. Téléphone (8 chiffres)
        if (!phone.matches("\\d{8}")) {
            errorLabel.setText("Le numéro de téléphone doit contenir exactement 8 chiffres.");
            return;
        }



        // --- SI TOUT EST OK ---
        User nouveauPatient = new User();
        nouveauPatient.setNom(nom);
        nouveauPatient.setPrenom(prenom);
        nouveauPatient.setEmail(email);
        nouveauPatient.setPassword(password);
        nouveauPatient.setTelephone(phone);
        nouveauPatient.setSexe(sexe);
        nouveauPatient.setDateNaissance(dateNais);

        AuthService.addPatient(nouveauPatient);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("Compte créé avec succès !");
        alert.showAndWait();

        goToLogin();
    }

    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            nomField.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}