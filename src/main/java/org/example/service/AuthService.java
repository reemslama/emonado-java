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

        // Utilisation du try-with-resources pour garantir la fermeture de la connexion et du statement
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            pstmt.setString(7, user.getSexe());

            // Modification de la gestion de la date pour être plus robuste
            if (user.getDateNaissance() != null) {
                pstmt.setDate(8, java.sql.Date.valueOf(user.getDateNaissance()));
            } else {
                pstmt.setNull(8, Types.DATE);
            }

            pstmt.executeUpdate();
            System.out.println("✅ Patient enregistré avec succès !");

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors de l'inscription : " + e.getMessage());
            // Il est préférable de jeter une exception pour que le Controller puisse l'attraper
            throw new RuntimeException("Erreur base de données : " + e.getMessage());
        }
    }

    /**
     * Authentification et recuperation de toutes les donnees de l'utilisateur
     */
    public static User authenticate(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ? AND password = ?";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();

                    // Récupération des données
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setTelephone(rs.getString("telephone"));
                    user.setSexe(rs.getString("sexe"));
                    user.setSpecialite(rs.getString("specialite"));

                    // Correction de la récupération de la date SQL vers LocalDate Java
                    Date sqlDate = rs.getDate("dateNaissance");
                    if (sqlDate != null) {
                        user.setDateNaissance(sqlDate.toLocalDate());
                    }

                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL Authentification : " + e.getMessage());
        }
        return null;
    }
}
