package org.example.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.example.entities.User;
import org.example.service.AppNavigationService;
import org.example.service.QrLoginService;

import java.io.IOException;

public class QrLoginController {
    @FXML private ImageView qrImageView;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label linkLabel;

    private final QrLoginService qrLoginService = QrLoginService.getInstance();
    private Timeline pollingTimeline;
    private QrLoginService.QrChallenge challenge;

    public void setChallenge(QrLoginService.QrChallenge challenge) {
        this.challenge = challenge;
        qrImageView.setImage(qrLoginService.buildQrImage(challenge.getApprovalUrl(), 280));
        titleLabel.setText("Validation QR pour " + challenge.getUser().getPrenom());
        subtitleLabel.setText("Scannez ce QR code avec votre smartphone sur le meme Wi-Fi pour valider la connexion.");
        linkLabel.setText(challenge.getApprovalUrl());
        startPolling();
    }

    private void startPolling() {
        stopPolling();
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshState()));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
        refreshState();
    }

    private void refreshState() {
        if (challenge == null) {
            return;
        }

        if (challenge.isApproved()) {
            stopPolling();
            qrLoginService.cancelChallenge(challenge.getToken());
            statusLabel.setStyle("-fx-text-fill: #16a34a;");
            statusLabel.setText("Validation recue. Redirection en cours...");
            AppNavigationService.goToDashboard(qrImageView.getScene(), challenge.getUser());
            return;
        }

        if (challenge.isExpired()) {
            stopPolling();
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            statusLabel.setText("Le QR code a expire. Retournez a la connexion pour recommencer.");
            qrLoginService.cancelChallenge(challenge.getToken());
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #475569;");
        statusLabel.setText("En attente de validation... " + challenge.getRemainingSeconds() + " s");
    }

    @FXML
    private void cancel() {
        if (challenge != null) {
            qrLoginService.cancelChallenge(challenge.getToken());
        }
        stopPolling();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            User user = challenge == null ? null : challenge.getUser();
            if (user != null) {
                controller.prefillEmail(user.getEmail());
            }
            qrImageView.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de revenir a l'ecran de connexion : " + e.getMessage(), e);
        }
    }

    private void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }
}
