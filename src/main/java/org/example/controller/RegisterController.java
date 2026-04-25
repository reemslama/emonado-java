package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    @FXML private CheckBox hasChildCheckBox;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.trim(), formatter);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
    }

    @FXML
    private void onHasChildToggled() {
        errorLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText("");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String phone = phoneField.getText().trim();
        String sexe = sexeCombo.getValue();
        LocalDate dateNais = parseBirthDate();
        boolean hasChild = hasChildCheckBox.isSelected();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()
                || phone.isEmpty() || sexe == null || dateNais == null) {
            errorLabel.setText("Tous les champs sont obligatoires.");
            return;
        }
        if (nom.length() < 3) {
            errorLabel.setText("Le nom doit contenir plus de 4 caracteres.");
            return;
        }
        if (prenom.length() < 3) {
            errorLabel.setText("Le prenom doit contenir plus de 4 caracteres.");
            return;
        }

        LocalDate dateMax = LocalDate.now().minusYears(16);
        if (dateNais.isAfter(dateMax)) {
            errorLabel.setText("Le parent doit avoir au moins 16 ans.");
            return;
        }
        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email)) {
            errorLabel.setText("Format d'email invalide.");
            return;
        }
        if (!Pattern.matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$", password)) {
            errorLabel.setText("Mot de passe: 8 caracteres min, 1 majuscule, 1 chiffre, 1 caractere special.");
            return;
        }
        if (!phone.matches("\\d{8}")) {
            errorLabel.setText("Le numero de telephone doit contenir exactement 8 chiffres.");
            return;
        }

        User nouveauPatient = new User();
        nouveauPatient.setNom(nom);
        nouveauPatient.setPrenom(prenom);
        nouveauPatient.setEmail(email);
        nouveauPatient.setPassword(password);
        nouveauPatient.setTelephone(phone);
        nouveauPatient.setSexe(sexe);
        nouveauPatient.setDateNaissance(dateNais);
        nouveauPatient.setHasChild(hasChild);

        User patientCree;
        try {
            patientCree = AuthService.addPatient(nouveauPatient);
        } catch (RuntimeException e) {
            errorLabel.setText(extractErrorMessage(e));
            return;
        }

        openFaceAvatarSetup(patientCree);
    }

    private void showSuccessAndGoLogin(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        goToLogin();
    }

    private void openFaceAvatarSetup(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/face_avatar_setup.fxml"));
            Parent root = loader.load();
            FaceAvatarSetupController controller = loader.getController();
            controller.setRegisteredUser(user);
            nomField.getScene().setRoot(root);
        } catch (IOException e) {
            String message = user != null && user.isHasChild()
                    ? "Compte parent cree avec succes. Connectez-vous en tant que parent pour ouvrir l'Espace Enfant."
                    : "Compte cree avec succes.";
            showSuccessAndGoLogin(message);
        }
    }

    private LocalDate parseBirthDate() {
        String text = datePicker.getEditor().getText();
        if (text == null || text.trim().isEmpty()) {
            return datePicker.getValue();
        }
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
        if (message == null || message.isBlank()) {
            return "Erreur lors de l'inscription.";
        }
        if (message.contains("Duplicate entry") || message.contains("duplicate")) {
            return "Cet email existe deja.";
        }
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
