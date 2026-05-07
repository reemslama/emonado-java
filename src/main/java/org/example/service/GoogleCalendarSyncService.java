package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.utils.DataSource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class GoogleCalendarSyncService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter GOOGLE_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private final Properties config = IntegrationConfigService.loadProperties("google-calendar.properties");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GoogleCalendarSyncService() {
        ensureSchema();
    }

    public boolean isConfiguredForPsychologue(int psychologueId) {
        return !resolveCalendarId(psychologueId).isBlank()
                && (!resolveAccessToken(psychologueId).isBlank() || hasRefreshTokenConfig(psychologueId));
    }

    public void syncAcceptedAppointment(int rendezVousId, int psychologueId) {
        AppointmentCalendarData data = getAppointmentData(rendezVousId, psychologueId);
        if (data == null || data.startAt == null || data.endAt == null || !isConfiguredForPsychologue(psychologueId)) {
            return;
        }
        try {
            String calendarId = resolveCalendarId(psychologueId);
            String accessToken = resolveValidAccessToken(psychologueId);
            if (accessToken.isBlank()) {
                upsertCalendarLink(rendezVousId, psychologueId, calendarId, getExistingEventId(rendezVousId), "failed",
                        "Token Google Calendar manquant ou non rafraichissable.");
                return;
            }
            String existingEventId = getExistingEventId(rendezVousId);
            String payload = buildEventPayload(data);
            HttpRequest request = buildCalendarRequest(calendarId, existingEventId, accessToken, payload);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 && hasRefreshTokenConfig(psychologueId)) {
                accessToken = refreshAccessToken(psychologueId);
                if (!accessToken.isBlank()) {
                    request = buildCalendarRequest(calendarId, existingEventId, accessToken, payload);
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                }
            }
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Google Calendar HTTP " + response.statusCode() + " : " + response.body());
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String eventId = root.path("id").asText(existingEventId == null ? "" : existingEventId);
            upsertCalendarLink(rendezVousId, psychologueId, calendarId, eventId, "synced", null);
        } catch (IOException e) {
            upsertCalendarLink(rendezVousId, psychologueId, resolveCalendarId(psychologueId), getExistingEventId(rendezVousId), "failed", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            upsertCalendarLink(rendezVousId, psychologueId, resolveCalendarId(psychologueId), getExistingEventId(rendezVousId), "failed", "Synchronisation Google interrompue.");
        } catch (RuntimeException e) {
            upsertCalendarLink(rendezVousId, psychologueId, resolveCalendarId(psychologueId), getExistingEventId(rendezVousId), "failed", e.getMessage());
        }
    }

    public void deleteCalendarEvent(int rendezVousId) {
        CalendarLink link = getCalendarLink(rendezVousId);
        if (link == null || link.eventId == null || link.eventId.isBlank()) {
            return;
        }
        String accessToken = resolveValidAccessToken(link.psychologueId);
        if (accessToken.isBlank()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/"
                            + encode(link.calendarId) + "/events/" + encode(link.eventId)))
                    .header("Authorization", "Bearer " + accessToken)
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            upsertCalendarLink(rendezVousId, link.psychologueId, link.calendarId, link.eventId, "deleted", null);
        } catch (Exception e) {
            upsertCalendarLink(rendezVousId, link.psychologueId, link.calendarId, link.eventId, "failed", e.getMessage());
        }
    }

    private String buildEventPayload(AppointmentCalendarData data) throws IOException {
        String timeZone = resolveTimeZone(data.psychologueId);
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("summary", "Consultation EmoNado - " + fallback(data.patientName, "Patient"));
        payload.put("description", buildDescription(data));
        payload.set("start", OBJECT_MAPPER.createObjectNode()
                .put("dateTime", GOOGLE_DATE_TIME.format(data.startAt))
                .put("timeZone", timeZone));
        payload.set("end", OBJECT_MAPPER.createObjectNode()
                .put("dateTime", GOOGLE_DATE_TIME.format(data.endAt))
                .put("timeZone", timeZone));
        payload.put("location", fallback(data.patientAddress, "Localisation patient non renseignee"));
        return payload.toPrettyString();
    }

    private String buildDescription(AppointmentCalendarData data) {
        return "Type: " + fallback(data.appointmentType, "consultation")
                + "\nPatient: " + fallback(data.patientName, "-")
                + "\nTelephone: " + fallback(data.patientPhone, "-")
                + "\nNotes patient: " + fallback(data.patientNotes, "-")
                + "\nAdresse: " + fallback(data.patientAddress, "-");
    }

    private String buildEventUrl(String calendarId, String existingEventId) {
        String base = "https://www.googleapis.com/calendar/v3/calendars/" + encode(calendarId) + "/events";
        return existingEventId == null || existingEventId.isBlank() ? base : base + "/" + encode(existingEventId);
    }

    private String resolveCalendarId(int psychologueId) {
        String psychologueSpecific = config.getProperty("google.calendar.psychologue." + psychologueId + ".calendarId", "").trim();
        if (!psychologueSpecific.isBlank()) {
            return psychologueSpecific;
        }
        return config.getProperty("google.calendar.default.calendarId", "").trim();
    }

    private String resolveAccessToken(int psychologueId) {
        String psychologueSpecific = config.getProperty("google.calendar.psychologue." + psychologueId + ".accessToken", "").trim();
        if (!psychologueSpecific.isBlank()) {
            return psychologueSpecific;
        }
        return config.getProperty("google.calendar.default.accessToken", "").trim();
    }

    private String resolveRefreshToken(int psychologueId) {
        String psychologueSpecific = config.getProperty("google.calendar.psychologue." + psychologueId + ".refreshToken", "").trim();
        if (!psychologueSpecific.isBlank()) {
            return psychologueSpecific;
        }
        return config.getProperty("google.calendar.default.refreshToken", "").trim();
    }

    private String resolveClientId(int psychologueId) {
        String psychologueSpecific = config.getProperty("google.calendar.psychologue." + psychologueId + ".clientId", "").trim();
        if (!psychologueSpecific.isBlank()) {
            return psychologueSpecific;
        }
        return config.getProperty("google.calendar.default.clientId", "").trim();
    }

    private String resolveClientSecret(int psychologueId) {
        String psychologueSpecific = config.getProperty("google.calendar.psychologue." + psychologueId + ".clientSecret", "").trim();
        if (!psychologueSpecific.isBlank()) {
            return psychologueSpecific;
        }
        return config.getProperty("google.calendar.default.clientSecret", "").trim();
    }

    private String resolveTimeZone(int psychologueId) {
        String psychologueSpecific = config.getProperty("google.calendar.psychologue." + psychologueId + ".timeZone", "").trim();
        if (!psychologueSpecific.isBlank()) {
            return psychologueSpecific;
        }
        return config.getProperty("google.calendar.default.timeZone", ZoneId.systemDefault().getId()).trim();
    }

    private boolean hasRefreshTokenConfig(int psychologueId) {
        return !resolveRefreshToken(psychologueId).isBlank()
                && !resolveClientId(psychologueId).isBlank()
                && !resolveClientSecret(psychologueId).isBlank();
    }

    private String resolveValidAccessToken(int psychologueId) {
        String accessToken = resolveAccessToken(psychologueId);
        if (!accessToken.isBlank()) {
            return accessToken;
        }
        if (!hasRefreshTokenConfig(psychologueId)) {
            return "";
        }
        return refreshAccessToken(psychologueId);
    }

    private String refreshAccessToken(int psychologueId) {
        String refreshToken = resolveRefreshToken(psychologueId);
        String clientId = resolveClientId(psychologueId);
        String clientSecret = resolveClientSecret(psychologueId);
        if (refreshToken.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            return "";
        }

        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Rafraichissement token Google HTTP " + response.statusCode() + " : " + response.body());
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String accessToken = root.path("access_token").asText("");
            if (accessToken.isBlank()) {
                throw new RuntimeException("Reponse Google sans access_token.");
            }
            return accessToken;
        } catch (IOException e) {
            throw new RuntimeException("Impossible de rafraichir le token Google : " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rafraichissement token Google interrompu.", e);
        }
    }

    private HttpRequest buildCalendarRequest(String calendarId, String existingEventId, String accessToken, String payload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(buildEventUrl(calendarId, existingEventId)))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method(existingEventId == null ? "POST" : "PUT", HttpRequest.BodyPublishers.ofString(payload))
                .build();
    }

    private AppointmentCalendarData getAppointmentData(int rendezVousId, int psychologueId) {
        String sql = "SELECT r.id, r.adresse, r.notes_patient, p.telephone AS patient_phone, "
                + "CONCAT(COALESCE(p.prenom, ''), ' ', COALESCE(p.nom, '')) AS patient_name, "
                + "t.libelle AS appointment_type, d.psychologue_id, "
                + "TIMESTAMP(d.date, d.heure_debut) AS start_at, TIMESTAMP(d.date, d.heure_fin) AS end_at "
                + "FROM rendez_vous r "
                + "JOIN user p ON p.id = r.user_id "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "JOIN type_rendez_vous t ON t.id = r.type_id "
                + "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = 'acceptee'";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            ps.setInt(2, psychologueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AppointmentCalendarData data = new AppointmentCalendarData();
                data.rendezVousId = rendezVousId;
                data.psychologueId = psychologueId;
                data.patientName = rs.getString("patient_name");
                data.patientPhone = rs.getString("patient_phone");
                data.appointmentType = rs.getString("appointment_type");
                data.patientAddress = rs.getString("adresse");
                data.patientNotes = rs.getString("notes_patient");
                Timestamp startAt = rs.getTimestamp("start_at");
                Timestamp endAt = rs.getTimestamp("end_at");
                data.startAt = startAt == null ? null : startAt.toLocalDateTime();
                data.endAt = endAt == null ? null : endAt.toLocalDateTime();
                return data;
            }
        } catch (SQLException e) {
            System.err.println("[GoogleCalendarSyncService] Impossible de charger le rendez-vous : " + e.getMessage());
            return null;
        }
    }

    private void ensureSchema() {
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement create = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS rendez_vous_calendar_link ("
                             + "id INT PRIMARY KEY AUTO_INCREMENT, "
                             + "rendez_vous_id INT NOT NULL UNIQUE, "
                             + "psychologue_id INT NOT NULL, "
                             + "calendar_id VARCHAR(255) NULL, "
                             + "event_id VARCHAR(255) NULL, "
                             + "status VARCHAR(30) NOT NULL DEFAULT 'pending', "
                             + "error_message TEXT NULL, "
                             + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                             + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                             + "CONSTRAINT fk_rdv_calendar_link_rdv FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id) ON DELETE CASCADE)")) {
            create.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GoogleCalendarSyncService] Impossible d'initialiser la table Google Calendar : " + e.getMessage());
        }
    }

    private void upsertCalendarLink(int rendezVousId,
                                    int psychologueId,
                                    String calendarId,
                                    String eventId,
                                    String status,
                                    String error) {
        String select = "SELECT id FROM rendez_vous_calendar_link WHERE rendez_vous_id = ?";
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(select)) {
            ps.setInt(1, rendezVousId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE rendez_vous_calendar_link SET psychologue_id = ?, calendar_id = ?, event_id = ?, status = ?, error_message = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        update.setInt(1, psychologueId);
                        update.setString(2, calendarId);
                        update.setString(3, eventId);
                        update.setString(4, status);
                        update.setString(5, error);
                        update.setInt(6, rs.getInt("id"));
                        update.executeUpdate();
                    }
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO rendez_vous_calendar_link (rendez_vous_id, psychologue_id, calendar_id, event_id, status, error_message) VALUES (?, ?, ?, ?, ?, ?)")) {
                insert.setInt(1, rendezVousId);
                insert.setInt(2, psychologueId);
                insert.setString(3, calendarId);
                insert.setString(4, eventId);
                insert.setString(5, status);
                insert.setString(6, error);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[GoogleCalendarSyncService] Impossible de persister le lien Google Calendar : " + e.getMessage());
        }
    }

    private String getExistingEventId(int rendezVousId) {
        CalendarLink link = getCalendarLink(rendezVousId);
        return link == null ? null : link.eventId;
    }

    private CalendarLink getCalendarLink(int rendezVousId) {
        try (Connection connection = DataSource.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT psychologue_id, calendar_id, event_id, status FROM rendez_vous_calendar_link WHERE rendez_vous_id = ?")) {
            ps.setInt(1, rendezVousId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                CalendarLink link = new CalendarLink();
                link.psychologueId = rs.getInt("psychologue_id");
                link.calendarId = rs.getString("calendar_id");
                link.eventId = rs.getString("event_id");
                link.status = rs.getString("status");
                return link;
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static final class AppointmentCalendarData {
        private int rendezVousId;
        private int psychologueId;
        private String patientName;
        private String patientPhone;
        private String appointmentType;
        private String patientAddress;
        private String patientNotes;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
    }

    private static final class CalendarLink {
        private int psychologueId;
        private String calendarId;
        private String eventId;
        private String status;
    }
}
