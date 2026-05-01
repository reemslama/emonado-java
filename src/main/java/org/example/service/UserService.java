package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserService {
    public static ObservableList<User> getByRole(String role) {
        ObservableList<User> users = FXCollections.observableArrayList();
        String query = "SELECT * FROM user WHERE UPPER(roles) LIKE ? ORDER BY prenom, nom";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%\"" + role.toUpperCase() + "\"%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static void updatePatientProfile(User user) throws SQLException {
        String query = "UPDATE user SET nom = ?, prenom = ?, email = ?, telephone = ?, sexe = ?, date_naissance = ? WHERE id = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getTelephone());
            pstmt.setString(5, user.getSexe());
            pstmt.setDate(6, user.getdate_naissance() != null ? Date.valueOf(user.getdate_naissance()) : null);
            pstmt.setInt(7, user.getId());
            pstmt.executeUpdate();
        }
    }

    public static List<User> getPatients() {
        return getByRoles("ROLE_PATIENT", "PATIENT", "USER");
    }

    public static List<User> getPsychologues() {
        return getByRoles("ROLE_PSYCHOLOGUE", "ROLE_PSY", "PSYCHOLOGUE", "PSY");
    }

    public static boolean isPatientRole(String role) {
        if (role == null) {
            return true;
        }
        String normalized = role.trim().toUpperCase();
        return normalized.equals("ROLE_PATIENT") || normalized.equals("PATIENT") || normalized.equals("USER");
    }

    public static boolean isPsychologueRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return normalized.equals("ROLE_PSYCHOLOGUE")
                || normalized.equals("ROLE_PSY")
                || normalized.equals("PSYCHOLOGUE")
                || normalized.equals("PSY");
    }

    private static List<User> getByRoles(String... roles) {
        List<User> users = new ArrayList<>();
        String conditions = String.join(" OR ", Collections.nCopies(roles.length, "UPPER(roles) LIKE ?"));
        String query = "SELECT * FROM user WHERE " + conditions + " ORDER BY prenom, nom";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < roles.length; i++) {
                pstmt.setString(i + 1, "%\"" + roles[i].toUpperCase() + "\"%");
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des utilisateurs : " + e.getMessage(), e);
        }

        return users;
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
}
