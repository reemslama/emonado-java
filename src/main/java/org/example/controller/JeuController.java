package org.example.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.entities.ImageCarte;
import org.example.entities.Jeu;
import org.example.service.ServiceImageCarte;
import org.example.service.ServiceJeu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JeuController {
    private static final Pattern TEXT_PATTERN = Pattern.compile("[-\\p{L}0-9'(),.!?\\s/]+");

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtSceneImagePath;
    @FXML private TextArea txtCheminsImages;
    @FXML private TextArea txtInterpretationBase;
    @FXML private TextArea txtTagsComportement;
    @FXML private TextField txtMaxParticipants;
    @FXML private CheckBox checkActif;
    @FXML private Label errorTitre;
    @FXML private Label errorDescription;
    @FXML private Label errorScene;
    @FXML private Label errorImages;
    @FXML private Label errorInterpretation;
    @FXML private Label errorTags;
    @FXML private Label errorMaxParticipants;
    @FXML private Label errorGlobal;
    @FXML private Label msgSuccess;
    @FXML private TableView<Jeu> tableJeux;
    @FXML private TableColumn<Jeu, String> colTitre;
    @FXML private TableColumn<Jeu, String> colParticipants;
    @FXML private TableColumn<Jeu, String> colEtat;
    @FXML private TableColumn<Jeu, String> colImages;

    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final ServiceImageCarte serviceImageCarte = new ServiceImageCarte();
    private Jeu selectedJeu;

    @FXML
    public void initialize() {
        colTitre.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitre()));
        colParticipants.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getNombreParticipations() + "/" + data.getValue().getMaxParticipants()
        ));
        colEtat.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isDisponible() ? "Disponible" : "Indisponible"));
        colImages.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getImages().stream()
                        .map(ImageCarte::getImagePath)
                        .collect(Collectors.joining(", "))
        ));

        tableJeux.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedJeu = newValue;
            populateForm(newValue);
        });

        txtMaxParticipants.setText("3");
        txtMaxParticipants.setDisable(true);
        checkActif.setSelected(true);
        txtSceneImagePath.setPromptText("/images/situation/enfants_groupe.png");
        txtTagsComportement.setPromptText("jouer\nrester_seul\npeur");
        serviceImageCarte.migrerAnciennesImages();
        refreshTable();
    }

    @FXML
    private void ajouterJeu() {
        Jeu jeu = validateForm();
        if (jeu == null) {
            return;
        }

        if (serviceJeu.ajouter(jeu)) {
            Jeu jeuSaved = serviceJeu.afficherTout().stream()
                    .filter(item -> item.getTitre().equalsIgnoreCase(jeu.getTitre()))
                    .findFirst()
                    .orElse(null);
            if (jeuSaved != null && !saveImagesForJeu(jeuSaved.getId())) {
                showError(errorGlobal, "Jeu ajoute, mais erreur lors de l'ajout des options.");
                return;
            }
            showSuccess("Jeu ajoute avec succes.");
            resetForm();
            refreshTable();
        } else {
            showError(errorGlobal, "Erreur lors de l'ajout du jeu.");
        }
    }

    @FXML
    private void modifierJeu() {
        if (selectedJeu == null) {
            showError(errorGlobal, "Selectionnez un jeu.");
            return;
        }

        Jeu jeu = validateForm();
        if (jeu == null) {
            return;
        }

        jeu.setId(selectedJeu.getId());
        if (serviceJeu.modifier(jeu)) {
            List<ImageCarte> oldImages = serviceImageCarte.findByJeuId(selectedJeu.getId());
            for (ImageCarte old : oldImages) {
                serviceImageCarte.supprimer(old.getId());
            }
            if (!saveImagesForJeu(selectedJeu.getId())) {
                showError(errorGlobal, "Jeu modifie, mais erreur lors de l'ajout des options.");
                return;
            }
            showSuccess("Jeu modifie avec succes.");
            resetForm();
            refreshTable();
        } else {
            showError(errorGlobal, "Erreur lors de la modification du jeu.");
        }
    }

    @FXML
    private void supprimerJeu() {
        if (selectedJeu == null) {
            showError(errorGlobal, "Selectionnez un jeu.");
            return;
        }

        if (serviceJeu.supprimer(selectedJeu.getId())) {
            showSuccess("Jeu supprime avec succes.");
            resetForm();
            refreshTable();
        } else {
            showError(errorGlobal, "Suppression impossible. Verifiez les participations liees.");
        }
    }

    private void resetForm() {
        selectedJeu = null;
        txtTitre.clear();
        txtDescription.clear();
        txtSceneImagePath.clear();
        txtCheminsImages.clear();
        txtInterpretationBase.clear();
        txtTagsComportement.clear();
        txtMaxParticipants.setText("3");
        checkActif.setSelected(true);
        clearMessages();
        tableJeux.getSelectionModel().clearSelection();
    }

    private Jeu validateForm() {
        clearMessages();
        boolean valid = true;

        String titre = normalizeText(txtTitre.getText());
        String description = normalizeText(txtDescription.getText());
        String sceneImagePath = normalizePath(txtSceneImagePath.getText());
        List<String> imagePaths = parseMultilineValues(txtCheminsImages.getText());
        List<String> interpretations = parseMultilineValues(txtInterpretationBase.getText());
        List<String> tags = parseTags(txtTagsComportement.getText());

        if (!isValidText(titre, 3, 80)) {
            showError(errorTitre, "Titre obligatoire, 3 a 80 caracteres.");
            valid = false;
        }

        if (!isValidText(description, 10, 400)) {
            showError(errorDescription, "Description obligatoire, 10 a 400 caracteres.");
            valid = false;
        }

        if (sceneImagePath == null || sceneImagePath.isBlank() || sceneImagePath.length() > 255) {
            showError(errorScene, "Image de scene obligatoire.");
            valid = false;
        }

        if (imagePaths.size() < 2 || imagePaths.size() > 3) {
            showError(errorImages, "Chaque scene doit proposer 2 a 3 options.");
            valid = false;
        }

        if (interpretations.size() != imagePaths.size()) {
            showError(errorInterpretation, "Une interpretation par image est requise.");
            valid = false;
        }

        if (tags.size() != imagePaths.size()) {
            showError(errorTags, "Un tag comportemental par image est requis.");
            valid = false;
        }

        Integer maxParticipants = parseMaxParticipants();
        if (maxParticipants == null) {
            valid = false;
        }

        Integer excludedId = selectedJeu == null ? null : selectedJeu.getId();
        if (titre != null && !titre.isBlank() && serviceJeu.existeTitre(titre, excludedId)) {
            showError(errorGlobal, "Un jeu avec ce titre existe deja.");
            valid = false;
        }

        if (!valid) {
            return null;
        }

        Jeu jeu = new Jeu();
        jeu.setTitre(titre);
        jeu.setDescription(description);
        jeu.setSceneImagePath(sceneImagePath);
        jeu.setMaxParticipants(maxParticipants);
        jeu.setActif(checkActif.isSelected());
        return jeu;
    }

    private Integer parseMaxParticipants() {
        String value = txtMaxParticipants.getText() == null ? "" : txtMaxParticipants.getText().trim();
        if (!value.matches("\\d+")) {
            showError(errorMaxParticipants, "Nombre entier requis.");
            return null;
        }

        int max = Integer.parseInt(value);
        if (max != 3) {
            showError(errorMaxParticipants, "Chaque jeu accepte exactement 3 participants.");
            return null;
        }
        return max;
    }

    private boolean isValidText(String value, int min, int max) {
        return value != null && value.length() >= min && value.length() <= max && TEXT_PATTERN.matcher(value).matches();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizePath(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replace("\\", "/");
    }

    private void populateForm(Jeu jeu) {
        if (jeu == null) {
            return;
        }

        txtTitre.setText(jeu.getTitre());
        txtDescription.setText(jeu.getDescription());
        txtSceneImagePath.setText(jeu.getSceneImagePath());
        txtCheminsImages.setText(jeu.getImages().stream().map(ImageCarte::getImagePath).collect(Collectors.joining("\n")));
        txtInterpretationBase.setText(jeu.getImages().stream().map(ImageCarte::getInterpretationPsy).collect(Collectors.joining("\n")));
        txtTagsComportement.setText(jeu.getImages().stream().map(ImageCarte::getComportementTag).collect(Collectors.joining("\n")));
        txtMaxParticipants.setText(String.valueOf(jeu.getMaxParticipants()));
        checkActif.setSelected(jeu.isActif());
        clearMessages();
    }

    private void refreshTable() {
        List<Jeu> jeux = serviceJeu.afficherTout();
        for (Jeu jeu : jeux) {
            jeu.setImages(serviceImageCarte.findByJeuId(jeu.getId()));
        }
        tableJeux.setItems(FXCollections.observableArrayList(jeux));
    }

    private boolean saveImagesForJeu(int jeuId) {
        List<String> imagePaths = parseMultilineValues(txtCheminsImages.getText());
        List<String> interpretations = parseMultilineValues(txtInterpretationBase.getText());
        List<String> tags = parseTags(txtTagsComportement.getText());
        if (imagePaths.size() != interpretations.size() || imagePaths.size() != tags.size()) {
            return false;
        }

        for (int i = 0; i < imagePaths.size(); i++) {
            ImageCarte imageCarte = new ImageCarte();
            imageCarte.setJeuId(jeuId);
            imageCarte.setImagePath(imagePaths.get(i));
            imageCarte.setInterpretationPsy(interpretations.get(i));
            imageCarte.setComportementTag(tags.get(i));
            if (!serviceImageCarte.ajouter(imageCarte)) {
                return false;
            }
        }
        return true;
    }

    private List<String> parseMultilineValues(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String line : rawText.split("\\R")) {
            String normalized = normalizePath(line);
            if (normalized != null && !normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private List<String> parseTags(String rawText) {
        List<String> values = parseMultilineValues(rawText);
        return values.stream()
                .map(value -> value.trim().toLowerCase().replace(' ', '_'))
                .collect(Collectors.toList());
    }

    private void clearMessages() {
        errorTitre.setText("");
        errorDescription.setText("");
        errorScene.setText("");
        errorImages.setText("");
        errorInterpretation.setText("");
        errorTags.setText("");
        errorMaxParticipants.setText("");
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
