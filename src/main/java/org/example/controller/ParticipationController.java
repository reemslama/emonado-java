package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.entities.ImageCarte;
import org.example.entities.Jeu;
import org.example.entities.Participation;
import org.example.entities.User;
import org.example.service.ServiceImageCarte;
import org.example.service.ServiceJeu;
import org.example.service.ServiceParticipation;
import org.example.utils.UserSession;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

public class ParticipationController {
    @FXML private ComboBox<Jeu> comboJeux;
    @FXML private TilePane imageChoicesPane;
    @FXML private Label labelUserNom;
    @FXML private Label labelInterpretation;
    @FXML private Label labelDisponibilite;
    @FXML private Label selectedImageLabel;
    @FXML private Label errorJeu;
    @FXML private Label errorChoix;
    @FXML private Label errorGlobal;
    @FXML private Label msgSuccess;

    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final ServiceImageCarte serviceImageCarte = new ServiceImageCarte();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private ImageCarte selectedImage;
    private boolean aDejaParticipeCeJeu = false;

    @FXML
    public void initialize() {
        comboJeux.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            aDejaParticipeCeJeu = false;
            updateChoixImages(newValue);
            updateDisponibiliteLabel(newValue);
            errorJeu.setText("");
            clearMessages();
            verifierDejaParticipe(newValue);
        });
        updateUserLabel();
        refreshJeux();
        if (!comboJeux.getItems().isEmpty()) {
            comboJeux.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void ajouterParticipation() {
        // Verification preliminaire
        if (aDejaParticipeCeJeu) {
            showError(errorGlobal, "Deja joue - Choisissez un autre jeu.");
            return;
        }

        Participation participation = validateForm();
        if (participation == null) {
            return;
        }

        participation.setDateParticipation(LocalDateTime.now());
        System.out.println("=== Tentative enregistrement participation ===");
        System.out.println("Jeu ID: " + participation.getJeuId());
        System.out.println("Image ID: " + participation.getImageChoisieId());
        System.out.println("Resultat: " + participation.getResultatPsy());
        
        if (serviceParticipation.ajouter(participation)) {
            aDejaParticipeCeJeu = true;
            String resultat = participation.getResultatPsy();
            labelInterpretation.setText(resultat);
            lockCards();
            // NE PAS refreshJeux() pour eviter de reinitialiser le listener
            updateDisponibiliteLabel(serviceJeu.findById(participation.getJeuId()));
            System.out.println("=== Enregistrement reussi ===");
        } else {
            String details = serviceParticipation.getLastError();
            showError(errorGlobal, "Erreur enregistrement DB: " + (details == null || details.isBlank() ? "verifiez la table participation." : details));
            System.out.println("=== Erreur enregistrement: " + details + " ===");
        }
    }

    private void resetForm() {
        comboJeux.getSelectionModel().clearSelection();
        imageChoicesPane.getChildren().clear();
        selectedImage = null;
        labelInterpretation.setText("");
        labelDisponibilite.setText("");
        selectedImageLabel.setText("");
        clearMessages();
    }

    private Participation validateForm() {
        clearMessages();
        Jeu jeu = comboJeux.getValue();
        if (jeu == null && !comboJeux.getItems().isEmpty()) {
            jeu = comboJeux.getItems().get(0);
            comboJeux.getSelectionModel().select(jeu);
        }
        User currentUser = UserSession.getInstance();

        if (jeu == null) {
            showError(errorJeu, "Selectionnez un jeu.");
            return null;
        }
        if (selectedImage == null) {
            showError(errorChoix, "Selectionnez une image.");
            return null;
        }
        if (jeu != null && selectedImage != null && selectedImage.getJeuId() != jeu.getId()) {
            showError(errorChoix, "Le choix doit appartenir au jeu selectionne.");
            return null;
        }
        if (jeu != null && !serviceJeu.aPlacesDisponibles(jeu.getId(), null)) {
            showError(errorGlobal, "Ce jeu est complet (3/3). Choisissez un autre jeu.");
            return null;
        }

        String interpretation = selectedImage.getInterpretationPsy();
        labelInterpretation.setText(interpretation.isBlank() ? "Resultat non defini." : interpretation);

        Participation participation = new Participation();
        participation.setUserId(currentUser == null ? 0 : currentUser.getId());
        participation.setJeuId(jeu.getId());
        participation.setJeuTitre(jeu.getTitre());
        participation.setImageChoisieId(selectedImage.getId());
        participation.setImagePath(selectedImage.getImagePath());
        participation.setResultatPsy(interpretation);
        return participation;
    }

    private void refreshJeux() {
        List<Jeu> jeux = serviceJeu.afficherTout().stream().filter(Jeu::isActif).toList();
        comboJeux.setItems(FXCollections.observableArrayList(jeux));
    }

    private void updateChoixImages(Jeu jeu) {
        List<ImageCarte> images = jeu == null ? List.of() : serviceImageCarte.findByJeuId(jeu.getId());
        imageChoicesPane.getChildren().clear();
        if (jeu == null) {
            selectedImage = null;
            selectedImageLabel.setText("");
            labelInterpretation.setText("");
        }
        for (ImageCarte image : images) {
            imageChoicesPane.getChildren().add(buildImageCard(image));
        }
        highlightSelectedImage();
    }

    private void updateDisponibiliteLabel(Jeu jeu) {
        if (jeu == null) {
            labelDisponibilite.setText("");
            return;
        }
        String message = "Participants: " + jeu.getNombreParticipations() + "/" + jeu.getMaxParticipants();
        if (aDejaParticipeCeJeu) {
            message += " - Deja joue";
        } else if (!jeu.isDisponible()) {
            message += " - COMPLET, choisissez un autre jeu";
        }
        labelDisponibilite.setText(message);
    }

    private VBox buildImageCard(ImageCarte imageCarte) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(90);
        imageView.setPreserveRatio(true);
        boolean imageLoaded = false;

        try {
            Image image = loadImage(imageCarte.getImagePath());
            if (image != null && !image.isError()) {
                imageView.setImage(image);
                imageLoaded = true;
            }
        } catch (Exception e) {
            System.out.println("Erreur chargement image " + imageCarte.getImagePath() + ": " + e.getMessage());
        }

        if (!imageLoaded) {
            imageView.setImage(null);
        }

        Label label = new Label(simplifyImageLabel(imageCarte.getImagePath()));
        label.setWrapText(true);
        label.setMaxWidth(120);
        Label statusLabel = new Label(imageLoaded ? "" : "fichier introuvable");
        statusLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 10px;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(120);

        Button button = new Button();
        button.setGraphic(new VBox(4, imageView, label, statusLabel));
        button.setUserData(imageCarte.getId());
        button.setStyle("-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
        button.setOnAction(event -> {
            if (aDejaParticipeCeJeu) {
                showError(errorGlobal, "Deja joue - Choisissez un autre jeu.");
                return;
            }
            selectedImage = imageCarte;
            selectedImageLabel.setText("Image choisie : " + simplifyImageLabel(imageCarte.getImagePath()));
            // Afficher le resultat psychologique AVANT enregistrement
            String interpretation = imageCarte.getInterpretationPsy();
            labelInterpretation.setText(interpretation.isBlank() ? "Resultat non defini." : interpretation);
            highlightSelectedImage();
            ajouterParticipation();
        });

        VBox wrapper = new VBox(button);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private void lockCards() {
        for (Node node : imageChoicesPane.getChildren()) {
            if (node instanceof VBox wrapper && !wrapper.getChildren().isEmpty()
                    && wrapper.getChildren().get(0) instanceof Button button) {
                button.setDisable(true);
                button.setOpacity(0.5);
            }
        }
    }

    private void verifierDejaParticipe(Jeu jeu) {
        if (jeu == null) return;
        User currentUser = UserSession.getInstance();
        if (currentUser != null && currentUser.getId() > 0
                && serviceParticipation.dejaParticipe(jeu.getId(), currentUser.getId())) {
            aDejaParticipeCeJeu = true;
            showError(errorGlobal, "Deja joue - Vous avez deja participe a ce jeu. Choisissez un autre jeu.");
            lockCards();
        }
    }

    private void highlightSelectedImage() {
        for (Node node : imageChoicesPane.getChildren()) {
            if (node instanceof VBox wrapper && !wrapper.getChildren().isEmpty() && wrapper.getChildren().get(0) instanceof Button button) {
                int imageId = (int) button.getUserData();
                boolean selected = selectedImage != null && imageId == selectedImage.getId();
                button.setStyle(selected
                        ? "-fx-background-color: #fff4d6; -fx-border-color: #e7a83c; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;"
                        : "-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
            }
        }
    }

    private String simplifyImageLabel(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "(image)";
        }
        int slashIndex = Math.max(imagePath.lastIndexOf('/'), imagePath.lastIndexOf('\\'));
        String fileName = slashIndex >= 0 ? imagePath.substring(slashIndex + 1) : imagePath;
        // Remove extension and replace underscores/hyphens with spaces, capitalize
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        fileName = fileName.replace('_', ' ').replace('-', ' ');
        if (!fileName.isEmpty()) {
            fileName = Character.toUpperCase(fileName.charAt(0)) + fileName.substring(1);
        }
        return fileName;
    }

    private void updateUserLabel() {
        User currentUser = UserSession.getInstance();
        labelUserNom.setText("Participant (Enfant) : " + (currentUser == null ? "Invite" : currentUser.getNom() + " " + currentUser.getPrenom()));
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        String path = imagePath.trim().replace("\\", "/");

        // Essayer directement via le classpath JavaFX (fonctionne avec file: ou chemin relatif)
        try {
            // Si le chemin commence déjà par /, l'utiliser tel quel
            if (path.startsWith("/")) {
                Image img = new Image(path);
                if (!img.isError()) {
                    return img;
                }
            }
            // Sinon essayer avec / devant
            Image img = new Image("/" + path);
            if (!img.isError()) {
                return img;
            }
        } catch (Exception e) {
            // Continuer avec les autres méthodes
        }

        // Essayer via getResourceAsStream
        String[] candidates = new String[] {
                path,
                path.startsWith("/") ? path : "/" + path,
        };

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

        // Essayer via le système de fichiers
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

        System.out.println("Image non trouvée: " + imagePath);
        return null;
    }

    private void clearMessages() {
        errorJeu.setText("");
        errorChoix.setText("");
        errorGlobal.setText("");
        msgSuccess.setText("");
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        msgSuccess.setText(message);
        msgSuccess.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        errorGlobal.setText("");
    }
}
