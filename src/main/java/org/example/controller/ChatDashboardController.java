package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.entities.ChatMessage;
import org.example.entities.User;
import org.example.service.ChatService;
import org.example.service.UserService;
import org.example.utils.UserSession;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatDashboardController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ListView<User> contactListView;
    @FXML private Label conversationNameLabel;
    @FXML private Label conversationMetaLabel;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label emptyStateLabel;

    private final ChatService chatService = new ChatService();
    private User currentUser;
    private User selectedContact;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance();

        contactListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox textBox = new VBox(2);
                Label name = new Label(item.getPrenom() + " " + item.getNom());
                name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
                Label meta = new Label(buildContactMeta(item));
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                textBox.getChildren().addAll(name, meta);
                setGraphic(textBox);
            }
        });

        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedContact = newValue;
            renderConversation();
        });

        loadContacts();
    }

    private void loadContacts() {
        if (currentUser == null) {
            return;
        }

        boolean patient = UserService.isPatientRole(currentUser.getRole());
        titleLabel.setText(patient ? "Mes psychologues" : "Mes patients");
        subtitleLabel.setText(patient
                ? "Choisissez un psychologue pour discuter en direct."
                : "Choisissez un patient pour ouvrir une discussion.");

        List<User> contacts = patient ? UserService.getPsychologues() : UserService.getPatients();
        contacts.removeIf(user -> user.getId() == currentUser.getId());
        contactListView.getItems().setAll(contacts);

        if (!contacts.isEmpty()) {
            contactListView.getSelectionModel().selectFirst();
        }
    }

    private String buildContactMeta(User user) {
        if (UserService.isPsychologueRole(user.getRole())) {
            return user.getSpecialite() == null || user.getSpecialite().isBlank()
                    ? "Psychologue"
                    : user.getSpecialite();
        }
        return user.getEmail();
    }

    private void renderConversation() {
        messagesBox.getChildren().clear();

        if (selectedContact == null || currentUser == null) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            conversationNameLabel.setText("Aucune discussion");
            conversationMetaLabel.setText("Selectionnez un contact.");
            sendButton.setDisable(true);
            return;
        }

        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);
        sendButton.setDisable(false);

        conversationNameLabel.setText(selectedContact.getPrenom() + " " + selectedContact.getNom());
        conversationMetaLabel.setText(buildContactMeta(selectedContact));

        List<ChatMessage> messages = chatService.getConversation(currentUser.getId(), selectedContact.getId());
        if (messages.isEmpty()) {
            Label noMessages = new Label("Aucun message pour le moment. Lancez la discussion.");
            noMessages.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            messagesBox.getChildren().add(noMessages);
        } else {
            for (ChatMessage message : messages) {
                messagesBox.getChildren().add(createMessageBubble(message));
            }
        }

        messagesScrollPane.layout();
        messagesScrollPane.setVvalue(1.0);
    }

    private HBox createMessageBubble(ChatMessage message) {
        boolean mine = message.getSenderId() == currentUser.getId();

        Label content = new Label(message.getContent());
        content.setWrapText(true);
        content.setMaxWidth(320);
        content.setStyle(mine
                ? "-fx-background-color: linear-gradient(to right, #2563eb, #06b6d4);"
                + "-fx-background-radius: 20; -fx-padding: 12 16; -fx-text-fill: white;"
                : "-fx-background-color: #e2e8f0; -fx-background-radius: 20;"
                + "-fx-padding: 12 16; -fx-text-fill: #0f172a;");

        Label time = new Label(message.getSentAt().format(TIME_FORMAT));
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        VBox bubble = new VBox(5, content, time);
        bubble.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox(bubble);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    @FXML
    private void sendMessage() {
        if (selectedContact == null || currentUser == null) {
            return;
        }

        String content = messageInput.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        chatService.sendMessage(currentUser.getId(), selectedContact.getId(), content);
        messageInput.clear();
        renderConversation();
    }

    @FXML
    private void goBack() {
        if (currentUser == null) {
            loadRoot("/login.fxml");
            return;
        }

        if (UserService.isPsychologueRole(currentUser.getRole())) {
            loadRoot("/psy_dashboard.fxml", currentUser);
        } else {
            loadRoot("/patient_dashboard.fxml", currentUser);
        }
    }

    private void loadRoot(String fxmlPath) {
        loadRoot(fxmlPath, null);
    }

    private void loadRoot(String fxmlPath, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (user != null && controller != null) {
                try {
                    controller.getClass().getMethod("setUserData", User.class).invoke(controller, user);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger l'interface : " + e.getMessage(), e);
        }
    }
}
