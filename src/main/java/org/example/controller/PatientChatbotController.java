package org.example.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.entities.User;
import org.example.service.GeminiChatService;
import org.example.utils.UserSession;

import java.io.IOException;

public class PatientChatbotController {
    @FXML private Label titleLabel;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label statusLabel;

    private final GeminiChatService geminiChatService = new GeminiChatService();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance();
        if (currentUser != null) {
            titleLabel.setText("Assistant psychologique de " + currentUser.getPrenom());
        }
        addBotMessage("Bonjour. Je suis un assistant temporaire en attendant votre psychologue. " +
                "Je parle uniquement de ressenti, stress, anxiete, tristesse, peur, relations ou mal-etre. " +
                "Comment vous sentez-vous aujourd'hui ?");
    }

    public void setUserData(User user) {
        this.currentUser = user;
        if (titleLabel != null && user != null) {
            titleLabel.setText("Assistant psychologique de " + user.getPrenom());
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        addUserMessage(message);
        messageInput.clear();
        statusLabel.setText("Analyse en cours...");
        sendButton.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                String patientName = currentUser == null ? "Patient" : currentUser.getPrenom() + " " + currentUser.getNom();
                return geminiChatService.askPsychologyAssistant(patientName, message);
            }
        };

        task.setOnSucceeded(event -> {
            addBotMessage(task.getValue());
            statusLabel.setText("Assistant disponible.");
            sendButton.setDisable(false);
        });

        task.setOnFailed(event -> {
            addBotMessage("Je ne suis pas disponible pour le moment. Essayez plus tard ou contactez votre psychologue.");
            statusLabel.setText(task.getException() == null ? "Erreur reseau." : task.getException().getMessage());
            sendButton.setDisable(false);
        });

        Thread worker = new Thread(task, "gemini-chatbot");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent root = loader.load();
            PatientDashboardController controller = loader.getController();
            if (currentUser != null) {
                controller.setUserData(currentUser);
            }
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de revenir au dashboard patient : " + e.getMessage(), e);
        }
    }

    private void addUserMessage(String text) {
        messagesBox.getChildren().add(createBubble(text, true));
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        messagesBox.getChildren().add(createBubble(text, false));
        scrollToBottom();
    }

    private HBox createBubble(String text, boolean mine) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(380);
        label.setStyle(mine
                ? "-fx-background-color: linear-gradient(to right, #16a34a, #22c55e); -fx-text-fill: white; -fx-padding: 12 16; -fx-background-radius: 20;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-padding: 12 16; -fx-background-radius: 20;");

        HBox row = new HBox(label);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }
}
