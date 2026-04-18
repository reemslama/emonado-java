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
        Participation participation = validateForm();
        if (participation == null) {
            return;
        }

        participation.setDateParticipation(LocalDateTime.now());
        if (serviceParticipation.ajouter(participation)) {
            aDejaParticipeCeJeu = true;
            String resultat = participation.getResultatPsy();
            labelInterpretation.setText(resultat);
            showSuccess("Choix enregistre !");
            lockCards();
            refreshJeux();
            Jeu jeuActuel = comboJeux.getValue();
            if (jeuActuel != null) {
                updateDisponibiliteLabel(serviceJeu.findById(jeuActuel.getId()));
            }
        } else {
            String details = serviceParticipation.getLastError();
            showError(errorGlobal, "Erreur enregistrement DB: " + (details == null || details.isBlank() ? "verifiez la table participation." : details));
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
        boolean valid = true;

        if (jeu == null) {
            showError(errorJeu, "Selectionnez un jeu.");
            valid = false;
        }
        if (selectedImage == null) {
            showError(errorChoix, "Selectionnez une image.");
            valid = false;
        }
        if (aDejaParticipeCeJeu) {
            showError(errorGlobal, "Vous avez deja participe a ce jeu. Choisissez un autre jeu.");
            valid = false;
        }
        if (jeu != null && !serviceJeu.aPlacesDisponibles(jeu.getId(), null)) {
            showError(errorGlobal, "Ce jeu est complet (3/3). Choisissez un autre jeu.");
            valid = false;
        }
        if (jeu != null && currentUser != null && currentUser.getId() > 0
                && serviceParticipation.dejaParticipe(jeu.getId(), currentUser.getId())) {
            showError(errorGlobal, "Vous avez deja participe a ce jeu. Choisissez un autre jeu.");
            aDejaParticipeCeJeu = true;
            valid = false;
        }
        if (jeu != null && selectedImage != null && selectedImage.getJeuId() != jeu.getId()) {
            showError(errorChoix, "Le choix doit appartenir au jeu selectionne.");
            valid = false;
        }
        if (!valid) {
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
        if (!jeu.isDisponible()) {
            message += " - complet, choisissez un autre jeu";
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
            imageView.setImage(image);
            imageLoaded = image != null;
        } catch (Exception e) {
            imageView.setImage(null);
        }

        Label label = new Label(simplifyImageLabel(imageCarte.getImagePath()));
        label.setWrapText(true);
        label.setMaxWidth(120);
        Label missingLabel = new Label(imageLoaded ? "" : "fichier introuvable");
        missingLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 10px;");
        missingLabel.setWrapText(true);
        missingLabel.setMaxWidth(120);

        Button button = new Button();
        button.setGraphic(new VBox(4, imageView, label, missingLabel));
        button.setUserData(imageCarte.getId());
        button.setStyle("-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
        button.setOnAction(event -> {
            selectedImage = imageCarte;
            selectedImageLabel.setText("Image choisie : " + simplifyImageLabel(imageCarte.getImagePath()));
            labelInterpretation.setText(imageCarte.getInterpretationPsy());
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
            showError(errorGlobal, "Vous avez deja participe a ce jeu. Choisissez un autre jeu.");
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

    private Image loadImage(String imagePath) throws Exception {
        if (imagePath == null || imagePath.isBlank()) {
            return loadPlaceholderImage();
        }
        imagePath = imagePath.trim();
        if (imagePath.startsWith("file:") || imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return new Image(imagePath);
        }

        String normalized = imagePath.replace("\\", "/");
        String fromResourcesRoot = normalized;
        int resourcesPrefixIndex = normalized.indexOf("src/main/resources/");
        if (resourcesPrefixIndex >= 0) {
            fromResourcesRoot = normalized.substring(resourcesPrefixIndex + "src/main/resources/".length());
        }
        String fileName = extractFileName(normalized);

        String[] candidates = new String[] {
                normalized,
                fromResourcesRoot,
                normalized.startsWith("/") ? normalized : "/" + normalized,
                fromResourcesRoot.startsWith("/") ? fromResourcesRoot : "/" + fromResourcesRoot,
                normalized.startsWith("/images/") ? normalized : "/images/" + fileName,
                "/images/animaux/" + fileName,
                "/images/nature/" + fileName
                ,
                "/images/situation/" + fileName
        };

        for (String candidate : candidates) {
            var classpathStream = getClass().getResourceAsStream(candidate);
            if (classpathStream == null && candidate.startsWith("/")) {
                classpathStream = getClass().getResourceAsStream(candidate.substring(1));
            }
            if (classpathStream != null) {
                return new Image(classpathStream);
            }
        }

        try {
            Path directPath = resolvePathFromRuntime(normalized);
            if (directPath != null && Files.exists(directPath)) {
                return new Image(new FileInputStream(directPath.toFile()));
            }
        } catch (Exception ignored) {}

        for (String candidate : candidates) {
            try {
                String localPath = "src/main/resources/" + (candidate.startsWith("/") ? candidate.substring(1) : candidate);
                Path candidatePath = resolvePathFromRuntime(localPath);
                if (Files.exists(candidatePath)) {
                    return new Image(new FileInputStream(candidatePath.toFile()));
                }
            } catch (Exception ignoredAgain) {
            }
        }
        return loadPlaceholderImage();
    }

    private Image loadPlaceholderImage() {
        return null;
    }

    private Path resolvePathFromRuntime(String rawPath) {
        Path givenPath = Paths.get(rawPath);
        if (givenPath.isAbsolute() && Files.exists(givenPath)) {
            return givenPath;
        }

        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path candidate = cwd.resolve(rawPath).normalize();
        if (Files.exists(candidate)) {
            return candidate;
        }

        // If app is launched from target/classes or another subdir, remonte parents.
        Path cursor = cwd;
        for (int i = 0; i < 8 && cursor != null; i++) {
            Path parentCandidate = cursor.resolve(rawPath).normalize();
            if (Files.exists(parentCandidate)) {
                return parentCandidate;
            }
            cursor = cursor.getParent();
        }
        return candidate;
    }

    private String extractFileName(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        String normalized = imagePath.replace("\\", "/");
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
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
