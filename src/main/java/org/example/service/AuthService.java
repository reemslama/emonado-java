package org.example.service;

import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    /**
     * Inscription d'un nouveau patient
     */
    public static void addPatient(User user) {
        user.setRole("ROLE_PATIENT");
        addUser(user);
    }

    public static void addUser(User user) {
        String query = "INSERT INTO user (nom, prenom, email, password, role, telephone, sexe, dateNaissance, specialite) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DataSource.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            pstmt.setString(7, user.getSexe());
            pstmt.setDate(8, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            pstmt.setString(9, user.getSpecialite());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de l'inscription : " + e.getMessage(), e);
        }
    }

    /**
     * Authentification et recuperation de toutes les donnees de l'utilisateur
     */
    public static User authenticate(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ? AND password = ?";

        Connection conn = DataSource.getInstance().getConnection();

        // ✅ Vérification que la connexion existe
        if (conn == null) {
            System.err.println("❌ Impossible de se connecter à la base de données.");
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setTelephone(rs.getString("telephone"));
                    user.setSexe(rs.getString("sexe"));
                    user.setSpecialite(rs.getString("specialite"));
                    if (rs.getDate("dateNaissance") != null) {
                        user.setDateNaissance(rs.getDate("dateNaissance").toLocalDate());
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL Authentification : " + e.getMessage());
        }
        return null;
    }
}
