package org.example.service;

import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;

public class AuthService {

    private static String lastAuthenticationError;

    public static String getLastAuthenticationError() {
        return lastAuthenticationError;
    }

    /** True si MySQL répond (utile pour un message d'erreur clair à la connexion). */
    public static boolean isDatabaseReachable() {
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            return false;
        }
        try {
            if (conn.isClosed()) {
                return false;
            }
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Crée le compte de démo si la base est joignable et que l'email n'existe pas encore.
     */
    public static void ensureTestPatientAccountExists() {
        if (!isDatabaseReachable()) {
            return;
        }
        final String testEmail = "patient.test@emonado.local";
        String query = "SELECT id FROM `user` WHERE LOWER(TRIM(email)) = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, testEmail.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        } catch (SQLException e) {
            System.err.println("ensureTestPatientAccountExists (lecture): " + e.getMessage());
            return;
        }

        User u = new User();
        u.setNom("Test");
        u.setPrenom("Patient");
        u.setEmail(testEmail);
        u.setPassword("Test123!");
        u.setTelephone("12345678");
        u.setSexe("Homme");
        u.setDateNaissance(LocalDate.of(1999, 2, 2));
        String err = addPatient(u);
        if (err != null) {
            System.err.println("ensureTestPatientAccountExists: " + err);
        }
    }

    /**
     * Inscription d'un nouveau patient.
     * @return message d'erreur lisible si échec, sinon {@code null}
     */
    public static String addPatient(User user) {
        // `user` est un mot réservé MySQL : backticks obligatoires
        // specialite : requis si la colonne est NOT NULL sans défaut (psy / admin)
        String query = "INSERT INTO `user` (nom, prenom, email, password, role, telephone, sexe, dateNaissance, specialite) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            return "Connexion à la base de données impossible. Vérifiez que MySQL est démarré (XAMPP) et la base 'emonado'.";
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, normalizeEmail(user.getEmail()));
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, "ROLE_PATIENT");
            pstmt.setString(6, user.getTelephone());
            pstmt.setString(7, user.getSexe());
            pstmt.setDate(8, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            // Patient : pas de spécialité (chaîne vide si la colonne est NOT NULL sans défaut)
            pstmt.setString(9, "");

            pstmt.executeUpdate();
            System.out.println("Patient enregistré avec succès !");
            return null;
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == 1062) {
                return "Cet email est déjà utilisé.";
            }
            System.err.println("Erreur SQL (contrainte) : " + e.getMessage());
            return "Données invalides ou contrainte violée : " + e.getMessage();
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Unknown column") && msg.contains("specialite")) {
                return addPatientWithoutSpecialite(user);
            }
            System.err.println("Erreur SQL lors de l'inscription : " + e.getMessage());
            return "Erreur lors de l'inscription : " + e.getMessage();
        }
    }

    /**
     * Anciennes bases sans colonne {@code specialite}.
     */
    private static String addPatientWithoutSpecialite(User user) {
        String query = "INSERT INTO `user` (nom, prenom, email, password, role, telephone, sexe, dateNaissance) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            return "Connexion à la base de données impossible.";
        }
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, normalizeEmail(user.getEmail()));
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, "ROLE_PATIENT");
            pstmt.setString(6, user.getTelephone());
            pstmt.setString(7, user.getSexe());
            pstmt.setDate(8, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            pstmt.executeUpdate();
            return null;
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == 1062) {
                return "Cet email est déjà utilisé.";
            }
            return "Données invalides : " + e.getMessage();
        } catch (SQLException e) {
            return "Erreur lors de l'inscription : " + e.getMessage();
        }
    }

    /**
     * Authentification et récupération de TOUTES les données de l'utilisateur
     */
    public static User authenticate(String email, String password) {
        lastAuthenticationError = null;
        String query = "SELECT * FROM `user` WHERE LOWER(TRIM(email)) = ? AND password = ?";

        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            lastAuthenticationError = "Impossible de contacter la base de données. Démarrez MySQL (XAMPP).";
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, normalizeEmail(email));
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
            lastAuthenticationError = "Erreur lors de la connexion : " + e.getMessage();
        }
        return null;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}