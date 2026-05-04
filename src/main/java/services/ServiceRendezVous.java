package services;

import entities.RendezVous;
import entities.RendezVousPsy;
import org.example.service.EmailService;
import org.example.service.ConsultationWorkflowService;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.service.GoogleCalendarSyncService;
import org.example.service.RendezVousReminderService;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {
    private static final String STATUT_EN_ATTENTE = "en attente";
    private static final String STATUT_ACCEPTEE = "acceptee";
    private static final String STATUT_REJETEE = "rejetee";

    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();
    private final MedicalDataService medicalDataService = new MedicalDataService();
    private final EmailService emailService = new EmailService();
    private final ConsultationWorkflowService consultationWorkflowService = new ConsultationWorkflowService();
    private final RendezVousReminderService reminderService = new RendezVousReminderService();
    private final GoogleCalendarSyncService googleCalendarSyncService = new GoogleCalendarSyncService();
    private String lastValidationError = "";
    private String lastSuccessMessage = "";

    public ServiceRendezVous() {
        ensureSchema();
    }

    private Connection getConnection() {
        return DataSource.getInstance().getConnection();
    }

    public boolean ajouter(RendezVous rendezVous) {
        if (rendezVous == null) {
            lastValidationError = "Donnees du rendez-vous invalides";
            return false;
        }
        if (!typeAutorise(rendezVous.getTypeId())) {
            lastValidationError = "Type de rendez-vous invalide";
            return false;
        }
        String patientNoteError = MedicalValidationService.validatePatientRendezVousNote(rendezVous.getNotesPatient());
        if (patientNoteError != null) {
            lastValidationError = patientNoteError;
            return false;
        }

        String sql = "INSERT INTO rendez_vous(age, adresse, latitude, longitude, type_id, dispo_id, user_id, statut, notes_patient, notes_psychologue) VALUES (?,?,?,?,?,?,?,?,?,?)";
        boolean initialAutoCommit = true;
        Connection cnx = getConnection();
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            ValidationReservation validation = validerReservation(cnx, rendezVous.getUserId(), rendezVous.getDispoId(), null, null);
            if (!validation.allowed) {
                cnx.rollback();
                lastValidationError = validation.message;
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, rendezVous.getAge());
                ps.setString(2, rendezVous.getAdresse());
                setNullableDouble(ps, 3, rendezVous.getLatitude());
                setNullableDouble(ps, 4, rendezVous.getLongitude());
                ps.setInt(5, rendezVous.getTypeId());
                ps.setInt(6, rendezVous.getDispoId());
                ps.setInt(7, rendezVous.getUserId());
                ps.setString(8, STATUT_EN_ATTENTE);
                ps.setString(9, MedicalValidationService.normalize(rendezVous.getNotesPatient()));
                ps.setString(10, MedicalValidationService.normalize(rendezVous.getNotesPsychologue()));
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Reservation impossible";
                    return false;
                }
            }

            updateDisponibiliteState(cnx, rendezVous.getDispoId(), false);
            cnx.commit();
            lastValidationError = "";
            lastSuccessMessage = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly(cnx);
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la reservation";
            return false;
        } finally {
            restoreAutoCommit(cnx, initialAutoCommit);
        }
    }

    public boolean modifier(RendezVous rendezVous) {
        if (rendezVous == null) {
            lastValidationError = "Donnees du rendez-vous invalides";
            return false;
        }
        if (!typeAutorise(rendezVous.getTypeId())) {
            lastValidationError = "Type de rendez-vous invalide";
            return false;
        }
        String patientNoteError = MedicalValidationService.validatePatientRendezVousNote(rendezVous.getNotesPatient());
        if (patientNoteError != null) {
            lastValidationError = patientNoteError;
            return false;
        }

        String sql = "UPDATE rendez_vous SET age = ?, adresse = ?, latitude = ?, longitude = ?, type_id = ?, dispo_id = ?, user_id = ?, statut = ?, notes_patient = ?, notes_psychologue = ? WHERE id = ?";
        boolean initialAutoCommit = true;
        Connection cnx = getConnection();
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            RendezVous existing = getRendezVousByIdForUpdate(cnx, rendezVous.getId());
            if (existing == null) {
                cnx.rollback();
                lastValidationError = "Rendez-vous introuvable";
                return false;
            }

            ValidationReservation validation = validerReservation(cnx, rendezVous.getUserId(), rendezVous.getDispoId(), rendezVous.getId(), existing.getDispoId());
            if (!validation.allowed) {
                cnx.rollback();
                lastValidationError = validation.message;
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, rendezVous.getAge());
                ps.setString(2, rendezVous.getAdresse());
                setNullableDouble(ps, 3, rendezVous.getLatitude());
                setNullableDouble(ps, 4, rendezVous.getLongitude());
                ps.setInt(5, rendezVous.getTypeId());
                ps.setInt(6, rendezVous.getDispoId());
                ps.setInt(7, rendezVous.getUserId());
                ps.setString(8, STATUT_EN_ATTENTE);
                ps.setString(9, MedicalValidationService.normalize(rendezVous.getNotesPatient()));
                ps.setString(10, MedicalValidationService.normalize(existing.getNotesPsychologue()));
                ps.setInt(11, rendezVous.getId());
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Modification impossible";
                    return false;
                }
            }

            if (existing.getDispoId() != rendezVous.getDispoId()) {
                updateDisponibiliteState(cnx, existing.getDispoId(), true);
                updateDisponibiliteState(cnx, rendezVous.getDispoId(), false);
            }

            cnx.commit();
            reminderService.cancelRemindersForRendezVous(rendezVous.getId());
            googleCalendarSyncService.deleteCalendarEvent(rendezVous.getId());
            lastValidationError = "";
            lastSuccessMessage = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly(cnx);
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la modification";
            return false;
        } finally {
            restoreAutoCommit(cnx, initialAutoCommit);
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM rendez_vous WHERE id = ?";
        boolean initialAutoCommit = true;
        Connection cnx = getConnection();
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            RendezVous existing = getRendezVousByIdForUpdate(cnx, id);
            if (existing == null) {
                cnx.rollback();
                lastValidationError = "Rendez-vous introuvable";
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, id);
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Suppression impossible";
                    return false;
                }
            }

            if (!hasActiveReservationOnDispo(cnx, existing.getDispoId(), null)) {
                updateDisponibiliteState(cnx, existing.getDispoId(), true);
            }

            cnx.commit();
            reminderService.cancelRemindersForRendezVous(id);
            googleCalendarSyncService.deleteCalendarEvent(id);
            lastValidationError = "";
            lastSuccessMessage = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly(cnx);
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la suppression";
            return false;
        } finally {
            restoreAutoCommit(cnx, initialAutoCommit);
        }
    }

    public List<RendezVous> getRendezVousByUser(int userId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT r.id, r.age, r.adresse, r.latitude, r.longitude, r.type_id, r.dispo_id, r.user_id, r.statut, r.notes_patient, r.notes_psychologue, d.psychologue_id, "
                + "t.libelle AS type_rdv, d.date, d.heure_debut, d.heure_fin, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom "
                + "FROM rendez_vous r "
                + "JOIN type_rendez_vous t ON r.type_id = t.id "
                + "JOIN disponibilite d ON r.dispo_id = d.id "
                + "LEFT JOIN user u ON u.id = d.psychologue_id "
                + "WHERE r.user_id = ? ORDER BY d.date DESC, d.heure_debut DESC";

        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRendezVous(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<RendezVousPsy> getRendezVousForPsy(int psychologueId) {
        List<RendezVousPsy> list = new ArrayList<>();
        String sql = "SELECT r.id, r.age, r.adresse, r.latitude, r.longitude, r.type_id, r.dispo_id, r.user_id, r.statut, r.notes_patient, r.notes_psychologue, "
                + "u.nom, u.prenom, t.libelle AS type_rdv, d.date, d.heure_debut, d.heure_fin "
                + "FROM rendez_vous r "
                + "JOIN user u ON r.user_id = u.id "
                + "JOIN type_rendez_vous t ON r.type_id = t.id "
                + "JOIN disponibilite d ON r.dispo_id = d.id "
                + "WHERE d.psychologue_id = ? AND r.statut IN (?, ?) "
                + "ORDER BY d.date DESC, d.heure_debut DESC";

        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, psychologueId);
                ps.setString(2, STATUT_EN_ATTENTE);
                ps.setString(3, STATUT_ACCEPTEE);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RendezVousPsy rendezVous = new RendezVousPsy();
                        rendezVous.setId(rs.getInt("id"));
                        rendezVous.setAge(rs.getInt("age"));
                        rendezVous.setAdresse(rs.getString("adresse"));
                        rendezVous.setLatitude(getNullableDouble(rs, "latitude"));
                        rendezVous.setLongitude(getNullableDouble(rs, "longitude"));
                        rendezVous.setTypeId(rs.getInt("type_id"));
                        rendezVous.setDispoId(rs.getInt("dispo_id"));
                        rendezVous.setUserId(rs.getInt("user_id"));
                        rendezVous.setNom(rs.getString("nom"));
                        rendezVous.setPrenom(rs.getString("prenom"));
                        rendezVous.setTypeRdv(rs.getString("type_rdv"));
                        rendezVous.setDate(String.valueOf(rs.getDate("date")));
                        rendezVous.setHeureDebut(String.valueOf(rs.getTime("heure_debut")));
                        rendezVous.setHeureFin(String.valueOf(rs.getTime("heure_fin")));
                        rendezVous.setStatut(rs.getString("statut"));
                        rendezVous.setNotesPatient(rs.getString("notes_patient"));
                        rendezVous.setNotesPsychologue(rs.getString("notes_psychologue"));
                        list.add(rendezVous);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean validerRendezVous(int rendezVousId, int psychologueId) {
        String sql = "UPDATE rendez_vous r "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "SET r.statut = ? "
                + "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ?";
        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, STATUT_ACCEPTEE);
                ps.setInt(2, rendezVousId);
                ps.setInt(3, psychologueId);
                ps.setString(4, STATUT_EN_ATTENTE);
                boolean updated = ps.executeUpdate() > 0;
                if (!updated) {
                    lastValidationError = "Validation impossible";
                    lastSuccessMessage = "";
                    return false;
                }
            }
            medicalDataService.ensureSchema();
            medicalDataService.createConsultationFromAcceptedRendezVous(rendezVousId);
            sendAcceptanceEmail(rendezVousId, psychologueId);
            createPaymentRequest(rendezVousId);
            reminderService.scheduleRemindersForAcceptedAppointment(rendezVousId);
            googleCalendarSyncService.syncAcceptedAppointment(rendezVousId, psychologueId);
            lastValidationError = "";
            if (lastSuccessMessage == null || lastSuccessMessage.isBlank()) {
                lastSuccessMessage = "Rendez-vous acceptee.";
            }
            if (googleCalendarSyncService.isConfiguredForPsychologue(psychologueId)) {
                lastSuccessMessage = appendSuccessMessage("Evenement Google Calendar synchronise.");
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la validation";
            lastSuccessMessage = "";
            return false;
        }
    }

    public boolean rejeterRendezVous(int rendezVousId, int psychologueId) {
        String sql = "UPDATE rendez_vous r "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "SET r.statut = ? "
                + "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ?";
        boolean initialAutoCommit = true;
        Connection cnx = getConnection();
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            Integer dispoId = getDispoIdForPendingRendezVous(cnx, rendezVousId, psychologueId);
            if (dispoId == null) {
                cnx.rollback();
                lastValidationError = "Rejet impossible";
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, STATUT_REJETEE);
                ps.setInt(2, rendezVousId);
                ps.setInt(3, psychologueId);
                ps.setString(4, STATUT_EN_ATTENTE);
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Rejet impossible";
                    return false;
                }
            }

            if (!hasActiveReservationOnDispo(cnx, dispoId, rendezVousId)) {
                updateDisponibiliteState(cnx, dispoId, true);
            }

            cnx.commit();
            lastValidationError = "";
            lastSuccessMessage = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly(cnx);
            e.printStackTrace();
            lastValidationError = "Erreur technique lors du rejet";
            return false;
        } finally {
            restoreAutoCommit(cnx, initialAutoCommit);
        }
    }

    public boolean updatePsychologueNote(int rendezVousId, int psychologueId, String note) {
        String validationError = MedicalValidationService.validatePsychologueNote(note);
        if (validationError != null) {
            lastValidationError = validationError;
            return false;
        }

        String sql = "UPDATE rendez_vous r "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "SET r.notes_psychologue = ? "
                + "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ?";
        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, MedicalValidationService.normalize(note));
                ps.setInt(2, rendezVousId);
                ps.setInt(3, psychologueId);
                ps.setString(4, STATUT_ACCEPTEE);
                boolean updated = ps.executeUpdate() > 0;
                lastValidationError = updated ? "" : "La note psychologue ne peut etre ajoutee que sur un rendez-vous accepte";
                lastSuccessMessage = "";
                return updated;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de l'enregistrement de la note psychologue";
            lastSuccessMessage = "";
            return false;
        }
    }

    public boolean typeAutorise(int typeId) {
        String sql = "SELECT libelle FROM type_rendez_vous WHERE id = ?";
        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, typeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    String libelle = rs.getString("libelle");
                    if (libelle == null) {
                        return false;
                    }
                    String normalized = libelle.trim().toLowerCase();
                    return normalized.equals("suivi") || normalized.equals("consultation");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean patientHasRendezVousAtSameTime(int userId, int dispoId, Integer excludedRendezVousId) {
        String sql = "SELECT COUNT(*) "
                + "FROM rendez_vous r "
                + "JOIN disponibilite cible ON cible.id = ? "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "WHERE r.user_id = ? "
                + "AND r.statut IN (?, ?) "
                + "AND d.date = cible.date "
                + "AND d.heure_debut < cible.heure_fin "
                + "AND d.heure_fin > cible.heure_debut";

        if (excludedRendezVousId != null) {
            sql += " AND r.id <> ?";
        }

        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, dispoId);
                ps.setInt(2, userId);
                ps.setString(3, STATUT_EN_ATTENTE);
                ps.setString(4, STATUT_ACCEPTEE);
                if (excludedRendezVousId != null) {
                    ps.setInt(5, excludedRendezVousId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean peutReserver(int userId, int dispoId, Integer excludedRendezVousId) {
        return serviceDisponibilite.estDisponiblePourReservation(dispoId)
                && !patientHasRendezVousAtSameTime(userId, dispoId, excludedRendezVousId);
    }

    public String getLastValidationError() {
        return lastValidationError;
    }

    public String getLastSuccessMessage() {
        return lastSuccessMessage;
    }

    private RendezVous mapRendezVous(ResultSet rs) throws SQLException {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setId(rs.getInt("id"));
        rendezVous.setAge(rs.getInt("age"));
        rendezVous.setAdresse(rs.getString("adresse"));
        rendezVous.setLatitude(getNullableDouble(rs, "latitude"));
        rendezVous.setLongitude(getNullableDouble(rs, "longitude"));
        rendezVous.setTypeId(rs.getInt("type_id"));
        rendezVous.setDispoId(rs.getInt("dispo_id"));
        rendezVous.setUserId(rs.getInt("user_id"));
        rendezVous.setTypeLibelle(rs.getString("type_rdv"));
        rendezVous.setDateDisponibilite(String.valueOf(rs.getDate("date")));
        rendezVous.setHeureDebut(String.valueOf(rs.getTime("heure_debut")));
        rendezVous.setHeureFin(String.valueOf(rs.getTime("heure_fin")));
        rendezVous.setStatut(rs.getString("statut"));
        rendezVous.setNotesPatient(rs.getString("notes_patient"));
        rendezVous.setNotesPsychologue(rs.getString("notes_psychologue"));
        rendezVous.setPsychologueId(rs.getInt("psychologue_id"));
        rendezVous.setPsychologueNomComplet(rs.getString("psychologue_nom"));
        return rendezVous;
    }

    private void ensureSchema() {
        try {
            Connection cnx = getConnection();
            addColumnIfMissing(cnx, "rendez_vous", "statut", "VARCHAR(30) NOT NULL DEFAULT 'en attente'");
            addColumnIfMissing(cnx, "rendez_vous", "notes_patient", "TEXT NULL");
            addColumnIfMissing(cnx, "rendez_vous", "notes_psychologue", "TEXT NULL");
            addColumnIfMissing(cnx, "rendez_vous", "latitude", "DOUBLE NULL");
            addColumnIfMissing(cnx, "rendez_vous", "longitude", "DOUBLE NULL");
            backfillStatut(cnx);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendAcceptanceEmail(int rendezVousId, int psychologueId) {
        AppointmentAcceptanceEmailData emailData = getAcceptanceEmailData(rendezVousId, psychologueId);
        if (emailData == null || emailData.patientEmail == null || emailData.patientEmail.isBlank()) {
            lastSuccessMessage = "Rendez-vous acceptee, mais aucun email patient n'est disponible.";
            return;
        }

        try {
            emailService.sendAppointmentAcceptedEmail(
                    emailData.patientEmail,
                    emailData.patientName,
                    emailData.psychologueName,
                    emailData.appointmentDate,
                    emailData.timeRange,
                    emailData.appointmentType
            );
            lastSuccessMessage = "Rendez-vous acceptee et email envoye au patient.";
        } catch (RuntimeException e) {
            e.printStackTrace();
            lastSuccessMessage = "Rendez-vous acceptee, mais l'email n'a pas pu etre envoye : " + e.getMessage();
        }
    }

    private AppointmentAcceptanceEmailData getAcceptanceEmailData(int rendezVousId, int psychologueId) {
        String sql = "SELECT p.email AS patient_email, CONCAT(COALESCE(p.prenom, ''), ' ', COALESCE(p.nom, '')) AS patient_name, "
                + "CONCAT(COALESCE(psy.prenom, ''), ' ', COALESCE(psy.nom, '')) AS psychologue_name, "
                + "d.date AS appointment_date, d.heure_debut, d.heure_fin, t.libelle AS type_rdv "
                + "FROM rendez_vous r "
                + "JOIN user p ON p.id = r.user_id "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "JOIN type_rendez_vous t ON t.id = r.type_id "
                + "LEFT JOIN user psy ON psy.id = d.psychologue_id "
                + "WHERE r.id = ? AND d.psychologue_id = ?";

        try {
            Connection cnx = getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, rendezVousId);
                ps.setInt(2, psychologueId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    AppointmentAcceptanceEmailData data = new AppointmentAcceptanceEmailData();
                    data.patientEmail = rs.getString("patient_email");
                    data.patientName = normalizeFullName(rs.getString("patient_name"));
                    data.psychologueName = normalizeFullName(rs.getString("psychologue_name"));
                    Date appointmentDate = rs.getDate("appointment_date");
                    data.appointmentDate = appointmentDate == null ? "-" : appointmentDate.toString();
                    Time start = rs.getTime("heure_debut");
                    Time end = rs.getTime("heure_fin");
                    data.timeRange = buildTimeRange(start, end);
                    data.appointmentType = rs.getString("type_rdv");
                    return data;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lastSuccessMessage = "Rendez-vous acceptee, mais impossible de preparer l'email du patient.";
            return null;
        }
    }

    private void createPaymentRequest(int rendezVousId) {
        try {
            ConsultationWorkflowService.PaymentCreationResult paymentResult =
                    consultationWorkflowService.createOrRefreshPaymentForAcceptedRendezVous(rendezVousId);
            if (paymentResult.isAlreadyPaid()) {
                lastSuccessMessage = appendSuccessMessage("Paiement deja valide.");
                return;
            }
            if (!paymentResult.isStripeConfigured()) {
                lastSuccessMessage = appendSuccessMessage("Paiement cree localement, mais Stripe n'est pas encore configure.");
                return;
            }
            lastSuccessMessage = appendSuccessMessage("Lien de paiement Stripe envoye au patient.");
        } catch (SQLException e) {
            e.printStackTrace();
            lastSuccessMessage = appendSuccessMessage("Paiement non initialise: " + e.getMessage());
        }
    }

    private String appendSuccessMessage(String suffix) {
        String base = (lastSuccessMessage == null || lastSuccessMessage.isBlank())
                ? "Rendez-vous acceptee."
                : lastSuccessMessage.trim();
        if (suffix == null || suffix.isBlank()) {
            return base;
        }
        return base + " " + suffix.trim();
    }

    private String buildTimeRange(Time start, Time end) {
        String startText = start == null ? "-" : start.toLocalTime().toString();
        String endText = end == null ? "-" : end.toLocalTime().toString();
        return startText + " - " + endText;
    }

    private String normalizeFullName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private void backfillStatut(Connection cnx) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE rendez_vous SET statut = ? WHERE statut IS NULL OR TRIM(statut) = ''")) {
            ps.setString(1, STATUT_EN_ATTENTE);
            ps.executeUpdate();
        }
    }

    private void addColumnIfMissing(Connection cnx, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (columnExists(cnx, tableName, columnName)) {
            return;
        }
        try (PreparedStatement stmt = cnx.prepareStatement(
                "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition)) {
            stmt.executeUpdate();
        }
    }

    private boolean columnExists(Connection cnx, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = cnx.getMetaData();
        try (ResultSet rs = metaData.getColumns(cnx.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private ValidationReservation validerReservation(Connection cnx, int userId, int dispoId, Integer excludedRendezVousId, Integer currentDispoId)
            throws SQLException {
        DispoReservationState dispoState = getDispoReservationStateForUpdate(cnx, dispoId);
        if (dispoState == null) {
            return ValidationReservation.denied("Le creneau selectionne n'existe pas");
        }
        if (!dispoState.future) {
            return ValidationReservation.denied("Impossible de reserver un creneau dans le passe");
        }
        if (!dispoState.free && (currentDispoId == null || dispoId != currentDispoId)) {
            return ValidationReservation.denied("Ce creneau est deja reserve");
        }
        if (patientHasRendezVousAtSameTime(userId, dispoId, excludedRendezVousId)) {
            return ValidationReservation.denied("Le patient a deja un rendez-vous sur ce creneau");
        }
        return ValidationReservation.allowed();
    }

    private DispoReservationState getDispoReservationStateForUpdate(Connection cnx, int dispoId) throws SQLException {
        String sql = "SELECT est_libre, TIMESTAMP(date, heure_debut) > NOW() AS est_future FROM disponibilite WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, dispoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new DispoReservationState(rs.getInt("est_libre") == 1, rs.getInt("est_future") == 1);
            }
        }
    }

    private RendezVous getRendezVousByIdForUpdate(Connection cnx, int id) throws SQLException {
        String sql = "SELECT id, age, adresse, latitude, longitude, type_id, dispo_id, user_id, statut, notes_patient, notes_psychologue FROM rendez_vous WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                RendezVous rendezVous = new RendezVous();
                rendezVous.setId(rs.getInt("id"));
                rendezVous.setAge(rs.getInt("age"));
                rendezVous.setAdresse(rs.getString("adresse"));
                rendezVous.setLatitude(getNullableDouble(rs, "latitude"));
                rendezVous.setLongitude(getNullableDouble(rs, "longitude"));
                rendezVous.setTypeId(rs.getInt("type_id"));
                rendezVous.setDispoId(rs.getInt("dispo_id"));
                rendezVous.setUserId(rs.getInt("user_id"));
                rendezVous.setStatut(rs.getString("statut"));
                rendezVous.setNotesPatient(rs.getString("notes_patient"));
                rendezVous.setNotesPsychologue(rs.getString("notes_psychologue"));
                return rendezVous;
            }
        }
    }

    private Integer getDispoIdForPendingRendezVous(Connection cnx, int rendezVousId, int psychologueId) throws SQLException {
        String sql = "SELECT r.dispo_id FROM rendez_vous r "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            ps.setInt(2, psychologueId);
            ps.setString(3, STATUT_EN_ATTENTE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("dispo_id") : null;
            }
        }
    }

    private boolean hasActiveReservationOnDispo(Connection cnx, int dispoId, Integer excludedRendezVousId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE dispo_id = ? AND statut IN (?, ?)";
        if (excludedRendezVousId != null) {
            sql += " AND id <> ?";
        }
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, dispoId);
            ps.setString(2, STATUT_EN_ATTENTE);
            ps.setString(3, STATUT_ACCEPTEE);
            if (excludedRendezVousId != null) {
                ps.setInt(4, excludedRendezVousId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void updateDisponibiliteState(Connection cnx, int dispoId, boolean libre) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("UPDATE disponibilite SET est_libre = ? WHERE id = ?")) {
            ps.setInt(1, libre ? 1 : 0);
            ps.setInt(2, dispoId);
            ps.executeUpdate();
        }
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }

    private Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private void rollbackQuietly(Connection cnx) {
        try {
            if (cnx != null && !cnx.getAutoCommit()) {
                cnx.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(Connection cnx, boolean initialAutoCommit) {
        try {
            if (cnx != null) {
                cnx.setAutoCommit(initialAutoCommit);
            }
        } catch (SQLException ignored) {
        }
    }

    private static final class ValidationReservation {
        private final boolean allowed;
        private final String message;

        private ValidationReservation(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        private static ValidationReservation allowed() {
            return new ValidationReservation(true, "");
        }

        private static ValidationReservation denied(String message) {
            return new ValidationReservation(false, message);
        }
    }

    private static final class DispoReservationState {
        private final boolean free;
        private final boolean future;

        private DispoReservationState(boolean free, boolean future) {
            this.free = free;
            this.future = future;
        }
    }

    private static final class AppointmentAcceptanceEmailData {
        private String patientEmail;
        private String patientName;
        private String psychologueName;
        private String appointmentDate;
        private String timeRange;
        private String appointmentType;
    }
}
