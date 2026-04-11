package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;

    private final String url = "jdbc:mysql://localhost:3306/emonado";
    private final String user = "root";
    private final String password = "";

    private Connection connection;

    private DataSource() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion MySQL réussie !");
        } catch (Exception e) {
            System.err.println("❌ Erreur connexion : " + e.getMessage());
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            System.err.println("Erreur connexion : " + e.getMessage());
        }
        return connection;
    }
}