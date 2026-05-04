package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class SmsService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Properties config = IntegrationConfigService.loadProperties("twilio.properties");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String accountSid = IntegrationConfigService.read(config, "TWILIO_ACCOUNT_SID", "");
    private final String authToken = IntegrationConfigService.read(config, "TWILIO_AUTH_TOKEN", "");
    private final String fromNumber = IntegrationConfigService.read(config, "TWILIO_FROM_NUMBER", "");

    public boolean isConfigured() {
        return !accountSid.isBlank() && !authToken.isBlank() && !fromNumber.isBlank();
    }

    public void sendAppointmentReminderSms(String toNumber,
                                           String patientName,
                                           String psychologueName,
                                           String appointmentDate,
                                           String timeRange,
                                           String appointmentType) {
        if (!isConfigured()) {
            throw new IllegalStateException("Twilio n'est pas configure.");
        }
        if (toNumber == null || toNumber.isBlank()) {
            throw new IllegalArgumentException("Numero de telephone patient manquant.");
        }

        String body = "Bonjour " + fallback(patientName, "patient")
                + ", rappel EmoNado: votre " + fallback(appointmentType, "rendez-vous")
                + " avec " + fallback(psychologueName, "votre psychologue")
                + " est prevu le " + fallback(appointmentDate, "-")
                + " a " + fallback(timeRange, "-") + ".";

        String form = "To=" + encode(toNumber)
                + "&From=" + encode(fromNumber)
                + "&Body=" + encode(body);

        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Erreur Twilio HTTP " + response.statusCode() + " : " + extractMessage(response.body()));
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Impossible d'envoyer le SMS via Twilio : " + e.getMessage(), e);
        }
    }

    private String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "reponse vide";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
