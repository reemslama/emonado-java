package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;
    private Connection connection;

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3307";
    private static final String DEFAULT_DB = "emonado";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private final String url;
    private final String user;
    private final String password;

    private DataSource() {
        String host = readConfig("DB_HOST", DEFAULT_HOST);
        String port = readConfig("DB_PORT", DEFAULT_PORT);
        String database = readConfig("DB_NAME", DEFAULT_DB);

        this.user = readConfig("DB_USER", DEFAULT_USER);
        this.password = readConfig("DB_PASSWORD", DEFAULT_PASSWORD);
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        connect();
    }

    private static String readConfig(String key, String defaultValue) {
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return defaultValue;
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion MySQL reussie : " + url);
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL introuvable : " + e.getMessage());
            this.connection = null;
        } catch (SQLException e) {
            System.err.println("Erreur connexion MySQL : " + e.getMessage());
            System.err.println("Configuration utilisee : url=" + url + ", user=" + user);
            System.err.println("Verifie que MySQL est demarre et que la base existe.");
            this.connection = null;
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        } else {
            try {
                if (instance.connection == null || instance.connection.isClosed()) {
                    System.out.println("Reinitialisation du singleton DataSource...");
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
                System.out.println("Connexion perdue, tentative de reconnexion...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Reconnexion echouee : " + e.getMessage());
            connect();
        }

        if (connection == null) {
            throw new RuntimeException("Impossible d'obtenir une connexion MySQL. Verifie MySQL, le port et le nom de base.");
        }

        return connection;
    }
}
