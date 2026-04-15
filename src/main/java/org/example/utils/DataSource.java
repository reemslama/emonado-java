package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;
    private Connection connection;

    // ✅ Modifie ces valeurs selon ta config
    private static final String URL      = "jdbc:mysql://localhost:3306/emonado";
    private static final String USER     = "root";
    private static final String PASSWORD = "";  // ton mot de passe MySQL

    private DataSource() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion MySQL réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver MySQL introuvable : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Erreur connexion MySQL : " + e.getMessage());
            System.err.println("👉 Vérifie que MySQL est démarré (XAMPP / WAMP / services)");
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
            // Reconnexion automatique si connexion perdue
            if (connection == null || connection.isClosed()) {
                System.err.println("⚠️ Connexion perdue, tentative de reconnexion...");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("❌ Reconnexion échouée : " + e.getMessage());
        }
        return connection;
    }
}