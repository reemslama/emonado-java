package org.example.service;

import org.example.entities.AntecedentMedical;
import org.example.entities.Consultation;
import org.example.entities.DossierMedical;
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
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_dossier_medical_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE"
                    + ")");

            execute(conn, "CREATE TABLE IF NOT EXISTS " + LEGACY_MEDICAL_RECORD_TABLE + " ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "patient_id INT NOT NULL UNIQUE, "
                    + "reminder_text TEXT, "
                    + "medical_history TEXT, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_patient_medical_record_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE"
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

            addColumnIfMissing(conn, "patient_consultation", "notes_psychologue", "TEXT NULL");
            addColumnIfMissing(conn, "patient_consultation", "psychologue_id", "INT NULL");
            addColumnIfMissing(conn, "patient_consultation", "updated_at",
                    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            syncMedicalRecordTables(conn);
        }
    }

    public DossierMedical getMedicalRecordByPatient(int patientId) throws SQLException {
        DossierMedical primaryRecord = null;
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, reminder_text, medical_history, created_at, updated_at "
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
                     "SELECT id, patient_id, reminder_text, medical_history, created_at, updated_at "
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
                         "INSERT INTO " + PRIMARY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history) VALUES (?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, dossierMedical.getPatientId());
                pstmt.setString(2, dossierMedical.getReminderText());
                pstmt.setString(3, dossierMedical.getMedicalHistory());
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
                         "UPDATE " + PRIMARY_MEDICAL_RECORD_TABLE + " SET reminder_text = ?, medical_history = ?, updated_at = CURRENT_TIMESTAMP "
                                 + "WHERE patient_id = ?")) {
                pstmt.setString(1, dossierMedical.getReminderText());
                pstmt.setString(2, dossierMedical.getMedicalHistory());
                pstmt.setInt(3, dossierMedical.getPatientId());
                pstmt.executeUpdate();
            }
        }
        mirrorMedicalRecord(dossierMedical.getPatientId(), dossierMedical.getReminderText(), dossierMedical.getMedicalHistory());
        return getMedicalRecordByPatient(dossierMedical.getPatientId());
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

    public Consultation saveConsultation(Consultation consultation) throws SQLException {
        if (consultation.getId() > 0) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE patient_consultation SET consultation_date = ?, notes = ?, notes_psychologue = ?, psychologue_id = ?, "
                                 + "updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                pstmt.setDate(1, Date.valueOf(consultation.getConsultationDate()));
                pstmt.setString(2, consultation.getNotesPatient());
                pstmt.setString(3, consultation.getNotesPsychologue());
                if (consultation.getPsychologueId() == null) {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(4, consultation.getPsychologueId());
                }
                pstmt.setInt(5, consultation.getId());
                pstmt.executeUpdate();
            }
        } else {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO patient_consultation (patient_id, consultation_date, notes, notes_psychologue, psychologue_id) "
                                 + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, consultation.getPatientId());
                pstmt.setDate(2, Date.valueOf(consultation.getConsultationDate()));
                pstmt.setString(3, consultation.getNotesPatient());
                pstmt.setString(4, consultation.getNotesPsychologue());
                if (consultation.getPsychologueId() == null) {
                    pstmt.setNull(5, java.sql.Types.INTEGER);
                } else {
                    pstmt.setInt(5, consultation.getPsychologueId());
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
                     "SELECT id, patient_id, consultation_date, notes, notes_psychologue, psychologue_id, created_at, updated_at "
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

    public List<Consultation> getConsultationsByPatient(int patientId) throws SQLException {
        List<Consultation> consultations = new ArrayList<>();
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, patient_id, consultation_date, notes, notes_psychologue, psychologue_id, created_at, updated_at "
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

    private int getOrCreateMedicalRecordId(int patientId) throws SQLException {
        DossierMedical dossierMedical = getMedicalRecordByPatient(patientId);
        if (dossierMedical != null) {
            return dossierMedical.getId();
        }

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO " + PRIMARY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history) VALUES (?, '', '')",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, patientId);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    mirrorMedicalRecord(patientId, "", "");
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
        dossierMedical.setMedicalHistory(rs.getString("medical_history"));
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

    private void execute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void syncMedicalRecordTables(Connection conn) throws SQLException {
        execute(conn, "INSERT INTO " + PRIMARY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, created_at, updated_at) "
                + "SELECT pmr.patient_id, pmr.reminder_text, pmr.medical_history, pmr.created_at, pmr.updated_at "
                + "FROM " + LEGACY_MEDICAL_RECORD_TABLE + " pmr "
                + "LEFT JOIN " + PRIMARY_MEDICAL_RECORD_TABLE + " dm ON dm.patient_id = pmr.patient_id "
                + "WHERE dm.patient_id IS NULL");

        execute(conn, "INSERT INTO " + LEGACY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history, created_at, updated_at) "
                + "SELECT dm.patient_id, dm.reminder_text, dm.medical_history, dm.created_at, dm.updated_at "
                + "FROM " + PRIMARY_MEDICAL_RECORD_TABLE + " dm "
                + "LEFT JOIN " + LEGACY_MEDICAL_RECORD_TABLE + " pmr ON pmr.patient_id = dm.patient_id "
                + "WHERE pmr.patient_id IS NULL");
    }

    private void mirrorMedicalRecord(int patientId, String reminderText, String medicalHistory) throws SQLException {
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO " + LEGACY_MEDICAL_RECORD_TABLE + " (patient_id, reminder_text, medical_history) VALUES (?, ?, ?) "
                             + "ON DUPLICATE KEY UPDATE reminder_text = VALUES(reminder_text), medical_history = VALUES(medical_history), updated_at = CURRENT_TIMESTAMP")) {
            pstmt.setInt(1, patientId);
            pstmt.setString(2, reminderText);
            pstmt.setString(3, medicalHistory);
            pstmt.executeUpdate();
        }
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
}
