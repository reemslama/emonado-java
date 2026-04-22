package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParticipationController {
    @FXML private ImageView sceneImageView;
    @FXML private FlowPane imageChoicesPane;
    @FXML private FlowPane progressPane;
    @FXML private StackPane completionPane;
    @FXML private Region endSignal;
    @FXML private Label completionTitle;
    @FXML private Label completionSummary;

    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final ServiceImageCarte serviceImageCarte = new ServiceImageCarte();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
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
        }

        avancerVersJeuSuivant();
    }

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
