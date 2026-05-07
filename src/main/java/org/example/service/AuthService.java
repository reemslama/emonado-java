package org.example.service;

import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class AuthService {

    /**
     * Inscription d'un nouveau patient
     */
    public static User addPatient(User user) {
        user.setRole("ROLE_PATIENT");
        return addUser(user);
    }

    public static User addUser(User user) {
        String query = "INSERT INTO user (nom, prenom, email, password, roles, telephone, sexe, date_naissance, specialite, avatar, face_id_image_path, has_child, reset_password_token, reset_password_token_expires_at, psychologue_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


        Connection conn = DataSource.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            String hashedPassword = PasswordHashService.ensureHashed(user.getPassword());
            user.setPassword(hashedPassword);
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, user.getRoles());
            pstmt.setString(6, user.getTelephone());
            pstmt.setString(7, user.getSexe());
            pstmt.setDate(8, user.getdate_naissance() != null ? Date.valueOf(user.getdate_naissance()) : null);
            pstmt.setString(9, user.getSpecialite());
            pstmt.setString(10, user.getAvatar());
            pstmt.setString(11, user.getFaceIdImagePath());
            pstmt.setBoolean(12, user.isHasChild());
            pstmt.setString(13, user.getResetPasswordToken());
            pstmt.setTimestamp(14, user.getResetPasswordTokenExpiresAt() != null
                    ? Timestamp.valueOf(user.getResetPasswordTokenExpiresAt())
                    : null);
            if (user.getPsychologueId() != null) {
                pstmt.setInt(15, user.getPsychologueId());
            } else {
                pstmt.setNull(15, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de l'inscription : " + e.getMessage(), e);
        }
    }

    /**
     * Authentification et recuperation de toutes les donnees de l'utilisateur
     */
    public static User authenticate(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ?";

        Connection conn = DataSource.getInstance().getConnection();

        // ✅ Vérification que la connexion existe
        if (conn == null) {
            System.err.println("❌ Impossible de se connecter à la base de données.");
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = getOptionalString(rs, "password");
                    if (PasswordHashService.matches(password, storedHash)) {
                        return mapUser(rs);
                    }
                    if (storedHash != null && storedHash.equals(password)) {
                        updatePasswordByEmail(email, password);
                        return findByEmail(email);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL Authentification : " + e.getMessage());
        }
        return null;
    }

    public static User findByEmail(String email) {
        String query = "SELECT * FROM user WHERE email = ?";
        Connection conn = DataSource.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de la recherche email : " + e.getMessage(), e);
        }
        return null;
    }

    public static boolean updatePasswordByEmail(String email, String newPassword) {
        String query = "UPDATE user SET password = ? WHERE email = ?";
        Connection conn = DataSource.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, PasswordHashService.ensureHashed(newPassword));
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de la mise a jour du mot de passe : " + e.getMessage(), e);
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setRoles(getOptionalString(rs, "roles"));
        user.setPassword(getOptionalString(rs, "password"));
        user.setTelephone(rs.getString("telephone"));
        user.setSexe(rs.getString("sexe"));
        user.setSpecialite(rs.getString("specialite"));
        user.setHasChild(rs.getBoolean("has_child"));
        user.setAvatar(getOptionalString(rs, "avatar"));
        user.setFaceIdImagePath(getOptionalString(rs, "face_id_image_path"));
        user.setResetPasswordToken(getOptionalString(rs, "reset_password_token"));
        Timestamp resetExpiresAt = getOptionalTimestamp(rs, "reset_password_token_expires_at");
        if (resetExpiresAt != null) {
            user.setResetPasswordTokenExpiresAt(resetExpiresAt.toLocalDateTime());
        }
        Integer psychologueId = getOptionalInt(rs, "psychologue_id");
        if (psychologueId != null) {
            user.setPsychologueId(psychologueId);
        }

        Date birthDate = rs.getDate("date_naissance");
        if (birthDate != null) {
            user.setdate_naissance(birthDate.toLocalDate());
        }
        return user;
    }

    private static String getOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private static Timestamp getOptionalTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private static Integer getOptionalInt(ResultSet rs, String column) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }
}
