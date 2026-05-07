package org.example.service;

import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RendezVousReminderService {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "rdv-reminder-worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private final EmailService emailService = new EmailService();
    private final SmsService smsService = new SmsService();

    public RendezVousReminderService() {
        ensureSchema();
        startScheduler();
    }

    public void scheduleRemindersForAcceptedAppointment(int rendezVousId) {
        ensureSchema();
        AppointmentReminderData data = getReminderData(rendezVousId);
        if (data == null || data.appointmentAt == null) {
            return;
        }
        LocalDateTime scheduledAt = data.appointmentAt.minusHours(24);
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            scheduledAt = LocalDateTime.now().plusMinutes(1);
        }
        upsertReminder(rendezVousId, "email", scheduledAt);
        if (smsService.isConfigured() && data.patientPhone != null && !data.patientPhone.isBlank()) {
            upsertReminder(rendezVousId, "sms", scheduledAt);
        }
    }

    public void cancelRemindersForRendezVous(int rendezVousId) {
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE rendez_vous_reminder SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP WHERE rendez_vous_id = ? AND status = 'pending'")) {
            ps.setInt(1, rendezVousId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[RendezVousReminderService] Impossible d'annuler les rappels : " + e.getMessage());
        }
    }

    private void startScheduler() {
        if (STARTED.compareAndSet(false, true)) {
            EXECUTOR.scheduleAtFixedRate(this::processDueReminders, 20, 60, TimeUnit.SECONDS);
        }
    }

    private void processDueReminders() {
        String sql = "SELECT id, rendez_vous_id, channel FROM rendez_vous_reminder WHERE status = 'pending' AND scheduled_at <= CURRENT_TIMESTAMP ORDER BY scheduled_at ASC";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int reminderId = rs.getInt("id");
                int rendezVousId = rs.getInt("rendez_vous_id");
                String channel = rs.getString("channel");
                sendReminder(reminderId, rendezVousId, channel);
            }
        } catch (SQLException e) {
            System.err.println("[RendezVousReminderService] Erreur scheduler: " + e.getMessage());
        }
    }

    private void sendReminder(int reminderId, int rendezVousId, String channel) {
        AppointmentReminderData data = getReminderData(rendezVousId);
        if (data == null) {
            markFailed(reminderId, "Rendez-vous introuvable pour le rappel.");
            return;
        }
        try {
            if ("email".equalsIgnoreCase(channel)) {
                emailService.sendAppointmentReminderEmail(
                        data.patientEmail,
                        data.patientName,
                        data.psychologueName,
                        data.appointmentDate,
                        data.timeRange,
                        data.appointmentType
                );
            } else if ("sms".equalsIgnoreCase(channel)) {
                smsService.sendAppointmentReminderSms(
                        data.patientPhone,
                        data.patientName,
                        data.psychologueName,
                        data.appointmentDate,
                        data.timeRange,
                        data.appointmentType
                );
            }
            markSent(reminderId);
        } catch (RuntimeException e) {
            markFailed(reminderId, e.getMessage());
        }
    }

    private void markSent(int reminderId) {
        updateReminderStatus(reminderId, "sent", null, true);
    }

    private void markFailed(int reminderId, String error) {
        updateReminderStatus(reminderId, "failed", error, false);
    }

    private void updateReminderStatus(int reminderId, String status, String error, boolean sent) {
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE rendez_vous_reminder SET status = ?, error_message = ?, sent_at = "
                             + (sent ? "CURRENT_TIMESTAMP" : "sent_at")
                             + ", updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, status);
            ps.setString(2, error);
            ps.setInt(3, reminderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[RendezVousReminderService] Impossible de mettre a jour le rappel " + reminderId + " : " + e.getMessage());
        }
    }

    private void upsertReminder(int rendezVousId, String channel, LocalDateTime scheduledAt) {
        String select = "SELECT id FROM rendez_vous_reminder WHERE rendez_vous_id = ? AND channel = ?";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(select)) {
            ps.setInt(1, rendezVousId);
            ps.setString(2, channel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE rendez_vous_reminder SET scheduled_at = ?, status = 'pending', error_message = NULL, sent_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        update.setTimestamp(1, Timestamp.valueOf(scheduledAt));
                        update.setInt(2, rs.getInt("id"));
                        update.executeUpdate();
                    }
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO rendez_vous_reminder (rendez_vous_id, channel, scheduled_at, status) VALUES (?, ?, ?, 'pending')")) {
                insert.setInt(1, rendezVousId);
                insert.setString(2, channel);
                insert.setTimestamp(3, Timestamp.valueOf(scheduledAt));
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[RendezVousReminderService] Impossible de planifier le rappel : " + e.getMessage());
        }
    }

    private AppointmentReminderData getReminderData(int rendezVousId) {
        String sql = "SELECT p.email AS patient_email, p.telephone AS patient_phone, "
                + "CONCAT(COALESCE(p.prenom, ''), ' ', COALESCE(p.nom, '')) AS patient_name, "
                + "CONCAT(COALESCE(psy.prenom, ''), ' ', COALESCE(psy.nom, '')) AS psychologue_name, "
                + "t.libelle AS type_rdv, d.date AS appointment_date, d.heure_debut, d.heure_fin, "
                + "TIMESTAMP(d.date, d.heure_debut) AS appointment_at "
                + "FROM rendez_vous r "
                + "JOIN user p ON p.id = r.user_id "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "JOIN type_rendez_vous t ON t.id = r.type_id "
                + "LEFT JOIN user psy ON psy.id = d.psychologue_id "
                + "WHERE r.id = ? AND r.statut = 'acceptee'";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AppointmentReminderData data = new AppointmentReminderData();
                data.patientEmail = rs.getString("patient_email");
                data.patientPhone = rs.getString("patient_phone");
                data.patientName = rs.getString("patient_name");
                data.psychologueName = rs.getString("psychologue_name");
                data.appointmentType = rs.getString("type_rdv");
                data.appointmentDate = rs.getDate("appointment_date") == null ? "-" : rs.getDate("appointment_date").toString();
                data.timeRange = buildTimeRange(rs.getString("heure_debut"), rs.getString("heure_fin"));
                Timestamp appointmentAt = rs.getTimestamp("appointment_at");
                data.appointmentAt = appointmentAt == null ? null : appointmentAt.toLocalDateTime();
                return data;
            }
        } catch (SQLException e) {
            System.err.println("[RendezVousReminderService] Impossible de charger les donnees du rappel : " + e.getMessage());
            return null;
        }
    }

    private String buildTimeRange(String start, String end) {
        return (start == null ? "-" : start) + " - " + (end == null ? "-" : end);
    }

    private void ensureSchema() {
        try (Connection connection = DataSource.getInstance().getConnection()) {
            try (PreparedStatement create = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS rendez_vous_reminder ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT, "
                            + "rendez_vous_id INT NOT NULL, "
                            + "channel VARCHAR(20) NOT NULL, "
                            + "scheduled_at TIMESTAMP NOT NULL, "
                            + "sent_at TIMESTAMP NULL, "
                            + "status VARCHAR(20) NOT NULL DEFAULT 'pending', "
                            + "error_message TEXT NULL, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                            + "UNIQUE KEY uk_rdv_reminder (rendez_vous_id, channel), "
                            + "CONSTRAINT fk_rdv_reminder_rdv FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id) ON DELETE CASCADE)")) {
                create.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[RendezVousReminderService] Impossible d'initialiser la table de rappels : " + e.getMessage());
        }
    }

    private static final class AppointmentReminderData {
        private String patientEmail;
        private String patientPhone;
        private String patientName;
        private String psychologueName;
        private String appointmentDate;
        private String timeRange;
        private String appointmentType;
        private LocalDateTime appointmentAt;
    }
}
