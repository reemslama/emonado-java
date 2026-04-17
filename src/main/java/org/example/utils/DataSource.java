package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;
    private Connection connection;

    private static final String URL      = "jdbc:mysql://localhost:3306/emonado?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private DataSource() {
        connect();
    }

    // ✅ Méthode de connexion séparée pour pouvoir retry
    private void connect() {
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

    // ✅ Reset du singleton pour forcer une nouvelle tentative
    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        } else {
            // Si la connexion est morte, on recrée l'instance
            try {
                if (instance.connection == null || instance.connection.isClosed()) {
                    System.out.println("🔄 Réinitialisation du singleton DataSource...");
                    instance = new DataSource();
                }
            } catch (SQLException e) {
                instance = new DataSource();
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("⚠️ Connexion perdue, tentative de reconnexion...");
                connect(); // ✅ Réutilise la méthode connect()
            }
        } catch (SQLException e) {
            System.err.println("❌ Reconnexion échouée : " + e.getMessage());
            connect();
        }

        if (connection == null) {
            throw new RuntimeException("❌ Impossible d'obtenir une connexion MySQL. Vérifie que MySQL est démarré.");
        }

        return connection;
    }
}