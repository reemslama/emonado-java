package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleCalendarOAuthTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CONFIG_FILE = "google-calendar.properties";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";

    public static void main(String[] args) throws Exception {
        Path configPath = Paths.get("config", CONFIG_FILE);
        Properties props = new Properties();
        if (Files.exists(configPath)) {
            try (var input = Files.newInputStream(configPath)) {
                props.load(input);
            }
        }

        String clientId = read(props, "google.calendar.default.clientId");
        String clientSecret = read(props, "google.calendar.default.clientSecret");
        String redirectUri = readOrDefault(props, "google.calendar.default.redirectUri", "http://localhost");

        if (clientId.isBlank() || clientSecret.isBlank()) {
            System.err.println("Client Google manquant dans config/google-calendar.properties");
            return;
        }

        URI redirect = URI.create(redirectUri);
        int port = redirect.getPort() > 0 ? redirect.getPort() : 80;
        String path = redirect.getPath() == null || redirect.getPath().isBlank() ? "/" : redirect.getPath();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> authCodeRef = new AtomicReference<>("");
        AtomicReference<String> errorRef = new AtomicReference<>("");

        HttpServer server = HttpServer.create(new InetSocketAddress(redirect.getHost(), port), 0);
        server.createContext(path, exchange -> handleCallback(exchange, authCodeRef, errorRef, latch));
        server.start();

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(CALENDAR_SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true";

        System.out.println("Ouverture du navigateur pour autoriser Google Calendar...");
        System.out.println(authUrl);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI.create(authUrl));
        }

        boolean completed = latch.await(600, TimeUnit.SECONDS);
        server.stop(0);
        if (!completed) {
            System.err.println("Delai depasse. Aucun code OAuth recu.");
            return;
        }
        if (!errorRef.get().isBlank()) {
            System.err.println("Erreur OAuth: " + errorRef.get());
            return;
        }
        if (authCodeRef.get().isBlank()) {
            System.err.println("Aucun code OAuth recu.");
            return;
        }

        JsonNode tokenResponse = exchangeCodeForTokens(clientId, clientSecret, redirectUri, authCodeRef.get());
        String accessToken = tokenResponse.path("access_token").asText("");
        String refreshToken = tokenResponse.path("refresh_token").asText("");
        if (accessToken.isBlank()) {
            System.err.println("Google n'a pas retourne d'access_token.");
            return;
        }

        props.setProperty("google.calendar.default.accessToken", accessToken);
        if (!refreshToken.isBlank()) {
            props.setProperty("google.calendar.default.refreshToken", refreshToken);
        }

        try (var output = Files.newOutputStream(configPath)) {
            props.store(output, "Google Calendar OAuth tokens");
        }

        System.out.println("Configuration Google Calendar mise a jour dans " + configPath.toAbsolutePath());
        if (refreshToken.isBlank()) {
            System.out.println("Attention: aucun refresh_token recu. Refaire le consentement peut etre necessaire.");
        } else {
            System.out.println("Refresh token enregistre avec succes.");
        }
    }

    private static void handleCallback(HttpExchange exchange,
                                       AtomicReference<String> authCodeRef,
                                       AtomicReference<String> errorRef,
                                       CountDownLatch latch) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = decode(parts[0]);
                String value = parts.length > 1 ? decode(parts[1]) : "";
                if ("code".equals(key)) {
                    authCodeRef.set(value);
                } else if ("error".equals(key)) {
                    errorRef.set(value);
                }
            }
        }

        String body = "<html><body><h2>Google Calendar connecte</h2><p>Vous pouvez fermer cette page et revenir dans l'application.</p></body></html>";
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        latch.countDown();
    }

    private static JsonNode exchangeCodeForTokens(String clientId,
                                                  String clientSecret,
                                                  String redirectUri,
                                                  String code) throws IOException, InterruptedException {
        String form = "code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Erreur token Google HTTP " + response.statusCode() + ": " + response.body());
        }
        return OBJECT_MAPPER.readTree(response.body());
    }

    private static String read(Properties props, String key) {
        return props.getProperty(key, "").trim();
    }

    private static String readOrDefault(Properties props, String key, String defaultValue) {
        String value = read(props, key);
        return value.isBlank() ? defaultValue : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
