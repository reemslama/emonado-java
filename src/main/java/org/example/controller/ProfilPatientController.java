package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import org.example.entities.User;
import org.example.service.FaceProfileService;
import org.example.utils.DataSource;
import org.example.utils.UserSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ProfilPatientController {
    @FXML private Label titleLabel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker datePicker;
    @FXML private Region avatarCircle;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label faceIdStatusLabel;
    @FXML private ImageView faceIdImageView;

    private User currentUser;

    @FXML
    public void initialize() {
        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;

        if (titleLabel != null) {
            titleLabel.setText("Profil de " + currentUser.getPrenom());
        }
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getTelephone());

        sexeCombo.getItems().setAll("Homme", "Femme");
        sexeCombo.setValue(currentUser.getSexe());
        datePicker.setValue(currentUser.getDateNaissance());
        avatarInitialsLabel.setText(buildInitials(currentUser));
        applyAvatarAppearance(currentUser.getAvatar());
        updateFaceIdPreview(currentUser.getFaceIdImagePath());
    }

    @FXML
    private void handleUpdate() {
        if (currentUser == null) {
            return;
        }

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

            currentUser.setEmail(emailField.getText());
            currentUser.setNom(nomField.getText());
            currentUser.setPrenom(prenomField.getText());
            currentUser.setTelephone(phoneField.getText());
            currentUser.setSexe(sexeCombo.getValue());
            currentUser.setDateNaissance(datePicker.getValue());
            UserSession.setInstance(currentUser);

            new Alert(Alert.AlertType.INFORMATION, "Profil mis a jour avec succes !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise a jour.").show();
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
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer votre compte ? Cette action est irreversible.",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Confirmation de suppression");

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE id = ?")) {

                pstmt.setInt(1, currentUser.getId());
                pstmt.executeUpdate();

                UserSession.setInstance(null);
                handleLogout();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent root = loader.load();
            PatientDashboardController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            nomField.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyAvatarAppearance(String avatarKey) {
        String normalized = FaceProfileService.normalizeAvatar(avatarKey);
        avatarCircle.setStyle(switch (normalized == null ? "" : normalized) {
            case "OCEAN" -> "-fx-background-color: linear-gradient(to bottom right, #38bdf8, #2563eb); -fx-background-radius: 55; -fx-min-width: 110; -fx-min-height: 110; -fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 55;";
            case "SOLEIL" -> "-fx-background-color: linear-gradient(to bottom right, #facc15, #f97316); -fx-background-radius: 55; -fx-min-width: 110; -fx-min-height: 110; -fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 55;";
            case "ROSE" -> "-fx-background-color: linear-gradient(to bottom right, #f9a8d4, #ec4899); -fx-background-radius: 55; -fx-min-width: 110; -fx-min-height: 110; -fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 55;";
            case "FORET" -> "-fx-background-color: linear-gradient(to bottom right, #4ade80, #166534); -fx-background-radius: 55; -fx-min-width: 110; -fx-min-height: 110; -fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 55;";
            case "LUNE" -> "-fx-background-color: linear-gradient(to bottom right, #94a3b8, #334155); -fx-background-radius: 55; -fx-min-width: 110; -fx-min-height: 110; -fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 55;";
            default -> "-fx-background-color: linear-gradient(to bottom right, #6ee7b7, #14b8a6); -fx-background-radius: 55; -fx-min-width: 110; -fx-min-height: 110; -fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 55;";
        });
    }

    private void updateFaceIdPreview(String faceIdImagePath) {
        if (faceIdImagePath == null || faceIdImagePath.isBlank()) {
            faceIdStatusLabel.setText("Face ID non configure");
            faceIdImageView.setImage(null);
            return;
        }

        Path imagePath = Path.of(faceIdImagePath);
        if (!Files.exists(imagePath)) {
            faceIdStatusLabel.setText("Photo Face ID introuvable");
            faceIdImageView.setImage(null);
            return;
        }

        try (InputStream inputStream = Files.newInputStream(imagePath)) {
            faceIdImageView.setImage(new Image(inputStream));
            faceIdStatusLabel.setText("Face ID configure");
        } catch (IOException e) {
            faceIdStatusLabel.setText("Impossible de charger la photo Face ID");
            faceIdImageView.setImage(null);
        }
    }

    private String buildInitials(User user) {
        String prenomInitial = user.getPrenom() != null && !user.getPrenom().isBlank()
                ? user.getPrenom().substring(0, 1).toUpperCase()
                : "";
        String nomInitial = user.getNom() != null && !user.getNom().isBlank()
                ? user.getNom().substring(0, 1).toUpperCase()
                : "";
        String initials = prenomInitial + nomInitial;
        return initials.isBlank() ? "PT" : initials;
    }
}
