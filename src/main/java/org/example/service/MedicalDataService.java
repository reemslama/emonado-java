package org.example.service;

import org.example.entities.AntecedentMedical;
import org.example.entities.Consultation;
import org.example.entities.DossierMedical;
import org.example.entities.TestResultMedical;
import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicalDataService {
    private static final String PRIMARY_MEDICAL_RECORD_TABLE = "dossier_medical";
    private static final String LEGACY_MEDICAL_RECORD_TABLE = "patient_medical_record";

    public void ensureSchema() throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection()) {
            if (conn == null) {
                throw new SQLException("Connexion a la base de donnees indisponible.");
            }

            execute(conn, "CREATE TABLE IF NOT EXISTS " + PRIMARY_MEDICAL_RECORD_TABLE + " ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "patient_id INT NOT NULL UNIQUE, "
                    + "reminder_text TEXT, "
                    + "medical_history TEXT, "
                    + "psychologue_note TEXT, "
                    + "psychologue_id INT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_dossier_medical_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE, "
                    + "CONSTRAINT fk_dossier_medical_psychologue FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE SET NULL"
                    + ")");

            execute(conn, "CREATE TABLE IF NOT EXISTS " + LEGACY_MEDICAL_RECORD_TABLE + " ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "patient_id INT NOT NULL UNIQUE, "
                    + "reminder_text TEXT, "
                    + "medical_history TEXT, "
                    + "psychologue_note TEXT, "
                    + "psychologue_id INT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_patient_medical_record_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE, "
                    + "CONSTRAINT fk_patient_medical_record_psychologue FOREIGN KEY (psychologue_id) REFERENCES user(id) ON DELETE SET NULL"
                    + ")");

            execute(conn, "CREATE TABLE IF NOT EXISTS patient_consultation ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "patient_id INT NOT NULL, "
                    + "consultation_date DATE NOT NULL, "
                    + "notes TEXT NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_patient_consultation_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE"
                    + ")");

            execute(conn, "CREATE TABLE IF NOT EXISTS antecedent_medical ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "dossier_medical_id INT NOT NULL, "
                    + "type VARCHAR(100) NOT NULL, "
                    + "description TEXT NOT NULL, "
                    + "date_diagnostic DATE, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_antecedent_medical_record FOREIGN KEY (dossier_medical_id) REFERENCES dossier_medical(id) ON DELETE CASCADE"
                    + ")");

            execute(conn, "CREATE TABLE IF NOT EXISTS patient_test_result ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "patient_id INT NOT NULL, "
                    + "categorie VARCHAR(100) NOT NULL, "
                    + "score INT NOT NULL, "
                    + "score_max INT NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_patient_test_result_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE"
                    + ")");

            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "reminder_text", "TEXT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "medical_history", "TEXT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "historique_medical", "LONGTEXT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "notes_psychologiques", "LONGTEXT NULL");
            addColumnIfMissing(conn, "patient_consultation", "notes_psychologue", "TEXT NULL");
            addColumnIfMissing(conn, "patient_consultation", "psychologue_id", "INT NULL");
            addColumnIfMissing(conn, "patient_consultation", "rendez_vous_id", "INT NULL");
            addColumnIfMissing(conn, "patient_consultation", "updated_at",
                    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "created_at",
                    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "updated_at",
                    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            addColumnIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "reminder_text", "TEXT NULL");
            addColumnIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "medical_history", "TEXT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "psychologue_note", "TEXT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "psychologue_id", "INT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "created_by_id", "INT NULL");
            addColumnIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "updated_by_id", "INT NULL");
            addColumnIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "created_at",
                    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            addColumnIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "updated_at",
                    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            addColumnIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "psychologue_note", "TEXT NULL");
            addColumnIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "psychologue_id", "INT NULL");
            addUniqueIndexIfMissing(conn, PRIMARY_MEDICAL_RECORD_TABLE, "uk_dossier_medical_patient", "patient_id");
            addUniqueIndexIfMissing(conn, LEGACY_MEDICAL_RECORD_TABLE, "uk_patient_medical_record_patient", "patient_id");
            addUniqueIndexIfMissing(conn, "patient_consultation", "uk_patient_consultation_rendez_vous", "rendez_vous_id");

            backfillMedicalRecordCompatColumns(conn);
            syncMedicalRecordTables(conn);
        }
    }

    public DossierMedical getMedicalRecordByPatient(int patientId) throws SQLException {
        DossierMedical primaryRecord = null;
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, reminder_text, medical_history, psychologue_note, psychologue_id, created_at, updated_at "
                             + "FROM " + PRIMARY_MEDICAL_RECORD_TABLE + " WHERE patient_id = ?")) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    primaryRecord = mapMedicalRecord(rs);
                }
            }
        }

        DossierMedical legacyRecord = null;
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, reminder_text, medical_history, psychologue_note, psychologue_id, created_at, updated_at "
                             + "FROM " + LEGACY_MEDICAL_RECORD_TABLE + " WHERE patient_id = ?")) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    legacyRecord = mapMedicalRecord(rs);
                }
            }
        }

        return mergeMedicalRecords(primaryRecord, legacyRecord);
    }

    public DossierMedical saveMedicalRecord(DossierMedical dossierMedical) throws SQLException {
        DossierMedical existing = getMedicalRecordByPatient(dossierMedical.getPatientId());
        if (existing == null) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO " + PRIMARY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, psychologue_note, psychologue_id, historique_medical, notes_psychologiques, created_at, updated_at, created_by_id, updated_by_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
                Integer actorId = dossierMedical.getPsychologueId() != null ? dossierMedical.getPsychologueId() : dossierMedical.getPatientId();
                pstmt.setInt(1, dossierMedical.getPatientId());
                pstmt.setString(2, dossierMedical.getReminderText());
                pstmt.setString(3, dossierMedical.getMedicalHistory());
                pstmt.setString(4, dossierMedical.getPsychologueNote());
                if (dossierMedical.getPsychologueId() == null) {
                    pstmt.setNull(5, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(5, dossierMedical.getPsychologueId());
                }
                pstmt.setString(6, dossierMedical.getMedicalHistory());
                pstmt.setString(7, dossierMedical.getPsychologueNote());
                pstmt.setTimestamp(8, now);
                pstmt.setTimestamp(9, now);
                if (actorId == null) {
                    pstmt.setNull(10, java.sql.Types.INTEGER);
                    pstmt.setNull(11, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(10, actorId);
                    pstmt.setInt(11, actorId);
                }
                pstmt.executeUpdate();
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        dossierMedical.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE + " SET reminder_text = ?, medical_history = ?, historique_medical = ?, updated_at = CURRENT_TIMESTAMP "
                                 + "WHERE patient_id = ?")) {
                pstmt.setString(1, dossierMedical.getReminderText());
                pstmt.setString(2, dossierMedical.getMedicalHistory());
                pstmt.setString(3, dossierMedical.getMedicalHistory());
                pstmt.setInt(4, dossierMedical.getPatientId());
                pstmt.executeUpdate();
            }
        }
        mirrorMedicalRecord(
                dossierMedical.getPatientId(),
                dossierMedical.getReminderText(),
                dossierMedical.getMedicalHistory(),
                dossierMedical.getPsychologueNote(),
                dossierMedical.getPsychologueId()
        );
        return getMedicalRecordByPatient(dossierMedical.getPatientId());
    }

    public void updateMedicalRecordPsychologueNote(int patientId, String note, Integer psychologueId) throws SQLException {
        int dossierId = getOrCreateMedicalRecordId(patientId);
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE + " SET psychologue_note = ?, notes_psychologiques = ?, psychologue_id = ?, updated_by_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            Integer actorId = psychologueId != null ? psychologueId : patientId;
            pstmt.setString(1, note);
            pstmt.setString(2, note);
            if (psychologueId == null) {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(3, psychologueId);
            }
            if (actorId == null) {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(4, actorId);
            }
            pstmt.setInt(5, dossierId);
            pstmt.executeUpdate();
        }
        DossierMedical record = getMedicalRecordByPatient(patientId);
        mirrorMedicalRecord(
                patientId,
                record == null ? "" : record.getReminderText(),
                record == null ? "" : record.getMedicalHistory(),
                note,
                psychologueId
        );
    }

    public void deleteMedicalRecord(int patientId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + PRIMARY_MEDICAL_RECORD_TABLE + " WHERE patient_id = ?")) {
            pstmt.setInt(1, patientId);
            pstmt.executeUpdate();
        }
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + LEGACY_MEDICAL_RECORD_TABLE + " WHERE patient_id = ?")) {
            pstmt.setInt(1, patientId);
            pstmt.executeUpdate();
        }
    }

    public AntecedentMedical saveAntecedent(int patientId, AntecedentMedical antecedentMedical) throws SQLException {
        int dossierId = getOrCreateMedicalRecordId(patientId);
        antecedentMedical.setDossierMedicalId(dossierId);

        if (antecedentMedical.getId() > 0) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE antecedent_medical SET type = ?, description = ?, date_diagnostic = ? WHERE id = ?")) {
                fillAntecedentStatement(pstmt, antecedentMedical);
                pstmt.setInt(4, antecedentMedical.getId());
                pstmt.executeUpdate();
            }
        } else {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO antecedent_medical (dossier_medical_id, type, description, date_diagnostic) VALUES (?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, dossierId);
                pstmt.setString(2, antecedentMedical.getType());
                pstmt.setString(3, antecedentMedical.getDescription());
                pstmt.setDate(4, antecedentMedical.getDateDiagnostic() == null ? null : Date.valueOf(antecedentMedical.getDateDiagnostic()));
                pstmt.executeUpdate();
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        antecedentMedical.setId(keys.getInt(1));
                    }
                }
            }
        }
        return antecedentMedical;
    }

    public void deleteAntecedent(int antecedentId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM antecedent_medical WHERE id = ?")) {
            pstmt.setInt(1, antecedentId);
            pstmt.executeUpdate();
        }
    }

    public List<AntecedentMedical> getAntecedentsByPatient(int patientId) throws SQLException {
        List<AntecedentMedical> antecedents = new ArrayList<>();
        DossierMedical dossierMedical = getMedicalRecordByPatient(patientId);
        if (dossierMedical == null) {
            return antecedents;
        }

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, type, description, date_diagnostic FROM antecedent_medical "
                             + "WHERE dossier_medical_id = ? ORDER BY date_diagnostic DESC, id DESC")) {
            pstmt.setInt(1, dossierMedical.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AntecedentMedical antecedentMedical = new AntecedentMedical();
                    antecedentMedical.setId(rs.getInt("id"));
                    antecedentMedical.setDossierMedicalId(dossierMedical.getId());
                    antecedentMedical.setType(rs.getString("type"));
                    antecedentMedical.setDescription(rs.getString("description"));
                    Date diagnosticDate = rs.getDate("date_diagnostic");
                    if (diagnosticDate != null) {
                        antecedentMedical.setDateDiagnostic(diagnosticDate.toLocalDate());
                    }
                    antecedents.add(antecedentMedical);
                }
            }
        }

        return antecedents;
    }

    public TestResultMedical saveTestResult(TestResultMedical testResult) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO patient_test_result (patient_id, categorie, score, score_max) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, testResult.getPatientId());
            pstmt.setString(2, testResult.getCategorie());
            pstmt.setInt(3, testResult.getScore());
            pstmt.setInt(4, testResult.getScoreMax());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    testResult.setId(keys.getInt(1));
                }
            }
        }
        return testResult;
    }

    public List<TestResultMedical> getTestResultsByPatient(int patientId) throws SQLException {
        List<TestResultMedical> results = new ArrayList<>();
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, categorie, score, score_max, created_at " +
                             "FROM patient_test_result WHERE patient_id = ? ORDER BY created_at DESC, id DESC")) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TestResultMedical result = new TestResultMedical();
                    result.setId(rs.getInt("id"));
                    result.setPatientId(rs.getInt("patient_id"));
                    result.setCategorie(rs.getString("categorie"));
                    result.setScore(rs.getInt("score"));
                    result.setScoreMax(rs.getInt("score_max"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        result.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    results.add(result);
                }
            }
        }
        return results;
    }

    public Consultation saveConsultation(Consultation consultation) throws SQLException {
        if (consultation.getId() > 0) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE patient_consultation SET consultation_date = ?, notes = ?, notes_psychologue = ?, psychologue_id = ?, rendez_vous_id = ?, "
                                 + "updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                pstmt.setDate(1, Date.valueOf(consultation.getConsultationDate()));
                pstmt.setString(2, consultation.getNotesPatient());
                pstmt.setString(3, consultation.getNotesPsychologue());
                if (consultation.getPsychologueId() == null) {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(4, consultation.getPsychologueId());
                }
                if (consultation.getRendezVousId() == null) {
                    pstmt.setNull(5, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(5, consultation.getRendezVousId());
                }
                pstmt.setInt(6, consultation.getId());
                pstmt.executeUpdate();
            }
        } else {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO patient_consultation (patient_id, consultation_date, notes, notes_psychologue, psychologue_id, rendez_vous_id) "
                                 + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, consultation.getPatientId());
                pstmt.setDate(2, Date.valueOf(consultation.getConsultationDate()));
                pstmt.setString(3, consultation.getNotesPatient());
                pstmt.setString(4, consultation.getNotesPsychologue());
                if (consultation.getPsychologueId() == null) {
                    pstmt.setNull(5, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(5, consultation.getPsychologueId());
                }
                if (consultation.getRendezVousId() == null) {
                    pstmt.setNull(6, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(6, consultation.getRendezVousId());
                }
                pstmt.executeUpdate();
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        consultation.setId(keys.getInt(1));
                    }
                }
            }
        }
        return getConsultationById(consultation.getId());
    }

    public void updatePsychologueNote(int consultationId, String note, Integer psychologueId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE patient_consultation SET notes_psychologue = ?, psychologue_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            pstmt.setString(1, note);
            if (psychologueId == null) {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(2, psychologueId);
            }
            pstmt.setInt(3, consultationId);
            pstmt.executeUpdate();
        }
    }

    public Consultation createConsultationFromAcceptedRendezVous(int rendezVousId) throws SQLException {
        Consultation existing = getConsultationByRendezVousId(rendezVousId);
        if (existing != null) {
            return existing;
        }

        String sql = "SELECT r.id AS rendez_vous_id, r.user_id AS patient_id, d.date AS consultation_date, d.psychologue_id " +
                "FROM rendez_vous r " +
                "JOIN disponibilite d ON d.id = r.dispo_id " +
                "WHERE r.id = ? AND r.statut = 'acceptee'";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rendezVousId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Consultation consultation = new Consultation();
                consultation.setPatientId(rs.getInt("patient_id"));
                consultation.setRendezVousId(rs.getInt("rendez_vous_id"));
                consultation.setConsultationDate(rs.getDate("consultation_date").toLocalDate());
                consultation.setNotesPatient("");
                consultation.setNotesPsychologue("");
                int psychologueId = rs.getInt("psychologue_id");
                if (!rs.wasNull()) {
                    consultation.setPsychologueId(psychologueId);
                }
                return saveConsultation(consultation);
            }
        }
    }

    public void deleteConsultation(int consultationId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM patient_consultation WHERE id = ?")) {
            pstmt.setInt(1, consultationId);
            pstmt.executeUpdate();
        }
    }

    public Consultation getConsultationById(int consultationId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, consultation_date, notes, notes_psychologue, psychologue_id, rendez_vous_id, created_at, updated_at "
                             + "FROM patient_consultation WHERE id = ?")) {
            pstmt.setInt(1, consultationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapConsultation(rs);
                }
            }
        }
        return null;
    }

    public Consultation getConsultationByRendezVousId(int rendezVousId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, consultation_date, notes, notes_psychologue, psychologue_id, rendez_vous_id, created_at, updated_at "
                             + "FROM patient_consultation WHERE rendez_vous_id = ?")) {
            pstmt.setInt(1, rendezVousId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapConsultation(rs);
                }
            }
        }
        return null;
    }

    public List<Consultation> getConsultationsByPatient(int patientId) throws SQLException {
        List<Consultation> consultations = new ArrayList<>();
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, consultation_date, notes, notes_psychologue, psychologue_id, rendez_vous_id, created_at, updated_at "
                             + "FROM patient_consultation WHERE patient_id = ? ORDER BY consultation_date DESC, id DESC")) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    consultations.add(mapConsultation(rs));
                }
            }
        }
        return consultations;
    }

    public List<Consultation> getConsultationsByPsychologue(int psychologueId) throws SQLException {
        List<Consultation> consultations = new ArrayList<>();
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, consultation_date, notes, notes_psychologue, psychologue_id, rendez_vous_id, created_at, updated_at "
                             + "FROM patient_consultation WHERE psychologue_id = ? ORDER BY consultation_date DESC, id DESC")) {
            pstmt.setInt(1, psychologueId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    consultations.add(mapConsultation(rs));
                }
            }
        }
        return consultations;
    }

    public List<User> getAllPatients() throws SQLException {
        List<User> patients = new ArrayList<>();
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, nom, prenom, email, roles, telephone, sexe, specialite, has_child, avatar, face_id_image_path, date_naissance "
                             + "FROM user WHERE UPPER(roles) LIKE '%\"ROLE_PATIENT\"%' ORDER BY nom, prenom")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapUser(rs));
                }
            }
        }
        return patients;
    }

    public List<User> getPatientsForPsychologue(int psychologueId) throws SQLException {
        List<User> patients = new ArrayList<>();
        String sql = "SELECT DISTINCT u.id, u.nom, u.prenom, u.email, u.roles, u.telephone, u.sexe, u.specialite, u.has_child, u.avatar, u.face_id_image_path, u.date_naissance, u.reset_password_token, u.reset_password_token_expires_at, u.psychologue_id "
                + "FROM user u "
                + "WHERE UPPER(u.roles) LIKE '%\"ROLE_PATIENT\"%' AND ("
                + "u.id IN ("
                + "SELECT pc.patient_id "
                + "FROM patient_consultation pc "
                + "WHERE pc.psychologue_id = ?"
                + ") "
                + "OR u.id IN ("
                + "SELECT r.user_id "
                + "FROM rendez_vous r "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "WHERE d.psychologue_id = ? AND r.statut = 'acceptee'"
                + ")"
                + ") "
                + "ORDER BY u.nom, u.prenom";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, psychologueId);
            pstmt.setInt(2, psychologueId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapUser(rs));
                }
            }
        }
        return patients;
    }

    private int getOrCreateMedicalRecordId(int patientId) throws SQLException {
        DossierMedical dossierMedical = getMedicalRecordByPatient(patientId);
        if (dossierMedical != null) {
            return dossierMedical.getId();
        }

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO " + PRIMARY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, psychologue_note, psychologue_id, historique_medical, notes_psychologiques, created_at, updated_at, created_by_id, updated_by_id) VALUES (?, '', '', NULL, NULL, '', NULL, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
            pstmt.setInt(1, patientId);
            pstmt.setTimestamp(2, now);
            pstmt.setTimestamp(3, now);
            pstmt.setInt(4, patientId);
            pstmt.setInt(5, patientId);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    mirrorMedicalRecord(patientId, "", "", null, null);
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Impossible de creer le dossier medical.");
    }

    private void fillAntecedentStatement(PreparedStatement pstmt, AntecedentMedical antecedentMedical) throws SQLException {
        pstmt.setString(1, antecedentMedical.getType());
        pstmt.setString(2, antecedentMedical.getDescription());
        pstmt.setDate(3, antecedentMedical.getDateDiagnostic() == null ? null : Date.valueOf(antecedentMedical.getDateDiagnostic()));
    }

    private DossierMedical mapMedicalRecord(ResultSet rs) throws SQLException {
        DossierMedical dossierMedical = new DossierMedical();
        dossierMedical.setId(rs.getInt("id"));
        dossierMedical.setPatientId(rs.getInt("patient_id"));
        dossierMedical.setReminderText(rs.getString("reminder_text"));
        String medicalHistory = rs.getString("medical_history");
        if (isBlank(medicalHistory)) {
            medicalHistory = getOptionalString(rs, "historique_medical");
        }
        dossierMedical.setMedicalHistory(medicalHistory);
        String psychologueNote = rs.getString("psychologue_note");
        if (isBlank(psychologueNote)) {
            psychologueNote = getOptionalString(rs, "notes_psychologiques");
        }
        dossierMedical.setPsychologueNote(psychologueNote);
        int psychologueId = rs.getInt("psychologue_id");
        if (!rs.wasNull()) {
            dossierMedical.setPsychologueId(psychologueId);
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            dossierMedical.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            dossierMedical.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return dossierMedical;
    }

    private Consultation mapConsultation(ResultSet rs) throws SQLException {
        Consultation consultation = new Consultation();
        consultation.setId(rs.getInt("id"));
        consultation.setPatientId(rs.getInt("patient_id"));
        consultation.setConsultationDate(rs.getDate("consultation_date").toLocalDate());
        consultation.setNotesPatient(rs.getString("notes"));
        consultation.setNotesPsychologue(rs.getString("notes_psychologue"));
        int psychologueId = rs.getInt("psychologue_id");
        if (!rs.wasNull()) {
            consultation.setPsychologueId(psychologueId);
        }
        int rendezVousId = rs.getInt("rendez_vous_id");
        if (!rs.wasNull()) {
            consultation.setRendezVousId(rendezVousId);
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            consultation.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            consultation.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return consultation;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setRoles(getOptionalString(rs, "roles"));
        user.setTelephone(rs.getString("telephone"));
        user.setSexe(rs.getString("sexe"));
        user.setSpecialite(rs.getString("specialite"));
        user.setHasChild(rs.getBoolean("has_child"));
        user.setAvatar(getOptionalString(rs, "avatar"));
        user.setFaceIdImagePath(getOptionalString(rs, "face_id_image_path"));
        user.setResetPasswordToken(getOptionalString(rs, "reset_password_token"));
        Timestamp resetExpiresAt = getOptionalTimestamp(rs, "reset_password_token_expires_at");
        if (resetExpiresAt != null) {
            user.setResetPasswordTokenExpiresAt(resetExpiresAt.toLocalDateTime());
        }
        Integer psychologueId = getOptionalInt(rs, "psychologue_id");
        if (psychologueId != null) {
            user.setPsychologueId(psychologueId);
        }
        Date birthDate = rs.getDate("date_naissance");
        if (birthDate != null) {
            user.setdate_naissance(birthDate.toLocalDate());
        }
        return user;
    }

    private String getOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Timestamp getOptionalTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Integer getOptionalInt(ResultSet rs, String column) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }

    private void execute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void syncMedicalRecordTables(Connection conn) throws SQLException {
        execute(conn, "INSERT INTO " + PRIMARY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, psychologue_note, psychologue_id, historique_medical, notes_psychologiques, created_at, updated_at, created_by_id, updated_by_id) "
                + "SELECT pmr.patient_id, pmr.reminder_text, pmr.medical_history, pmr.psychologue_note, pmr.psychologue_id, pmr.medical_history, pmr.psychologue_note, "
                + "COALESCE(pmr.created_at, CURRENT_TIMESTAMP), COALESCE(pmr.updated_at, CURRENT_TIMESTAMP), "
                + "COALESCE(pmr.psychologue_id, pmr.patient_id), pmr.psychologue_id "
                + "FROM " + LEGACY_MEDICAL_RECORD_TABLE + " pmr "
                + "LEFT JOIN " + PRIMARY_MEDICAL_RECORD_TABLE + " dm ON dm.patient_id = pmr.patient_id "
                + "WHERE dm.patient_id IS NULL");

        execute(conn, "INSERT INTO " + LEGACY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, psychologue_note, psychologue_id, created_at, updated_at) "
                + "SELECT dm.patient_id, dm.reminder_text, "
                + "COALESCE(NULLIF(dm.medical_history, ''), dm.historique_medical), "
                + "COALESCE(NULLIF(dm.psychologue_note, ''), dm.notes_psychologiques), "
                + "dm.psychologue_id, dm.created_at, dm.updated_at "
                + "FROM " + PRIMARY_MEDICAL_RECORD_TABLE + " dm "
                + "LEFT JOIN " + LEGACY_MEDICAL_RECORD_TABLE + " pmr ON pmr.patient_id = dm.patient_id "
                + "WHERE pmr.patient_id IS NULL");
    }

    private void mirrorMedicalRecord(int patientId, String reminderText, String medicalHistory, String psychologueNote, Integer psychologueId) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO " + LEGACY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, psychologue_note, psychologue_id) VALUES (?, ?, ?, ?, ?) "
                             + "ON DUPLICATE KEY UPDATE reminder_text = VALUES(reminder_text), medical_history = VALUES(medical_history), "
                             + "psychologue_note = VALUES(psychologue_note), psychologue_id = VALUES(psychologue_id), updated_at = CURRENT_TIMESTAMP")) {
            pstmt.setInt(1, patientId);
            pstmt.setString(2, reminderText);
            pstmt.setString(3, medicalHistory);
            pstmt.setString(4, psychologueNote);
            if (psychologueId == null) {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(5, psychologueId);
            }
            pstmt.executeUpdate();
        }
    }

    private void backfillMedicalRecordCompatColumns(Connection conn) throws SQLException {
        execute(conn, "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE
                + " SET medical_history = historique_medical "
                + "WHERE (medical_history IS NULL OR medical_history = '') "
                + "AND historique_medical IS NOT NULL AND historique_medical <> ''");

        execute(conn, "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE
                + " SET historique_medical = medical_history "
                + "WHERE (historique_medical IS NULL OR historique_medical = '') "
                + "AND medical_history IS NOT NULL AND medical_history <> ''");

        execute(conn, "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE
                + " SET psychologue_note = notes_psychologiques "
                + "WHERE (psychologue_note IS NULL OR psychologue_note = '') "
                + "AND notes_psychologiques IS NOT NULL AND notes_psychologiques <> ''");

        execute(conn, "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE
                + " SET notes_psychologiques = psychologue_note "
                + "WHERE (notes_psychologiques IS NULL OR notes_psychologiques = '') "
                + "AND psychologue_note IS NOT NULL AND psychologue_note <> ''");

        execute(conn, "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE
                + " SET created_by_id = patient_id "
                + "WHERE created_by_id IS NULL");
    }

    private DossierMedical mergeMedicalRecords(DossierMedical primaryRecord, DossierMedical legacyRecord) {
        if (primaryRecord == null) {
            return legacyRecord;
        }
        if (legacyRecord == null) {
            return primaryRecord;
        }

        if (isBlank(primaryRecord.getReminderText()) && !isBlank(legacyRecord.getReminderText())) {
            primaryRecord.setReminderText(legacyRecord.getReminderText());
        }
        if (isBlank(primaryRecord.getMedicalHistory()) && !isBlank(legacyRecord.getMedicalHistory())) {
            primaryRecord.setMedicalHistory(legacyRecord.getMedicalHistory());
        }
        if (isBlank(primaryRecord.getPsychologueNote()) && !isBlank(legacyRecord.getPsychologueNote())) {
            primaryRecord.setPsychologueNote(legacyRecord.getPsychologueNote());
        }
        if (primaryRecord.getPsychologueId() == null && legacyRecord.getPsychologueId() != null) {
            primaryRecord.setPsychologueId(legacyRecord.getPsychologueId());
        }
        if (primaryRecord.getCreatedAt() == null) {
            primaryRecord.setCreatedAt(legacyRecord.getCreatedAt());
        }
        if (primaryRecord.getUpdatedAt() == null) {
            primaryRecord.setUpdatedAt(legacyRecord.getUpdatedAt());
        }
        return primaryRecord;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void addColumnIfMissing(Connection conn, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (columnExists(conn, tableName, columnName)) {
            return;
        }
        execute(conn, "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void addUniqueIndexIfMissing(Connection conn, String tableName, String indexName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(conn.getCatalog(), null, tableName, true, false)) {
            while (rs.next()) {
                String existingIndex = rs.getString("INDEX_NAME");
                if (existingIndex != null && existingIndex.equalsIgnoreCase(indexName)) {
                    return;
                }
            }
        }
        execute(conn, "ALTER TABLE " + tableName + " ADD UNIQUE INDEX " + indexName + " (" + columnName + ")");
    }
}
