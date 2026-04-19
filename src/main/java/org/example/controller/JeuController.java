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
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JeuController {
    private static final Pattern TEXT_PATTERN = Pattern.compile("[-\\p{L}0-9'(),.!?\\s]+");

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private TextArea txtCheminsImages;
    @FXML private TextArea txtInterpretationBase;
    @FXML private TextField txtMaxParticipants;
    @FXML private CheckBox checkActif;
    @FXML private Label errorTitre;
    @FXML private Label errorDescription;
    @FXML private Label errorImages;
    @FXML private Label errorInterpretation;
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
        txtCheminsImages.setPromptText("Chemins des images (un par ligne, ex: /images/animaux/lion.png)");
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
                showError(errorGlobal, "Jeu ajoute, mais erreur lors de l'ajout des images.");
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
                showError(errorGlobal, "Jeu modifie, mais erreur lors de l'ajout des images.");
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
        txtCheminsImages.clear();
        txtInterpretationBase.clear();
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
        List<String> imagePaths = parseMultilineValues(txtCheminsImages.getText());
        List<String> interpretations = parseMultilineValues(txtInterpretationBase.getText());

        if (!isValidText(titre, 3, 80)) {
            showError(errorTitre, "Titre obligatoire, 3 a 80 caracteres.");
            valid = false;
        }

        if (!isValidText(description, 10, 400)) {
            showError(errorDescription, "Description obligatoire, 10 a 400 caracteres.");
            valid = false;
        }

        if (imagePaths.isEmpty()) {
            showError(errorImages, "Entrez au moins un chemin d'image.");
            valid = false;
        }

        if (interpretations.isEmpty() || interpretations.size() != imagePaths.size()) {
            showError(errorInterpretation, "Entrez " + imagePaths.size() + " interpretations (1 ligne par image).");
            valid = false;
        }

        for (String imagePath : imagePaths) {
            if (imagePath.length() > 255) {
                showError(errorImages, "Chaque chemin d'image doit faire max 255 caracteres.");
                valid = false;
                break;
            }
        }

        for (String interpretation : interpretations) {
            if (!isValidText(interpretation, 5, 255)) {
                showError(errorInterpretation, "Chaque interpretation doit faire 5 a 255 caracteres.");
                valid = false;
                break;
            }
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

    private void populateForm(Jeu jeu) {
        if (jeu == null) {
            return;
        }

        txtTitre.setText(jeu.getTitre());
        txtDescription.setText(jeu.getDescription());
        txtCheminsImages.setText(jeu.getImages().stream().map(ImageCarte::getImagePath).collect(Collectors.joining("\n")));
        txtInterpretationBase.setText(jeu.getImages().stream().map(ImageCarte::getInterpretationPsy).collect(Collectors.joining("\n")));
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
        if (imagePaths.size() != interpretations.size()) {
            return false;
        }

        for (int i = 0; i < imagePaths.size(); i++) {
            ImageCarte imageCarte = new ImageCarte();
            imageCarte.setJeuId(jeuId);
            imageCarte.setImagePath(imagePaths.get(i));
            imageCarte.setInterpretationPsy(interpretations.get(i));
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
            String normalized = normalizeText(line);
            if (normalized != null && !normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private void clearMessages() {
        errorTitre.setText("");
        errorDescription.setText("");
        errorImages.setText("");
        errorInterpretation.setText("");
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
