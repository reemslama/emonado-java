package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.example.entities.User;
import org.example.service.AuthService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker datePicker;
    @FXML private Label errorLabel;
    @FXML private CheckBox hasChildCheckBox;   // ✅
    @FXML private Button espaceEnfantBtn;       // ✅

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Pour savoir si l'inscription est faite avec hasChild=true
    private boolean inscriptionReussieAvecEnfant = false;

    @FXML
    public void initialize() {
        sexeCombo.getItems().addAll("Homme", "Femme");

        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : formatter.format(date);
            }
            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) return null;
                try {
                    return LocalDate.parse(text.trim(), formatter);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
    }

    // ✅ Quand la checkbox est cochée/décochée
    @FXML
    private void onHasChildToggled() {
        // On ne montre le bouton qu'APRÈS une inscription réussie avec hasChild=true
        // Pendant le remplissage du form, on ne fait rien ici
    }

    @FXML
    private void handleRegister() {
        errorLabel.setText("");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String phone = phoneField.getText().trim();
        String sexe = sexeCombo.getValue();
        LocalDate dateNais = parseBirthDate();
        boolean hasChild = hasChildCheckBox.isSelected(); // ✅

        // --- VALIDATIONS ---
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()
                || phone.isEmpty() || sexe == null || dateNais == null) {
            errorLabel.setText("Tous les champs sont obligatoires.");
            return;
        }
        if (nom.length() <= 4) {
            errorLabel.setText("Le nom doit contenir plus de 4 caractères.");
            return;
        }
        if (prenom.length() <= 4) {
            errorLabel.setText("Le prénom doit contenir plus de 4 caractères.");
            return;
        }
        LocalDate dateMax = LocalDate.now().minusYears(16);
        if (dateNais.isAfter(dateMax)) {
            errorLabel.setText("Le patient doit avoir au moins 16 ans (né avant le "
                    + formatter.format(dateMax) + ").");
            return;
        }
        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email)) {
            errorLabel.setText("Format d'email invalide (ex: exemple@mail.com).");
            return;
        }
        if (!Pattern.matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$", password)) {
            errorLabel.setText("MDP: 8 caractères min, 1 Majuscule, 1 Chiffre, 1 Caractère spécial.");
            return;
        }
        if (!phone.matches("\\d{8}")) {
            errorLabel.setText("Le numéro de téléphone doit contenir exactement 8 chiffres.");
            return;
        }

        // --- CRÉATION DU USER ---
        User nouveauPatient = new User();
        nouveauPatient.setNom(nom);
        nouveauPatient.setPrenom(prenom);
        nouveauPatient.setEmail(email);
        nouveauPatient.setPassword(password);
        nouveauPatient.setTelephone(phone);
        nouveauPatient.setSexe(sexe);
        nouveauPatient.setDateNaissance(dateNais);
        nouveauPatient.setHasChild(hasChild); // ✅

        try {
            AuthService.addPatient(nouveauPatient);
        } catch (RuntimeException e) {
            errorLabel.setText(extractErrorMessage(e));
            return;
        }

        // --- INSCRIPTION RÉUSSIE ---
        if (hasChild) {
            // ✅ Afficher le bouton Espace Enfant directement
            espaceEnfantBtn.setVisible(true);
            espaceEnfantBtn.setManaged(true);
            inscriptionReussieAvecEnfant = true;

            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("✅ Compte créé ! Vous pouvez accéder à l'Espace Enfant.");
        } else {
            // Pas d'enfant → redirection directe vers login
            showSuccessAndGoLogin();
        }
    }

    // ✅ Ouvrir l'Espace Enfant
    @FXML
    private void openEspaceEnfant() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/EspaceEnfant.fxml"));
            espaceEnfantBtn.getScene().setRoot(root);
        } catch (IOException e) {
            errorLabel.setText("Erreur lors de l'ouverture de l'Espace Enfant.");
            e.printStackTrace();
        }
    }

    private void showSuccessAndGoLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("Compte créé avec succès !");
        alert.showAndWait();
        goToLogin();
    }

    private LocalDate parseBirthDate() {
        String text = datePicker.getEditor().getText();
        if (text == null || text.trim().isEmpty()) return datePicker.getValue();
        try {
            LocalDate parsedDate = LocalDate.parse(text.trim(), formatter);
            datePicker.setValue(parsedDate);
            return parsedDate;
        } catch (DateTimeParseException e) {
            errorLabel.setText("Date invalide. Utilisez le format jj/MM/aaaa.");
            return null;
        }
    }

    private String extractErrorMessage(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return "Erreur lors de l'inscription.";
        if (message.contains("Duplicate entry") || message.contains("duplicate")) return "Cet email existe déjà.";
        return message;
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