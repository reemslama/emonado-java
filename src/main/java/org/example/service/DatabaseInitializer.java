package org.example.service;

import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() {
        try {
            Connection connection = DataSource.getInstance().getConnection();
            ensureUserTable(connection);
            ensureJournalTable(connection);
            ensureAnalyseEmotionnelleTable(connection);
            ensureQuestionTable(connection);
            ensureReponseTable(connection);
            seedDefaultQuestions(connection);
            ensureRendezVousTables(connection);
            ensureConsultationWorkflowTables(connection);
            seedDefaultRendezVousTypes(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser le schema MySQL.", e);
        }

        try {
            new MedicalDataService().ensureSchema();
            new ServiceJeu();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser les tables medicales.", e);
        }
    }

    private static void ensureUserTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS user (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    nom VARCHAR(100) NOT NULL,
                    prenom VARCHAR(100) NOT NULL,
                    email VARCHAR(150) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    roles JSON NOT NULL,
                    telephone VARCHAR(30),
                    sexe VARCHAR(20),
                    date_naissance DATE,
                    specialite VARCHAR(150),
                    avatar VARCHAR(255) NULL,
                    face_id_image_path VARCHAR(255) NULL,
                    has_child TINYINT(1) NOT NULL DEFAULT 0,
                    reset_password_token VARCHAR(255) NULL,
                    reset_password_token_expires_at TIMESTAMP NULL,
                    psychologue_id INT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT uq_user_email UNIQUE (email),
                    CONSTRAINT fk_user_psychologue FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE SET NULL
                )
                """);

        addColumnIfMissing(connection, "user", "telephone", "VARCHAR(30) NULL");
        addColumnIfMissing(connection, "user", "sexe", "VARCHAR(20) NULL");
        addColumnIfMissing(connection, "user", "roles", "JSON NULL");
        addColumnIfMissing(connection, "user", "date_naissance", "DATE NULL");
        addColumnIfMissing(connection, "user", "specialite", "VARCHAR(150) NULL");
        addColumnIfMissing(connection, "user", "avatar", "VARCHAR(255) NULL");
        addColumnIfMissing(connection, "user", "face_id_image_path", "VARCHAR(255) NULL");
        addColumnIfMissing(connection, "user", "has_child", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "user", "reset_password_token", "VARCHAR(255) NULL");
        addColumnIfMissing(connection, "user", "reset_password_token_expires_at", "TIMESTAMP NULL");
        addColumnIfMissing(connection, "user", "psychologue_id", "INT NULL");
    }

    private static void ensureJournalTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS journal (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    contenu TEXT NOT NULL,
                    humeur VARCHAR(50) NOT NULL,
                    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    user_id INT NOT NULL,
                    CONSTRAINT fk_journal_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);
    }

    private static void ensureAnalyseEmotionnelleTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS analyse_emotionnelle (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    journal_id INT NOT NULL,
                    etat_emotionnel VARCHAR(120) NOT NULL,
                    niveau VARCHAR(120) NOT NULL,
                    declencheur TEXT,
                    conseil TEXT,
                    date_analyse TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_analyse_journal FOREIGN KEY (journal_id) REFERENCES journal(id) ON DELETE CASCADE
                )
                """);
    }

    private static void ensureQuestionTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS question (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    texte TEXT NOT NULL,
                    ordre INT NOT NULL DEFAULT 0,
                    type_question VARCHAR(100) NOT NULL,
                    categorie VARCHAR(100) NOT NULL
                )
                """);
    }

    private static void ensureReponseTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS reponse (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    texte TEXT NOT NULL,
                    valeur INT NOT NULL DEFAULT 0,
                    ordre INT NOT NULL DEFAULT 0,
                    question_id INT NOT NULL,
                    CONSTRAINT fk_reponse_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
                )
                """);
    }

    private static void seedDefaultQuestions(Connection connection) throws SQLException {
        seedCategory(connection, "stress", "Stress", new String[]{
                "Au cours du dernier mois, a quelle frequence avez-vous ete contrarie par un evenement inattendu ?",
                "Au cours du dernier mois, a quelle frequence avez-vous eu le sentiment de ne pas pouvoir controler les choses importantes de votre vie ?",
                "Au cours du dernier mois, a quelle frequence vous etes-vous senti nerveux ou stresse ?",
                "Au cours du dernier mois, a quelle frequence avez-vous gere avec succes les irritations quotidiennes ?",
                "Au cours du dernier mois, a quelle frequence avez-vous senti que les choses allaient dans votre sens ?",
                "Au cours du dernier mois, a quelle frequence avez-vous eu le sentiment de ne pas pouvoir faire face a toutes vos obligations ?",
                "Au cours du dernier mois, a quelle frequence avez-vous pu controler les sources d'irritation dans votre vie ?",
                "Au cours du dernier mois, a quelle frequence avez-vous senti que vous maitrisiez la situation ?",
                "Au cours du dernier mois, a quelle frequence avez-vous ete irrite par des choses hors de votre controle ?",
                "Au cours du dernier mois, a quelle frequence avez-vous eu le sentiment que les difficultes s'accumulaient trop ?"
        });
        seedCategory(connection, "depression", "Depression", new String[]{
                "Peu d'interet ou de plaisir a faire les choses ?",
                "Sentiment de tristesse, de depression ou de desespoir ?",
                "Difficultes a vous endormir, sommeil interrompu ou sommeil excessif ?",
                "Fatigue ou manque d'energie ?",
                "Manque d'appetit ou tendance a trop manger ?",
                "Mauvaise opinion de vous-meme, impression d'etre un echec ?",
                "Difficultes a vous concentrer sur des activites comme lire ou regarder quelque chose ?",
                "Ralentissement ou agitation remarquee par les autres ?",
                "Pensees que vous seriez mieux mort ou de vous faire du mal ?"
        });
        seedCategory(connection, "anxiete", "Anxiete", new String[]{
                "Sentiment de nervosite, d'anxiete ou de tension ?",
                "Incapacite a arreter ou controler vos inquietudes ?",
                "Inquietudes excessives a propos de choses differentes ?",
                "Difficulte a vous detendre ?",
                "Agitation telle qu'il est difficile de rester tranquille ?",
                "Irritabilite facile ?",
                "Peur que quelque chose de terrible puisse arriver ?"
        });
        seedCategory(connection, "iq", "QI", new String[]{
                "Quel nombre complete la suite : 2, 4, 8, 16, ?",
                "Si tous les chats sont des animaux et que Felix est un chat, que peut-on conclure ?",
                "Quel mot ne va pas avec les autres : rouge, bleu, table, vert ?",
                "Combien font 15 + 27 ?",
                "Quelle forme a trois cotes ?"
        }, new String[][]{
                {"24", "0", "32", "3", "30", "1", "18", "0"},
                {"Felix est un animal", "3", "Felix est un chien", "0", "Tous les animaux sont des chats", "0", "Aucune conclusion", "1"},
                {"table", "3", "rouge", "0", "bleu", "0", "vert", "0"},
                {"42", "3", "40", "1", "41", "2", "45", "0"},
                {"triangle", "3", "carre", "0", "cercle", "0", "rectangle", "0"}
        });
    }

    private static void seedCategory(Connection connection, String categorie, String typeQuestion, String[] questions)
            throws SQLException {
        String[][] responses = new String[questions.length][];
        for (int i = 0; i < questions.length; i++) {
            responses[i] = new String[]{
                    "Jamais", "0",
                    "Plusieurs jours", "1",
                    "Plus de la moitie des jours", "2",
                    "Presque tous les jours", "3"
            };
        }
        seedCategory(connection, categorie, typeQuestion, questions, responses);
    }

    private static void seedCategory(Connection connection,
                                     String categorie,
                                     String typeQuestion,
                                     String[] questions,
                                     String[][] responses) throws SQLException {
        if (countQuestions(connection, categorie) > 0) {
            return;
        }

        String questionSql = "INSERT INTO question (texte, ordre, type_question, categorie) VALUES (?, ?, ?, ?)";
        String responseSql = "INSERT INTO reponse (texte, valeur, ordre, question_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement questionStatement = connection.prepareStatement(questionSql, PreparedStatement.RETURN_GENERATED_KEYS);
             PreparedStatement responseStatement = connection.prepareStatement(responseSql)) {
            for (int i = 0; i < questions.length; i++) {
                questionStatement.setString(1, questions[i]);
                questionStatement.setInt(2, i + 1);
                questionStatement.setString(3, typeQuestion);
                questionStatement.setString(4, categorie);
                questionStatement.executeUpdate();

                int questionId;
                try (ResultSet keys = questionStatement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Impossible de recuperer l'id de la question inseree.");
                    }
                    questionId = keys.getInt(1);
                }

                String[] responseValues = responses[i];
                for (int r = 0; r < responseValues.length; r += 2) {
                    responseStatement.setString(1, responseValues[r]);
                    responseStatement.setInt(2, Integer.parseInt(responseValues[r + 1]));
                    responseStatement.setInt(3, (r / 2) + 1);
                    responseStatement.setInt(4, questionId);
                    responseStatement.addBatch();
                }
                responseStatement.executeBatch();
            }
        }
    }

    private static int countQuestions(Connection connection, String categorie) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM question WHERE categorie = ?")) {
            statement.setString(1, categorie);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void ensureRendezVousTables(Connection connection) throws SQLException {
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
                    latitude DOUBLE NULL,
                    longitude DOUBLE NULL,
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
        addColumnIfMissing(connection, "rendez_vous", "latitude", "DOUBLE NULL");
        addColumnIfMissing(connection, "rendez_vous", "longitude", "DOUBLE NULL");
    }

    private static void ensureConsultationWorkflowTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS consultation_payment (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    rendez_vous_id INT NOT NULL,
                    consultation_id INT NULL,
                    patient_id INT NOT NULL,
                    psychologue_id INT NOT NULL,
                    stripe_session_id VARCHAR(255) NULL,
                    stripe_payment_intent_id VARCHAR(255) NULL,
                    checkout_url TEXT NULL,
                    amount_cents INT NOT NULL DEFAULT 0,
                    currency VARCHAR(10) NOT NULL DEFAULT 'eur',
                    status VARCHAR(30) NOT NULL DEFAULT 'pending',
                    paid_at TIMESTAMP NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT uq_consultation_payment_rendez_vous UNIQUE (rendez_vous_id),
                    CONSTRAINT fk_consultation_payment_rendez_vous FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id) ON DELETE CASCADE,
                    CONSTRAINT fk_consultation_payment_patient FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE,
                    CONSTRAINT fk_consultation_payment_psychologue FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);

        execute(connection, """
                CREATE TABLE IF NOT EXISTS consultation_questionnaire (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    payment_id INT NOT NULL,
                    rendez_vous_id INT NOT NULL,
                    patient_id INT NOT NULL,
                    psychologue_id INT NOT NULL,
                    chief_complaint TEXT NOT NULL,
                    symptom_summary TEXT NOT NULL,
                    stress_level INT NOT NULL DEFAULT 0,
                    anxiety_level INT NOT NULL DEFAULT 0,
                    mood_level INT NOT NULL DEFAULT 0,
                    sleep_quality INT NOT NULL DEFAULT 0,
                    energy_level INT NOT NULL DEFAULT 0,
                    support_level INT NOT NULL DEFAULT 0,
                    urgency_level INT NOT NULL DEFAULT 0,
                    self_harm_risk VARCHAR(30) NOT NULL DEFAULT 'none',
                    additional_context TEXT NULL,
                    voice_transcript TEXT NULL,
                    risk_score INT NOT NULL DEFAULT 0,
                    predicted_state VARCHAR(255) NOT NULL DEFAULT '',
                    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT uq_consultation_questionnaire_payment UNIQUE (payment_id),
                    CONSTRAINT fk_consultation_questionnaire_payment FOREIGN KEY (payment_id) REFERENCES consultation_payment(id) ON DELETE CASCADE,
                    CONSTRAINT fk_consultation_questionnaire_rendez_vous FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id) ON DELETE CASCADE,
                    CONSTRAINT fk_consultation_questionnaire_patient FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE,
                    CONSTRAINT fk_consultation_questionnaire_psychologue FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE CASCADE
                )
                """);
        addColumnIfMissing(connection, "consultation_questionnaire", "voice_transcript", "TEXT NULL");
    }

    private static void seedDefaultRendezVousTypes(Connection connection) throws SQLException {
        execute(connection, "INSERT IGNORE INTO type_rendez_vous(libelle) VALUES ('consultation')");
        execute(connection, "INSERT IGNORE INTO type_rendez_vous(libelle) VALUES ('suivi')");
    }

    private static void addColumnIfMissing(Connection connection, String tableName, String columnName, String definition)
            throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
