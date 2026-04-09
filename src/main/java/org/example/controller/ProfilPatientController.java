package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.entities.User;
import org.example.utils.DataSource;
import org.example.utils.UserSession;
import java.io.IOException;
import java.sql.*;

public class ProfilPatientController {
    @FXML private Label titleLabel;
    @FXML private TextField nomField, prenomField, emailField, phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker datePicker;

    private User currentUser;

    @FXML
    public void initialize() {
        // Au cas où setUserData n'est pas appelé, on récupère via la session
        if (this.currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) return;
        this.currentUser = user;

        if (titleLabel != null) titleLabel.setText("Profil de " + currentUser.getPrenom());
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getTelephone());

        sexeCombo.getItems().setAll("Homme", "Femme");
        sexeCombo.setValue(currentUser.getSexe());
        datePicker.setValue(currentUser.getDateNaissance());
    }

    @FXML
    private void handleUpdate() {
        if (currentUser == null) return;

        // On vérifie si on doit mettre à jour le mot de passe ou non
        String newPassword = passwordField.getText();
        boolean updatePassword = newPassword != null && !newPassword.trim().isEmpty();

        String query = updatePassword
                ? "UPDATE user SET nom=?, prenom=?, telephone=?, sexe=?, dateNaissance=?, password=? WHERE id=?"
                : "UPDATE user SET nom=?, prenom=?, telephone=?, sexe=?, dateNaissance=? WHERE id=?";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, nomField.getText());
            pstmt.setString(2, prenomField.getText());
            pstmt.setString(3, phoneField.getText());
            pstmt.setString(4, sexeCombo.getValue());
            pstmt.setDate(5, datePicker.getValue() != null ? Date.valueOf(datePicker.getValue()) : null);

            if (updatePassword) {
                pstmt.setString(6, newPassword);
                pstmt.setInt(7, currentUser.getId());
            } else {
                pstmt.setInt(6, currentUser.getId());
            }

            pstmt.executeUpdate();

            // Mettre à jour l'objet en session pour que le reste de l'app soit synchro
            currentUser.setNom(nomField.getText());
            currentUser.setPrenom(prenomField.getText());

            new Alert(Alert.AlertType.INFORMATION, "Profil mis à jour avec succès !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour.").show();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer votre compte ? Cette action est irréversible.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation de suppression");

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE id = ?")) {

                pstmt.setInt(1, currentUser.getId());
                pstmt.executeUpdate();

                UserSession.setInstance(null);
                handleLogout(); // Redirection vers login après suppression

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/patient_dashboard.fxml"));
            nomField.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

}