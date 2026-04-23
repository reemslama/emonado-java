package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.example.entities.User;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class QrLoginService {
    private static final QrLoginService INSTANCE = new QrLoginService();
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);

    private final Map<String, QrChallenge> challenges = new ConcurrentHashMap<>();
    private HttpServer httpServer;
    private int port = -1;

    public static QrLoginService getInstance() {
        return INSTANCE;
    }

    public synchronized QrChallenge createChallenge(User user) {
        ensureServerStarted();
        String lanIp = resolveLanIp();
        if (lanIp == null || lanIp.isBlank()) {
            throw new RuntimeException("Impossible de detecter une adresse reseau locale. Connectez le PC et le smartphone au meme Wi-Fi.");
        }

        String token = UUID.randomUUID().toString();
        String approvalUrl = "http://" + lanIp + ":" + port + "/qr-login/approve?token=" + token;
        QrChallenge challenge = new QrChallenge(token, user, approvalUrl, Instant.now().plus(CHALLENGE_TTL));
        challenges.put(token, challenge);
        return challenge;
    }

    public Image buildQrImage(String content, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            WritableImage image = new WritableImage(size, size);
            PixelWriter writer = image.getPixelWriter();
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    writer.setArgb(x, y, matrix.get(x, y) ? 0xFF111827 : 0xFFFFFFFF);
                }
            }
            return image;
        } catch (WriterException e) {
            throw new RuntimeException("Impossible de generer le QR code : " + e.getMessage(), e);
        }
    }

    public void cancelChallenge(String token) {
        if (token != null) {
            challenges.remove(token);
        }
    }

    private synchronized void ensureServerStarted() {
        if (httpServer != null) {
            return;
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            port = httpServer.getAddress().getPort();
            httpServer.createContext("/qr-login/approve", this::handleApproval);
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, "qr-login-server");
                thread.setDaemon(true);
                return thread;
            };
            httpServer.setExecutor(Executors.newCachedThreadPool(threadFactory));
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de demarrer le serveur local du QR login : " + e.getMessage(), e);
        }
    }

    private void handleApproval(HttpExchange exchange) throws IOException {
        String token = readQueryParam(exchange.getRequestURI().getQuery(), "token");
        QrChallenge challenge = token == null ? null : challenges.get(token);

        String response;
        int status;
        if (challenge == null) {
            status = 404;
            response = buildHtml("Lien invalide", "Ce QR code n'est plus valide.");
        } else if (challenge.isExpired()) {
            challenges.remove(token);
            status = 410;
            response = buildHtml("QR expire", "La demande a expire. Relancez la connexion sur le PC.");
        } else {
            challenge.approve();
            status = 200;
            response = buildHtml("Connexion validee", "Vous pouvez revenir sur l'application desktop.");
        }

        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String buildHtml(String title, String body) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body { font-family: Arial, sans-serif; background: linear-gradient(135deg, #eff6ff, #ecfeff); display:flex; align-items:center; justify-content:center; min-height:100vh; margin:0; }
                        .card { background:white; padding:32px; border-radius:24px; max-width:420px; box-shadow:0 18px 40px rgba(15,23,42,0.12); text-align:center; }
                        h1 { color:#0f172a; margin-bottom:12px; }
                        p { color:#475569; line-height:1.5; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>%s</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(title, title, body);
    }

    private String readQueryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String prefix = key + "=";
        for (String pair : query.split("&")) {
            if (pair.startsWith(prefix)) {
                return pair.substring(prefix.length());
            }
        }
        return null;
    }

    private String resolveLanIp() {
        try {
            String fallback = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || isIgnoredInterface(networkInterface)) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address inet4Address
                            && !inet4Address.isLoopbackAddress()
                            && inet4Address.isSiteLocalAddress()) {
                        String hostAddress = inet4Address.getHostAddress();
                        if (isPreferredInterface(networkInterface)) {
                            return hostAddress;
                        }
                        if (fallback == null) {
                            fallback = hostAddress;
                        }
                    }
                }
            }

            if (fallback != null) {
                return fallback;
            }

            InetAddress localhost = InetAddress.getLocalHost();
            return localhost instanceof Inet4Address ? localhost.getHostAddress() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isIgnoredInterface(NetworkInterface networkInterface) {
        String name = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase();
        return name.contains("vmware")
                || name.contains("virtualbox")
                || name.contains("hyper-v")
                || name.contains("vethernet")
                || name.contains("docker")
                || name.contains("loopback")
                || name.contains("host-only")
                || name.contains("tunnel");
    }

    private boolean isPreferredInterface(NetworkInterface networkInterface) {
        String name = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase();
        return name.contains("wi-fi")
                || name.contains("wifi")
                || name.contains("wireless")
                || name.contains("wlan")
                || name.contains("ethernet");
    }

    public static class QrChallenge {
        private final String token;
        private final User user;
        private final String approvalUrl;
        private final Instant expiresAt;
        private volatile boolean approved;

        public QrChallenge(String token, User user, String approvalUrl, Instant expiresAt) {
            this.token = token;
            this.user = user;
            this.approvalUrl = approvalUrl;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return token;
        }

        public User getUser() {
            return user;
        }

        public String getApprovalUrl() {
            return approvalUrl;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public boolean isApproved() {
            return approved;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public long getRemainingSeconds() {
            return Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        }

        public void approve() {
            this.approved = true;
        }
    }
}
