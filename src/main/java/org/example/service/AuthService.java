package org.example.service;

import org.example.entities.User;
import org.example.utils.DataSource;
import java.sql.*;

public class AuthService {

    /**
     * Inscription d'un nouveau patient
     */
    public static void addPatient(User user) {
        String query = "INSERT INTO user (nom, prenom, email, password, role, telephone, sexe, dateNaissance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DataSource.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, "ROLE_PATIENT");
            pstmt.setString(6, user.getTelephone());
            pstmt.setString(7, user.getSexe());
            pstmt.setDate(8, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);

            pstmt.executeUpdate();
            System.out.println("Patient enregistré avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de l'inscription : " + e.getMessage());
        }
    }

    /**
     * Authentification et récupération de TOUTES les données de l'utilisateur
     */
    public static User authenticate(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ? AND password = ?";

        Connection conn = DataSource.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();

                    // ON RÉCUPÈRE TOUT POUR LE PROFIL
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setTelephone(rs.getString("telephone"));
                    user.setSexe(rs.getString("sexe"));
                    user.setSpecialite(rs.getString("specialite")); // Important pour le Psy

                    // Conversion de la date SQL vers LocalDate Java
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