package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSource {
<<<<<<< HEAD

    private static final String DB_NAME = "emonado";

    /**
     * Paramètres recommandés pour XAMPP / MySQL 8 en local (évite erreurs SSL, timezone, clé publique).
     */
    private static final String JDBC_PARAMS =
            "useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC"
                    + "&characterEncoding=UTF-8"
                    + "&connectTimeout=10000";

    private final String host = System.getProperty("emonado.db.host", "127.0.0.1");
    private final String port = System.getProperty("emonado.db.port", "3306");
    private final String user = System.getProperty("emonado.db.user", "root");
    private final String password = System.getProperty("emonado.db.password", "");

    private final String urlWithDb;
    private final String urlNoDb;

    private Connection connection;
    private static DataSource instance;
    private static String lastConnectionError;

    public static String getLastConnectionError() {
        return lastConnectionError;
    }
=======
    private final String url = "jdbc:mysql://127.0.0.1:3306/emonado_java";
    private final String user = "root";
    private final String password = "";
    private Connection connection;
    public static DataSource instance;
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44

    private DataSource() {
        this.urlWithDb = "jdbc:mysql://" + host + ":" + port + "/" + DB_NAME + "?" + JDBC_PARAMS;
        this.urlNoDb = "jdbc:mysql://" + host + ":" + port + "/?" + JDBC_PARAMS;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
<<<<<<< HEAD
            connection = openConnectionWithAutoCreateDb();
            lastConnectionError = null;
            System.out.println("Connexion a la base '" + DB_NAME + "' reussie !");
        } catch (ClassNotFoundException e) {
            lastConnectionError = "Driver MySQL introuvable : " + e.getMessage();
            System.err.println(lastConnectionError);
        } catch (SQLException e) {
            lastConnectionError = e.getMessage();
            System.err.println("Erreur de connexion a MySQL : " + lastConnectionError);
        }
    }

    /**
     * Ouvre une connexion ; si la base n'existe pas, tente de la créer puis reconnecte.
     */
    private Connection openConnectionWithAutoCreateDb() throws SQLException {
        Connection c;
        try {
            c = DriverManager.getConnection(urlWithDb, user, password);
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Unknown database") || msg.contains("n'existe pas"))) {
                createDatabaseIfMissing();
                c = DriverManager.getConnection(urlWithDb, user, password);
            } else {
                throw e;
            }
        }
        ensureUserTableExists(c);
        return c;
    }

    /**
     * Schéma minimal attendu par l'application (inscription / connexion / profils).
     */
    private void ensureUserTableExists(Connection c) {
        String sql = "CREATE TABLE IF NOT EXISTS `user` ("
                + "id INT PRIMARY KEY AUTO_INCREMENT, "
                + "nom VARCHAR(255) NOT NULL, "
                + "prenom VARCHAR(255) NOT NULL, "
                + "email VARCHAR(255) NOT NULL, "
                + "password VARCHAR(255) NOT NULL, "
                + "role VARCHAR(64) NOT NULL DEFAULT 'ROLE_PATIENT', "
                + "telephone VARCHAR(64), "
                + "sexe VARCHAR(32), "
                + "specialite VARCHAR(255), "
                + "dateNaissance DATE, "
                + "UNIQUE KEY uk_user_email (email)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        try (Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("ensureUserTableExists : " + e.getMessage());
        }
    }

    private void createDatabaseIfMissing() {
        try (Connection c = DriverManager.getConnection(urlNoDb, user, password);
             Statement st = c.createStatement()) {
            st.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `" + DB_NAME + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Base '" + DB_NAME + "' creee (ou deja presente).");
        } catch (SQLException e) {
            System.err.println("Impossible de creer la base '" + DB_NAME + "' : " + e.getMessage());
=======
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion a la base emonado_java reussie.");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL non trouve : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur de connexion a MySQL : " + e.getMessage());
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44
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
                connection = openConnectionWithAutoCreateDb();
                lastConnectionError = null;
            }
        } catch (SQLException e) {
<<<<<<< HEAD
            lastConnectionError = e.getMessage();
            System.err.println("Erreur lors de la recuperation de la connexion : " + lastConnectionError);
            connection = null;
=======
            System.err.println("Erreur lors de la recuperation de la connexion : " + e.getMessage());
>>>>>>> d0613d39f294842365d8edf17cb7726a89df6e44
        }
        return connection;
    }
}
