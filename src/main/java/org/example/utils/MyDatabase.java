package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String url = "jdbc:mysql://localhost:3306/emonado_java";
    private final String user = "root";
    private final String password = "";

    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver non trouvé : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
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
