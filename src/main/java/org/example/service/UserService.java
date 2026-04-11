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
                if (d != null) u.setDateNaissance(d.toLocalDate());
                users.add(u);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }
}