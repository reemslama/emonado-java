package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSource {
    private static final String HOST = "127.0.0.1";
    private static final String DATABASE = "emonado";
    private static final String SERVER_URL = "jdbc:mysql://" + HOST + ":3306/?serverTimezone=UTC";
    private static final String URL = "jdbc:mysql://" + HOST + ":3306/" + DATABASE + "?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static DataSource instance;

    private DataSource() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            ensureSchema();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver MySQL non trouve", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de l'initialisation de la base MySQL", e);
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
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la connexion a MySQL", e);
        }
    }

    private void ensureSchema() throws SQLException {
        try (Connection serverConnection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD)) {
            try (var statement = serverConnection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
            }
        }

        try (Connection databaseConnection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            createUserTable(databaseConnection);
            createQuestionTables(databaseConnection);
            createJournalTables(databaseConnection);
            createRendezVousTables(databaseConnection);
            createMedicalTables(databaseConnection);
            seedDefaultRendezVousTypes(databaseConnection);
        }
    }

    private void createUserTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS user (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(100) NOT NULL,
                    prenom VARCHAR(100) NOT NULL,
                    email VARCHAR(190) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    telephone VARCHAR(20),
                    sexe VARCHAR(20),
                    dateNaissance DATE,
                    specialite VARCHAR(150)
                )
                """);
    }

    private void createQuestionTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS question (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    texte TEXT NOT NULL,
                    ordre INT,
                    type_question VARCHAR(100),
                    categorie VARCHAR(100)
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS reponse (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    texte TEXT NOT NULL,
                    valeur INT NOT NULL,
                    ordre INT,
                    question_id INT NOT NULL,
                    CONSTRAINT fk_reponse_question
                        FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
                )
                """);
    }

    private void createJournalTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS journal (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    contenu TEXT NOT NULL,
                    humeur VARCHAR(100) NOT NULL,
                    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    user_id INT NOT NULL,
                    CONSTRAINT fk_journal_user
                        FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS analyse_emotionnelle (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    journal_id INT NOT NULL,
                    etat_emotionnel VARCHAR(100) NOT NULL,
                    niveau VARCHAR(100) NOT NULL,
                    declencheur TEXT,
                    conseil TEXT,
                    date_analyse TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_analyse_journal
                        FOREIGN KEY (journal_id) REFERENCES journal(id) ON DELETE CASCADE
                )
                """);
    }

    private void createRendezVousTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS type_rendez_vous (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    libelle VARCHAR(100) NOT NULL UNIQUE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS disponibilite (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    psychologue_id INT NULL,
                    date DATE NOT NULL,
                    heure_debut TIME NOT NULL,
                    heure_fin TIME NOT NULL,
                    est_libre INT NOT NULL DEFAULT 1,
                    CONSTRAINT fk_disponibilite_psychologue
                        FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE SET NULL
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS rendez_vous (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    age INT NOT NULL,
                    adresse VARCHAR(255) NOT NULL,
                    type_id INT NOT NULL,
                    dispo_id INT NOT NULL,
                    user_id INT NOT NULL,
                    statut VARCHAR(30) NOT NULL DEFAULT 'en attente',
                    notes_patient TEXT NULL,
                    notes_psychologue TEXT NULL,
                    CONSTRAINT fk_rendez_vous_type
                        FOREIGN KEY (type_id) REFERENCES type_rendez_vous(id) ON DELETE RESTRICT,
                    CONSTRAINT fk_rendez_vous_dispo
                        FOREIGN KEY (dispo_id) REFERENCES disponibilite(id) ON DELETE CASCADE,
                    CONSTRAINT fk_rendez_vous_user
                        FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);
    }

    private void createMedicalTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS dossier_medical (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    patient_id INT NOT NULL UNIQUE,
                    reminder_text TEXT,
                    medical_history TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_dossier_medical_user
                        FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS patient_medical_record (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    patient_id INT NOT NULL UNIQUE,
                    reminder_text TEXT,
                    medical_history TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_patient_medical_record_user
                        FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS antecedent_medical (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    dossier_medical_id INT NOT NULL,
                    type VARCHAR(100) NOT NULL,
                    description TEXT NOT NULL,
                    date_diagnostic DATE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_antecedent_medical_record
                        FOREIGN KEY (dossier_medical_id) REFERENCES dossier_medical(id) ON DELETE CASCADE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS patient_test_result (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    patient_id INT NOT NULL,
                    categorie VARCHAR(100) NOT NULL,
                    score INT NOT NULL,
                    score_max INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_patient_test_result_user
                        FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS patient_consultation (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    patient_id INT NOT NULL,
                    consultation_date DATE NOT NULL,
                    notes TEXT NOT NULL,
                    notes_psychologue TEXT NULL,
                    psychologue_id INT NULL,
                    rendez_vous_id INT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_patient_consultation_user
                        FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE,
                    CONSTRAINT fk_patient_consultation_psychologue
                        FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE SET NULL,
                    CONSTRAINT fk_patient_consultation_rendez_vous
                        FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id) ON DELETE SET NULL,
                    CONSTRAINT uk_patient_consultation_rendez_vous UNIQUE (rendez_vous_id)
                )
                """);
    }

    private void seedDefaultRendezVousTypes(Connection connection) throws SQLException {
        execute(connection, "INSERT IGNORE INTO type_rendez_vous(libelle) VALUES ('consultation')");
        execute(connection, "INSERT IGNORE INTO type_rendez_vous(libelle) VALUES ('suivi')");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
