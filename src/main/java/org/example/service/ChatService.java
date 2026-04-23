package org.example.service;

import org.example.entities.ChatMessage;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    public ChatService() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS chat_message (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    sender_id INT NOT NULL,
                    receiver_id INT NOT NULL,
                    content TEXT NOT NULL,
                    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = DataSource.getInstance().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser la messagerie : " + e.getMessage(), e);
        }
    }

    public List<ChatMessage> getConversation(int firstUserId, int secondUserId) {
        String sql = """
                SELECT id, sender_id, receiver_id, content, sent_at
                FROM chat_message
                WHERE (sender_id = ? AND receiver_id = ?)
                   OR (sender_id = ? AND receiver_id = ?)
                ORDER BY sent_at ASC, id ASC
                """;

        List<ChatMessage> messages = new ArrayList<>();
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, firstUserId);
            statement.setInt(2, secondUserId);
            statement.setInt(3, secondUserId);
            statement.setInt(4, firstUserId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ChatMessage message = new ChatMessage();
                    message.setId(rs.getInt("id"));
                    message.setSenderId(rs.getInt("sender_id"));
                    message.setReceiverId(rs.getInt("receiver_id"));
                    message.setContent(rs.getString("content"));
                    Timestamp sentAt = rs.getTimestamp("sent_at");
                    message.setSentAt(sentAt != null ? sentAt.toLocalDateTime() : LocalDateTime.now());
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de charger la conversation : " + e.getMessage(), e);
        }

        return messages;
    }

    public void sendMessage(int senderId, int receiverId, String content) {
        String sql = "INSERT INTO chat_message (sender_id, receiver_id, content) VALUES (?, ?, ?)";

        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, senderId);
            statement.setInt(2, receiverId);
            statement.setString(3, content);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'envoyer le message : " + e.getMessage(), e);
        }
    }
}
