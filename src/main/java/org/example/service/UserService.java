package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.entities.User;
import org.example.utils.DataSource;
import java.sql.*;

public class UserService {
    public static ObservableList<User> getByRole(String role) {
        ObservableList<User> users = FXCollections.observableArrayList();
        String query = "SELECT * FROM user WHERE role = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setTelephone(rs.getString("telephone"));
                u.setSexe(rs.getString("sexe"));
                u.setSpecialite(rs.getString("specialite"));
                u.setRole(rs.getString("role"));
                Date d = rs.getDate("dateNaissance");
                u.setHasChild(rs.getBoolean("hasChild"));
                if (d != null) u.setDateNaissance(d.toLocalDate());
                users.add(u);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    public static void updatePatientProfile(User user) throws SQLException {
        String query = "UPDATE user SET nom = ?, prenom = ?, email = ?, telephone = ?, sexe = ?, dateNaissance = ? WHERE id = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getTelephone());
            pstmt.setString(5, user.getSexe());
            pstmt.setDate(6, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            pstmt.setInt(7, user.getId());
            pstmt.executeUpdate();
        }
    }
}
