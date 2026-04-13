package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {
    // L'URL pointe maintenant vers ta base "emonado"
    private final String url = "jdbc:mysql://127.0.0.1:3306/emonado";
    private final String user = "root";
    private final String password = ""; // Vide par défaut sur XAMPP
    private Connection connection;
    private static DataSource instance;

    private DataSource() {
        try {
            // Chargement explicite du driver (optionnel mais recommandé)
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion à la base 'emonado' réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver MySQL non trouvé : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à MySQL : " + e.getMessage());
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
            // Si la connexion est nulle ou a été fermée par erreur, on la réouvre
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la connexion : " + e.getMessage());
        }
        return connection;
    }
}