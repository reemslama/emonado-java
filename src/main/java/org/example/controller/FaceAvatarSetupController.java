package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import org.example.entities.User;
import org.example.service.FaceProfileService;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FaceAvatarSetupController {
    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label facePhotoLabel;
    @FXML private Label initialsLabel;
    @FXML private Region avatarPreviewCircle;
    @FXML private ImageView facePreviewImage;

    private User registeredUser;
    private String selectedAvatar;
    private Path selectedFacePhoto;

    @FXML
    public void initialize() {
        facePreviewImage.setPreserveRatio(true);
        facePreviewImage.setFitWidth(150);
        facePreviewImage.setFitHeight(150);
        statusLabel.setText("");
        facePhotoLabel.setText("Aucune photo choisie.");
        applyAvatarPreview(null);
    }

    public void setRegisteredUser(User user) {
        this.registeredUser = user;
        if (user == null) {
            return;
        }

        welcomeLabel.setText("Compte cree pour " + user.getPrenom());
        subtitleLabel.setText("Vous pouvez ajouter un avatar et une photo Face ID, ou cliquer sur Ignorer.");
        initialsLabel.setText(buildInitials(user));
        applyAvatarPreview(user.getAvatar());
    }

    @FXML
    private void handleAvatarSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button button)) {
            return;
        }
        selectedAvatar = FaceProfileService.normalizeAvatar((String) button.getUserData());
        applyAvatarPreview(selectedAvatar);
        statusLabel.setStyle("-fx-text-fill: #166534;");
        statusLabel.setText("Avatar selectionne : " + button.getText());
    }

    @FXML
    private void chooseFacePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo pour Face ID");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = fileChooser.showOpenDialog(facePreviewImage.getScene().getWindow());
        if (file == null) {
            return;
        }

        selectedFacePhoto = file.toPath();
        facePhotoLabel.setText(file.getName());
        facePreviewImage.setImage(new Image(file.toURI().toString(), 150, 150, true, true));
        statusLabel.setStyle("-fx-text-fill: #166534;");
        statusLabel.setText("Photo visage prete pour Face ID.");
    }

    @FXML
    private void saveAndContinue() {
        if (registeredUser == null) {
            statusLabel.setStyle("-fx-text-fill: #b91c1c;");
            statusLabel.setText("Utilisateur introuvable.");
            return;
        }

        if (selectedAvatar == null && selectedFacePhoto == null) {
            statusLabel.setStyle("-fx-text-fill: #b45309;");
            statusLabel.setText("Choisissez un avatar ou une photo, sinon cliquez sur Ignorer.");
            return;
        }

        try {
            FaceProfileService.savePreferences(registeredUser, selectedAvatar, selectedFacePhoto);
            showInfo("Configuration terminee", "Avatar et Face ID enregistres.");
            goToLogin();
        } catch (RuntimeException e) {
            statusLabel.setStyle("-fx-text-fill: #b91c1c;");
            statusLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void skip() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            if (registeredUser != null) {
                controller.prefillEmail(registeredUser.getEmail());
            }
            welcomeLabel.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page de connexion : " + e.getMessage(), e);
        }
    }

    private void applyAvatarPreview(String avatarKey) {
        String normalized = FaceProfileService.normalizeAvatar(avatarKey);
        avatarPreviewCircle.setStyle(switch (normalized == null ? "" : normalized) {
            case "OCEAN" -> "-fx-background-color: linear-gradient(to bottom right, #38bdf8, #2563eb); -fx-background-radius: 55;";
            case "SOLEIL" -> "-fx-background-color: linear-gradient(to bottom right, #facc15, #f97316); -fx-background-radius: 55;";
            case "ROSE" -> "-fx-background-color: linear-gradient(to bottom right, #f9a8d4, #ec4899); -fx-background-radius: 55;";
            case "FORET" -> "-fx-background-color: linear-gradient(to bottom right, #4ade80, #166534); -fx-background-radius: 55;";
            case "LUNE" -> "-fx-background-color: linear-gradient(to bottom right, #94a3b8, #334155); -fx-background-radius: 55;";
            default -> "-fx-background-color: linear-gradient(to bottom right, #6ee7b7, #14b8a6); -fx-background-radius: 55;";
        });
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
