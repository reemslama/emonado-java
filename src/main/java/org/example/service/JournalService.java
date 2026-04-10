package org.example.service;

import org.example.entities.Journal;
import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JournalService {
    private static final List<String> MOODS = List.of("heureux", "calme", "SOS", "en colere");

    public List<Journal> findByUser(User user, String keyword, String sortOrder) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, contenu, humeur, date_creation, user_id FROM journal WHERE user_id = ?"
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(user.getId());

        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (!trimmedKeyword.isEmpty()) {
            sql.append(" AND (LOWER(humeur) LIKE ? OR LOWER(contenu) LIKE ?)");
            String likeValue = "%" + trimmedKeyword.toLowerCase() + "%";
            parameters.add(likeValue);
            parameters.add(likeValue);
        }

        sql.append("old".equalsIgnoreCase(sortOrder)
                ? " ORDER BY date_creation ASC"
                : " ORDER BY date_creation DESC");

        List<Journal> journals = new ArrayList<>();
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    journals.add(mapRow(resultSet));
                }
            }
        }

        return journals;
    }

    public Map<String, Integer> countByMood(User user) throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (String mood : MOODS) {
            stats.put(mood, 0);
        }

        String sql = "SELECT humeur, COUNT(id) AS total FROM journal WHERE user_id = ? GROUP BY humeur";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, user.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String mood = resultSet.getString("humeur");
                    if (stats.containsKey(mood)) {
                        stats.put(mood, resultSet.getInt("total"));
                    }
                }
            }
        }

        return stats;
    }

    public void create(Journal journal) throws SQLException {
        String sql = "INSERT INTO journal (contenu, humeur, date_creation, user_id) VALUES (?, ?, ?, ?)";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, journal.getContenu());
            statement.setString(2, journal.getHumeur());
            statement.setTimestamp(3, Timestamp.valueOf(journal.getDateCreation()));
            statement.setInt(4, journal.getUserId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    journal.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Journal journal) throws SQLException {
        String sql = "UPDATE journal SET contenu = ?, humeur = ? WHERE id = ? AND user_id = ?";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, journal.getContenu());
            statement.setString(2, journal.getHumeur());
            statement.setInt(3, journal.getId());
            statement.setInt(4, journal.getUserId());
            statement.executeUpdate();
        }
    }

    public void delete(int journalId, int userId) throws SQLException {
        String sql = "DELETE FROM journal WHERE id = ? AND user_id = ?";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, journalId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public Journal buildNewJournal(String contenu, String humeur, User user) {
        Journal journal = new Journal();
        journal.setContenu(contenu);
        journal.setHumeur(humeur);
        journal.setDateCreation(LocalDateTime.now());
        journal.setUserId(user.getId());
        return journal;
    }

    private Journal mapRow(ResultSet resultSet) throws SQLException {
        Journal journal = new Journal();
        journal.setId(resultSet.getInt("id"));
        journal.setContenu(resultSet.getString("contenu"));
        journal.setHumeur(resultSet.getString("humeur"));
        Timestamp timestamp = resultSet.getTimestamp("date_creation");
        if (timestamp != null) {
            journal.setDateCreation(timestamp.toLocalDateTime());
        }
        journal.setUserId(resultSet.getInt("user_id"));
        return journal;
    }
}
