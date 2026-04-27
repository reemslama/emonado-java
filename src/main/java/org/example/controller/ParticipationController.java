package org.example.controller;

<<<<<<< Updated upstream
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
=======
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
>>>>>>> Stashed changes
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.entities.ImageCarte;
import org.example.entities.Jeu;
import org.example.entities.Participation;
import org.example.entities.RapportSessionJeu;
import org.example.entities.User;
import org.example.service.ServiceImageCarte;
import org.example.service.ServiceJeu;
import org.example.service.ServiceParticipation;
import org.example.service.SessionJeuService;
import org.example.utils.UserSession;

import java.io.FileInputStream;
<<<<<<< Updated upstream
import java.awt.Toolkit;
=======
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
>>>>>>> Stashed changes
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
<<<<<<< Updated upstream
import java.util.ArrayList;
=======
import java.time.format.DateTimeFormatter;
>>>>>>> Stashed changes
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.mail.*;
import javax.mail.internet.*;

public class ParticipationController {
<<<<<<< Updated upstream
    @FXML private ImageView sceneImageView;
    @FXML private FlowPane imageChoicesPane;
    @FXML private FlowPane progressPane;
    @FXML private StackPane completionPane;
    @FXML private Region endSignal;
    @FXML private Label completionTitle;
    @FXML private Label completionSummary;
=======
>>>>>>> Stashed changes

    @FXML private ComboBox<Jeu> comboJeux;
    @FXML private Label         labelScenario;
    @FXML private TilePane      imageChoicesPane;
    @FXML private Label         labelUserNom;
    @FXML private Label         labelInterpretation;
    @FXML private Label         labelDisponibilite;
    @FXML private Label         selectedImageLabel;
    @FXML private Label         errorJeu;
    @FXML private Label         errorChoix;
    @FXML private Label         errorGlobal;
    @FXML private Label         msgSuccess;
    @FXML private ProgressIndicator progressIA;

    private final ServiceJeu           serviceJeu           = new ServiceJeu();
    private final ServiceImageCarte    serviceImageCarte    = new ServiceImageCarte();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
<<<<<<< Updated upstream
    private final SessionJeuService sessionJeuService = new SessionJeuService();

    private List<Jeu> jeuxActifs = new ArrayList<>();
    private int currentIndex = -1;
    private long gameDisplayedAt = 0L;
    private boolean rapportEnvoye = false;

    @FXML
    public void initialize() {
        chargerJeuxSession();
        avancerVersJeuSuivant();
    }

    private void chargerJeuxSession() {
        List<Jeu> jeux = serviceJeu.afficherTout().stream().filter(Jeu::isActif).toList();
        List<Jeu> scenarioGames = jeux.stream()
                .filter(jeu -> jeu.getTitre() != null && jeu.getTitre().startsWith("TEST "))
                .toList();
        List<Jeu> sourceGames = scenarioGames.isEmpty() ? jeux : scenarioGames;
        jeuxActifs = new ArrayList<>();
        for (Jeu jeu : sourceGames) {
            jeu.setImages(serviceImageCarte.findByJeuId(jeu.getId()));
            if (jeu.getImages().size() >= 2 && jeu.getImages().size() <= 3) {
                jeuxActifs.add(jeu);
            }
        }
    }

    private void avancerVersJeuSuivant() {
        currentIndex++;
        if (currentIndex >= jeuxActifs.size()) {
            afficherFinSession();
            return;
        }
        afficherJeu(jeuxActifs.get(currentIndex));
    }

    private void afficherJeu(Jeu jeu) {
        completionPane.setVisible(false);
        completionPane.setManaged(false);
        sceneImageView.setImage(loadImage(jeu.getSceneImagePath()));
        imageChoicesPane.getChildren().clear();
        progressPane.getChildren().clear();
        gameDisplayedAt = System.currentTimeMillis();
        playScenarioCue(jeu);

        for (int i = 0; i < jeuxActifs.size(); i++) {
            Region dot = new Region();
            dot.setPrefSize(18, 18);
            dot.setStyle("-fx-background-color: " + (i <= currentIndex ? "#ff9f1c" : "#d7dce2") + "; -fx-background-radius: 99;");
            progressPane.getChildren().add(dot);
        }

        for (ImageCarte imageCarte : jeu.getImages()) {
            imageChoicesPane.getChildren().add(buildImageChoice(imageCarte, jeu));
        }
    }

    private VBox buildImageChoice(ImageCarte imageCarte, Jeu jeu) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);
        imageView.setImage(loadImage(imageCarte.getImagePath()));

        StackPane button = new StackPane(imageView);
        button.setPrefSize(190, 150);
        button.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-border-radius: 28; -fx-border-color: #ffd9a0; -fx-border-width: 3; -fx-padding: 12;");
        button.setOnMouseClicked(event -> enregistrerChoix(jeu, imageCarte));
        button.setOnMouseEntered(event -> button.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-border-radius: 28; -fx-border-color: #ffb347; -fx-border-width: 5; -fx-padding: 10;"));
        button.setOnMouseExited(event -> button.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-border-radius: 28; -fx-border-color: #ffd9a0; -fx-border-width: 3; -fx-padding: 12;"));

        VBox wrapper = new VBox(button);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private void enregistrerChoix(Jeu jeu, ImageCarte imageCarte) {
        User currentUser = UserSession.getInstance();
        Participation participation = new Participation();
        participation.setUserId(currentUser == null ? 0 : currentUser.getId());
        participation.setJeuId(jeu.getId());
        participation.setJeuTitre(jeu.getTitre());
        participation.setImageChoisieId(imageCarte.getId());
        participation.setChoixImage(imageCarte.getImagePath());
        participation.setResultatPsy(imageCarte.getInterpretationPsy());
        participation.setChoixTag(imageCarte.getComportementTag());
        participation.setSessionCode("SESSION-" + participation.getUserId() + "-" + System.currentTimeMillis());
        participation.setTempsReponseMs(Math.max(0L, System.currentTimeMillis() - gameDisplayedAt));
        participation.setDateParticipation(LocalDateTime.now());

        if (!serviceParticipation.ajouter(participation)) {
            return;
=======

    private ImageCarte selectedImage;
    private boolean    aDejaParticipeCeJeu = false;

    // ── Clé API Claude (variable d'environnement ANTHROPIC_API_KEY) ───────
    private static final String CLAUDE_API_KEY = System.getenv("ANTHROPIC_API_KEY");
    private static final String CLAUDE_MODEL   = "claude-sonnet-4-20250514";
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";

    // ── Config SMTP (variables d'environnement) ───────────────────────────
    private static final String SMTP_HOST     = System.getenv().getOrDefault("SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT     = System.getenv().getOrDefault("SMTP_PORT", "587");
    private static final String SMTP_USER     = System.getenv().getOrDefault("SMTP_USER", "");
    private static final String SMTP_PASSWORD = System.getenv().getOrDefault("SMTP_PASSWORD", "");
    private static final String SENDER_NAME   = "EMONADO – Suivi Psychologique";

    @FXML
    public void initialize() {
        if (progressIA != null) progressIA.setVisible(false);

        comboJeux.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            aDejaParticipeCeJeu = false;
            updateScenarioLabel(newVal);
            updateChoixImages(newVal);
            updateDisponibiliteLabel(newVal);
            clearMessages();
            verifierDejaParticipe(newVal);
        });

        updateUserLabel();
        refreshJeux();
        if (!comboJeux.getItems().isEmpty()) comboJeux.getSelectionModel().selectFirst();
    }

    @FXML
    private void ajouterParticipation() {
        Participation participation = validateForm();
        if (participation == null) return;

        participation.setDateParticipation(LocalDateTime.now());

        if (!serviceParticipation.ajouter(participation)) {
            showError(errorGlobal, "Erreur enregistrement : " + serviceParticipation.getLastError());
            return;
        }

        aDejaParticipeCeJeu = true;
        lockCards();
        refreshJeux();
        updateDisponibiliteLabel(serviceJeu.findById(participation.getJeuId()));

        showSuccess("🌟 Bravo ! Tu as fait ton choix. Nous allons envoyer un message à tes parents !");
        labelInterpretation.setText("⏳ Nous analysons ton choix avec notre assistant magique...");

        if (progressIA != null) progressIA.setVisible(true);

        final Participation savedParticipation = participation;
        CompletableFuture.supplyAsync(() -> generateAIReport(savedParticipation))
                .thenAccept(rapport -> Platform.runLater(() -> {
                    if (progressIA != null) progressIA.setVisible(false);
                    if (rapport == null || rapport.isBlank()) {
                        labelInterpretation.setText("✅ Analyse complète. Le rapport sera envoyé à tes parents.");
                        return;
                    }
                    savedParticipation.setResultatPsy(rapport);
                    serviceParticipation.modifier(savedParticipation);
                    labelInterpretation.setText(extraireResumeEnfant(rapport));
                    envoyerMailParent(savedParticipation, rapport);
                }));
    }

    private String generateAIReport(Participation participation) {
        try {
            Jeu jeu = serviceJeu.findById(participation.getJeuId());
            String scenario     = jeu != null ? jeu.getDescription() : "Scénario non disponible";
            String titreJeu     = jeu != null ? jeu.getTitre() : participation.getJeuTitre();
            String imageChoisie = simplifyImageLabel(participation.getImagePath());
            String interp       = participation.getResultatPsy();
            User   enfant       = UserSession.getInstance();
            String nomEnfant    = enfant != null ? enfant.getNom() + " " + enfant.getPrenom() : "l'enfant";

            String prompt = buildPrompt(nomEnfant, titreJeu, scenario, imageChoisie, interp);

            URL url = new URL(CLAUDE_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", CLAUDE_API_KEY != null ? CLAUDE_API_KEY : "");
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(60_000);

            String body = "{\"model\":\"" + CLAUDE_MODEL + "\","
                    + "\"max_tokens\":1000,"
                    + "\"messages\":[{\"role\":\"user\",\"content\":"
                    + toJsonString(prompt) + "}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return null;

            String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return extractTextFromClaudeResponse(response);

        } catch (Exception e) {
            System.err.println("[IA] Erreur Claude : " + e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String nomEnfant, String titreTest, String scenario,
                               String imageChoisie, String interpretationBase) {
        return "Tu es un psychologue bienveillant spécialisé dans le bien-être des enfants.\n\n"
                + "Un enfant prénommé " + nomEnfant + " vient de participer au test « " + titreTest + " ».\n\n"
                + "SCÉNARIO présenté : " + scenario + "\n\n"
                + "CHOIX de l'enfant : image « " + imageChoisie + " »\n\n"
                + "ANALYSE INITIALE : " + interpretationBase + "\n\n"
                + "Génère un rapport complet pour les PARENTS structuré ainsi :\n\n"
                + "1. 🧠 ÉTAT ÉMOTIONNEL OBSERVÉ\n"
                + "2. 💡 POINTS D'ATTENTION\n"
                + "3. 🌱 CONSEILS PRATIQUES POUR LES PARENTS (3 à 5 actions)\n"
                + "4. 👨‍⚕️ RECOMMANDATION (consultation psy : conseillée / recommandée / non nécessaire)\n"
                + "5. 🌟 MESSAGE DE RÉASSURANCE POUR L'ENFANT\n\n"
                + "Ton chaleureux et professionnel. Pas de diagnostic définitif.";
    }

    private String extractTextFromClaudeResponse(String json) {
        int textIdx = json.indexOf("\"text\":");
        if (textIdx < 0) return null;
        int start = json.indexOf("\"", textIdx + 7) + 1;
        int end   = json.lastIndexOf("\"");
        if (start >= end) return null;
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/");
    }

    private String extraireResumeEnfant(String rapport) {
        int idx = rapport.indexOf("🌟");
        if (idx < 0) idx = rapport.indexOf("MESSAGE DE RÉASSURANCE");
        if (idx >= 0) {
            String[] lines = rapport.substring(idx).split("\\n");
            for (int i = 1; i < lines.length; i++) {
                String l = lines[i].trim();
                if (!l.isBlank() && l.length() > 10) return "🌟 " + l;
            }
        }
        return "✅ Merci pour ta participation ! Tes parents vont recevoir un message.";
    }

    private void envoyerMailParent(Participation participation, String rapport) {
        CompletableFuture.runAsync(() -> {
            try {
                User parent = UserSession.getInstance();
                if (parent == null || parent.getEmail() == null || parent.getEmail().isBlank()) return;

                String nomEnfant = parent.getNom() + " " + parent.getPrenom();
                String dateStr   = participation.getDateParticipation()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
                String subject   = "📋 Rapport psychologique de " + nomEnfant + " — EMONADO";
                String htmlBody  = buildEmailBody(nomEnfant, participation.getJeuTitre(), dateStr, rapport);

                sendEmail(parent.getEmail(), subject, htmlBody);
                Platform.runLater(() -> showSuccess("✅ Rapport envoyé par e-mail à " + parent.getEmail()));

            } catch (Exception e) {
                System.err.println("[Mail] Erreur : " + e.getMessage());
                Platform.runLater(() -> showSuccess("✅ Analyse terminée. (Envoi mail non disponible)"));
            }
        });
    }

    private String buildEmailBody(String nomEnfant, String titreTest, String date, String rapport) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;color:#333'>"
                + "<div style='max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden'>"
                + "<div style='background:linear-gradient(135deg,#1f6f5f,#2196F3);padding:24px;color:white'>"
                + "<h1 style='margin:0;font-size:22px'>🧠 EMONADO</h1>"
                + "<p style='margin:4px 0 0;opacity:.9'>Suivi psychologique de votre enfant</p></div>"
                + "<div style='padding:28px'>"
                + "<h2 style='color:#1f6f5f'>Rapport psychologique</h2>"
                + "<p><b>Enfant :</b> " + nomEnfant + "</p>"
                + "<p><b>Test :</b> " + titreTest + "</p>"
                + "<p><b>Date :</b> " + date + "</p>"
                + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0'>"
                + "<div style='background:#f9f9f9;padding:20px;border-radius:6px;white-space:pre-wrap;line-height:1.7'>"
                + escapeHtml(rapport) + "</div>"
                + "<p style='font-size:12px;color:#999;margin-top:20px'>"
                + "Ce rapport ne remplace pas une consultation professionnelle.</p>"
                + "</div></div></body></html>";
    }

    private void sendEmail(String to, String subject, String htmlBody) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_USER, SENDER_NAME));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);
        msg.setContent(htmlBody, "text/html; charset=utf-8");
        Transport.send(msg);
    }

    private void updateScenarioLabel(Jeu jeu) {
        if (labelScenario == null) return;
        labelScenario.setText(jeu == null ? "" : "📖 " + jeu.getDescription());
    }

    private void refreshJeux() {
        List<Jeu> jeux = serviceJeu.afficherTout().stream().filter(Jeu::isActif).toList();
        comboJeux.setItems(FXCollections.observableArrayList(jeux));
    }

    private void updateChoixImages(Jeu jeu) {
        List<ImageCarte> images = jeu == null ? List.of() : serviceImageCarte.findByJeuId(jeu.getId());
        imageChoicesPane.getChildren().clear();
        selectedImage = null;
        if (selectedImageLabel != null) selectedImageLabel.setText("");
        if (labelInterpretation != null) labelInterpretation.setText("");
        for (ImageCarte image : images) imageChoicesPane.getChildren().add(buildImageCard(image));
    }

    private void updateDisponibiliteLabel(Jeu jeu) {
        if (jeu == null) { labelDisponibilite.setText(""); return; }
        String msg = "Participants : " + jeu.getNombreParticipations() + "/" + jeu.getMaxParticipants();
        if (!jeu.isDisponible()) msg += " — complet, choisissez un autre scénario";
        labelDisponibilite.setText(msg);
    }

    private VBox buildImageCard(ImageCarte imageCarte) {
        ImageView iv = new ImageView();
        iv.setFitWidth(120); iv.setFitHeight(90); iv.setPreserveRatio(true);
        try { iv.setImage(loadImage(imageCarte.getImagePath())); } catch (Exception ignored) {}

        Label lbl = new Label(simplifyImageLabel(imageCarte.getImagePath()));
        lbl.setWrapText(true); lbl.setMaxWidth(120);

        Button btn = new Button();
        btn.setGraphic(new VBox(4, iv, lbl));
        btn.setUserData(imageCarte.getId());
        btn.setStyle("-fx-background-color: white; -fx-border-color: #d9d9d9; "
                + "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
        btn.setOnAction(e -> {
            selectedImage = imageCarte;
            if (selectedImageLabel != null)
                selectedImageLabel.setText("Image choisie : " + simplifyImageLabel(imageCarte.getImagePath()));
            highlightSelectedImage();
            ajouterParticipation();
        });

        VBox w = new VBox(btn);
        w.setAlignment(Pos.CENTER);
        return w;
    }

    private Participation validateForm() {
        clearMessages();
        Jeu jeu = comboJeux.getValue();
        if (jeu == null && !comboJeux.getItems().isEmpty()) {
            jeu = comboJeux.getItems().get(0);
            comboJeux.getSelectionModel().select(jeu);
        }
        User currentUser = UserSession.getInstance();
        boolean valid = true;

        if (jeu == null)           { showError(errorJeu,   "Sélectionnez un scénario."); valid = false; }
        if (selectedImage == null) { showError(errorChoix, "Sélectionnez une image.");   valid = false; }
        if (aDejaParticipeCeJeu) {
            showError(errorGlobal, "Vous avez déjà participé à ce scénario."); valid = false;
        }
        if (jeu != null && !serviceJeu.aPlacesDisponibles(jeu.getId(), null)) {
            showError(errorGlobal, "Ce scénario est complet (3/3)."); valid = false;
        }
        if (jeu != null && currentUser != null && currentUser.getId() > 0
                && serviceParticipation.dejaParticipe(jeu.getId(), currentUser.getId())) {
            showError(errorGlobal, "Vous avez déjà participé à ce scénario.");
            aDejaParticipeCeJeu = true; valid = false;
        }
        if (!valid) return null;

        Participation p = new Participation();
        p.setUserId(currentUser == null ? 0 : currentUser.getId());
        p.setJeuId(jeu.getId());
        p.setJeuTitre(jeu.getTitre());
        p.setImageChoisieId(selectedImage.getId());
        p.setImagePath(selectedImage.getImagePath());
        p.setResultatPsy(selectedImage.getInterpretationPsy());
        return p;
    }

    private void verifierDejaParticipe(Jeu jeu) {
        if (jeu == null) return;
        User u = UserSession.getInstance();
        if (u != null && u.getId() > 0 && serviceParticipation.dejaParticipe(jeu.getId(), u.getId())) {
            aDejaParticipeCeJeu = true;
            showError(errorGlobal, "Vous avez déjà participé à ce scénario.");
            lockCards();
>>>>>>> Stashed changes
        }

        avancerVersJeuSuivant();
    }

<<<<<<< Updated upstream
    private void afficherFinSession() {
        imageChoicesPane.getChildren().clear();
        progressPane.getChildren().clear();
        sceneImageView.setImage(null);
        completionPane.setVisible(true);
        completionPane.setManaged(true);
        endSignal.setStyle("-fx-background-color: #77dd77; -fx-background-radius: 999;");

        User currentUser = UserSession.getInstance();
        if (currentUser != null && currentUser.getId() > 0 && !rapportEnvoye) {
            RapportSessionJeu rapport = sessionJeuService.genererRapportSession(currentUser.getId());
            sessionJeuService.envoyerRapportParent(currentUser, rapport);
            rapportEnvoye = true;
            completionTitle.setText("Resultat du test");
            completionSummary.setText(
                    rapport.getProfilPsychologique().getProfil()
                            + " | Score emotionnel: "
                            + rapport.getProfilPsychologique().getScoreEmotionnel()
                            + "/100"
            );
        } else {
            completionTitle.setText("Test termine");
            completionSummary.setText("La session est complete.");
        }
    }

    private void playScenarioCue(Jeu jeu) {
        String description = jeu.getDescription() == null ? "" : jeu.getDescription();
        if (!description.startsWith("SCENARIO:AUDIO_")) {
            return;
        }

        int beeps = switch (description) {
            case "SCENARIO:AUDIO_CHIEN" -> 1;
            case "SCENARIO:AUDIO_CHAT" -> 2;
            case "SCENARIO:AUDIO_LION" -> 3;
            default -> 1;
        };

        Thread cueThread = new Thread(() -> {
            for (int i = 0; i < beeps; i++) {
                Toolkit.getDefaultToolkit().beep();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        cueThread.setDaemon(true);
        cueThread.start();
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        String path = imagePath.trim().replace("\\", "/");

        try {
            if (path.startsWith("/")) {
                Image img = new Image(path);
                if (!img.isError()) {
                    return img;
                }
            }
            Image img = new Image("/" + path);
            if (!img.isError()) {
                return img;
            }
        } catch (Exception ignored) {
        }

        String[] candidates = new String[] { path, path.startsWith("/") ? path : "/" + path };
        for (String candidate : candidates) {
            var stream = getClass().getResourceAsStream(candidate);
            if (stream == null && candidate.startsWith("/")) {
                stream = getClass().getResourceAsStream(candidate.substring(1));
            }
            if (stream != null) {
                try {
                    Image img = new Image(stream);
                    if (!img.isError()) {
                        return img;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        try {
            String[] fsPaths = new String[] {
                    "src/main/resources" + (path.startsWith("/") ? path : "/" + path),
                    "target/classes" + (path.startsWith("/") ? path : "/" + path),
                    path.startsWith("/") ? "." + path : "./" + path,
            };
            for (String fsPath : fsPaths) {
                Path p = Paths.get(fsPath).normalize();
                if (Files.exists(p)) {
                    try (FileInputStream fis = new FileInputStream(p.toFile())) {
                        Image img = new Image(fis);
                        if (!img.isError()) {
                            return img;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
=======
    private void lockCards() {
        for (Node node : imageChoicesPane.getChildren()) {
            if (node instanceof VBox w && !w.getChildren().isEmpty()
                    && w.getChildren().get(0) instanceof Button b) {
                b.setDisable(true); b.setOpacity(0.5);
            }
        }
    }

    private void highlightSelectedImage() {
        for (Node node : imageChoicesPane.getChildren()) {
            if (node instanceof VBox w && !w.getChildren().isEmpty()
                    && w.getChildren().get(0) instanceof Button b) {
                boolean sel = selectedImage != null && (int) b.getUserData() == selectedImage.getId();
                b.setStyle(sel
                        ? "-fx-background-color: #fff4d6; -fx-border-color: #e7a83c; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;"
                        : "-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
            }
        }
    }

    private void updateUserLabel() {
        User u = UserSession.getInstance();
        labelUserNom.setText("Participant : " + (u == null ? "Invité" : u.getNom() + " " + u.getPrenom()));
    }

    private Image loadImage(String imagePath) throws Exception {
        if (imagePath == null || imagePath.isBlank()) return null;
        imagePath = imagePath.trim().replace("\\", "/");
        String fileName = imagePath.contains("/")
                ? imagePath.substring(imagePath.lastIndexOf('/') + 1) : imagePath;
        String[] candidates = {
                imagePath, imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/images/emotions/" + fileName, "/images/famille/" + fileName,
                "/images/ecole/" + fileName,    "/images/animaux/" + fileName,
                "/images/nature/" + fileName,   "/images/situation/" + fileName
        };
        for (String c : candidates) {
            var stream = getClass().getResourceAsStream(c);
            if (stream == null && c.startsWith("/")) stream = getClass().getResourceAsStream(c.substring(1));
            if (stream != null) return new Image(stream);
        }
        Path p = Paths.get("src/main/resources" + (imagePath.startsWith("/") ? "" : "/") + imagePath);
        if (Files.exists(p)) return new Image(new FileInputStream(p.toFile()));
        return null;
    }

    private String simplifyImageLabel(String path) {
        if (path == null || path.isBlank()) return "(image)";
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String f = idx >= 0 ? path.substring(idx + 1) : path;
        int dot = f.lastIndexOf('.');
        if (dot > 0) f = f.substring(0, dot);
        f = f.replace('_', ' ').replace('-', ' ');
        return f.isEmpty() ? "(image)" : Character.toUpperCase(f.charAt(0)) + f.substring(1);
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private String toJsonString(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r") + "\"";
    }

    private void clearMessages() {
        errorJeu.setText(""); errorChoix.setText("");
        errorGlobal.setText(""); msgSuccess.setText("");
        if (labelInterpretation != null) labelInterpretation.setText("");
    }

    private void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
    }

    private void showSuccess(String msg) {
        msgSuccess.setText(msg);
        msgSuccess.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        errorGlobal.setText("");
    }
}
>>>>>>> Stashed changes
