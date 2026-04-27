package org.example.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.entities.ImageCarte;
import org.example.entities.Jeu;
import org.example.service.ServiceImageCarte;
import org.example.service.ServiceJeu;

<<<<<<< Updated upstream
import java.util.ArrayList;
import java.util.List;
=======
import java.util.*;
>>>>>>> Stashed changes
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JeuController {
<<<<<<< Updated upstream
    private static final Pattern TEXT_PATTERN = Pattern.compile("[-\\p{L}0-9'(),.!?\\s/]+");

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtSceneImagePath;
    @FXML private TextArea txtCheminsImages;
    @FXML private TextArea txtInterpretationBase;
    @FXML private TextArea txtTagsComportement;
    @FXML private TextField txtMaxParticipants;
    @FXML private CheckBox checkActif;
=======

    private static final Pattern TEXT_PATTERN = Pattern.compile("[-\\p{L}0-9'(),.!?\\s]+");

    @FXML private TextField  txtTitre;
    @FXML private TextArea   txtDescription;
    @FXML private TextArea   txtImagesAuto;
    @FXML private TextArea   txtInterpretationBase;
    @FXML private TextField  txtMaxParticipants;
    @FXML private CheckBox   checkActif;
>>>>>>> Stashed changes
    @FXML private Label errorTitre;
    @FXML private Label errorDescription;
    @FXML private Label errorScene;
    @FXML private Label errorImages;
    @FXML private Label errorInterpretation;
    @FXML private Label errorTags;
    @FXML private Label errorMaxParticipants;
    @FXML private Label errorGlobal;
    @FXML private Label msgSuccess;
    @FXML private TableView<Jeu>          tableJeux;
    @FXML private TableColumn<Jeu,String> colTitre;
    @FXML private TableColumn<Jeu,String> colParticipants;
    @FXML private TableColumn<Jeu,String> colEtat;
    @FXML private TableColumn<Jeu,String> colImages;

    private final ServiceJeu        serviceJeu        = new ServiceJeu();
    private final ServiceImageCarte serviceImageCarte = new ServiceImageCarte();
    private Jeu selectedJeu;
<<<<<<< Updated upstream
=======

    private static final String THEME_EMOTIONS  = "emotions";
    private static final String THEME_FAMILLE   = "famille";
    private static final String THEME_ECOLE     = "ecole";
    private static final String THEME_ANIMAUX   = "animaux";
    private static final String THEME_NATURE    = "nature";
    private static final String THEME_SITUATION = "situation";

    private static final Map<String, List<String>> STATIC_THEME_IMAGES = Map.of(
            THEME_EMOTIONS, List.of(
                    "/images/emotions/joie.png",
                    "/images/emotions/tristesse.png",
                    "/images/emotions/colere.png"
            ),
            THEME_FAMILLE, List.of(
                    "/images/famille/famille_heureuse.png",
                    "/images/famille/enfant_seul.png",
                    "/images/famille/dispute.png"
            ),
            THEME_ECOLE, List.of(
                    "/images/ecole/enfant_classe.png",
                    "/images/ecole/enfants_groupe.png",
                    "/images/ecole/enfant_dessin.png"
            ),
            THEME_ANIMAUX, List.of(
                    "/images/animaux/lion.png",
                    "/images/animaux/chat.png",
                    "/images/animaux/chien.png"
            ),
            THEME_NATURE, List.of(
                    "/images/nature/soleil.png",
                    "/images/nature/ciel.png",
                    "/images/nature/arbre.png"
            ),
            THEME_SITUATION, List.of(
                    "/images/situation/enfant_seul.png",
                    "/images/situation/enfant_dessin.png",
                    "/images/situation/enfants_groupe.png"
            )
    );
>>>>>>> Stashed changes

    @FXML
    public void initialize() {
        colTitre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitre()));
        colParticipants.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getNombreParticipations() + "/" + d.getValue().getMaxParticipants()
        ));
        colEtat.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().isDisponible() ? "✅ Disponible" : "🔒 Complet"
        ));
        colImages.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getImages().stream()
                        .map(ImageCarte::getImagePath)
                        .collect(Collectors.joining(", "))
        ));

<<<<<<< Updated upstream
        tableJeux.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedJeu = newValue;
            populateForm(newValue);
        });
=======
        tableJeux.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> { selectedJeu = newVal; populateForm(newVal); }
        );
        txtTitre.textProperty().addListener(
                (obs, old, newVal) -> updateAutoImagesPreview(newVal)
        );
>>>>>>> Stashed changes

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
        if (jeu == null) return;

        if (serviceJeu.ajouter(jeu)) {
<<<<<<< Updated upstream
            Jeu jeuSaved = serviceJeu.afficherTout().stream()
                    .filter(item -> item.getTitre().equalsIgnoreCase(jeu.getTitre()))
                    .findFirst()
                    .orElse(null);
            if (jeuSaved != null && !saveImagesForJeu(jeuSaved.getId())) {
                showError(errorGlobal, "Jeu ajoute, mais erreur lors de l'ajout des options.");
=======
            Jeu saved = serviceJeu.afficherTout().stream()
                    .filter(j -> j.getTitre().equalsIgnoreCase(jeu.getTitre()))
                    .findFirst().orElse(null);
            if (saved != null && !saveImagesForJeu(saved.getId(), jeu.getTitre())) {
                showError(errorGlobal, "Scénario ajouté, mais erreur lors de l'ajout des images.");
>>>>>>> Stashed changes
                return;
            }
            showSuccess("✅ Scénario ajouté avec succès.");
            resetForm();
            refreshTable();
        } else {
            showError(errorGlobal, "Erreur lors de l'ajout du scénario.");
        }
    }

    @FXML
    private void modifierJeu() {
        if (selectedJeu == null) { showError(errorGlobal, "Sélectionnez un scénario."); return; }
        Jeu jeu = validateForm();
        if (jeu == null) return;
        jeu.setId(selectedJeu.getId());
        if (serviceJeu.modifier(jeu)) {
<<<<<<< Updated upstream
            List<ImageCarte> oldImages = serviceImageCarte.findByJeuId(selectedJeu.getId());
            for (ImageCarte old : oldImages) {
                serviceImageCarte.supprimer(old.getId());
            }
            if (!saveImagesForJeu(selectedJeu.getId())) {
                showError(errorGlobal, "Jeu modifie, mais erreur lors de l'ajout des options.");
=======
            serviceImageCarte.findByJeuId(selectedJeu.getId())
                    .forEach(img -> serviceImageCarte.supprimer(img.getId()));
            if (!saveImagesForJeu(selectedJeu.getId(), jeu.getTitre())) {
                showError(errorGlobal, "Scénario modifié, mais erreur lors de la mise à jour des images.");
>>>>>>> Stashed changes
                return;
            }
            showSuccess("✅ Scénario modifié avec succès.");
            resetForm();
            refreshTable();
        } else {
            showError(errorGlobal, "Erreur lors de la modification.");
        }
    }

    @FXML
    private void supprimerJeu() {
        if (selectedJeu == null) { showError(errorGlobal, "Sélectionnez un scénario."); return; }
        if (serviceJeu.supprimer(selectedJeu.getId())) {
            showSuccess("✅ Scénario supprimé avec succès.");
            resetForm();
            refreshTable();
        } else {
            showError(errorGlobal, "Suppression impossible. Vérifiez les participations liées.");
        }
    }

    private Jeu validateForm() {
        clearMessages();
        boolean valid = true;
        String titre       = normalizeText(txtTitre.getText());
        String description = normalizeText(txtDescription.getText());
        List<String> imagePaths    = getStaticImagesForTitre(titre);
        List<String> interpretations = parseMultilineValues(txtInterpretationBase.getText());

        if (!isValidText(titre, 3, 80)) {
            showError(errorTitre, "Titre obligatoire, 3 à 80 caractères."); valid = false;
        }
        if (!isValidText(description, 10, 500)) {
            showError(errorDescription, "Scénario obligatoire, 10 à 500 caractères."); valid = false;
        }
        if (imagePaths.isEmpty()) {
            showError(errorImages, "Impossible de détecter les images du thème."); valid = false;
        }
        if (interpretations.isEmpty() || interpretations.size() != imagePaths.size()) {
            showError(errorInterpretation,
                    "Entrez exactement " + imagePaths.size() + " interprétations (1 ligne par image).");
            valid = false;
        }
        for (String interp : interpretations) {
            if (!isValidText(interp, 5, 500)) {
                showError(errorInterpretation, "Chaque interprétation : 5 à 500 caractères.");
                valid = false; break;
            }
        }
        Integer maxP = parseMaxParticipants();
        if (maxP == null) valid = false;

        Integer excludedId = selectedJeu == null ? null : selectedJeu.getId();
        if (titre != null && !titre.isBlank() && serviceJeu.existeTitre(titre, excludedId)) {
            showError(errorGlobal, "Un scénario avec ce titre existe déjà."); valid = false;
        }
        if (!valid) return null;

        Jeu jeu = new Jeu();
        jeu.setTitre(titre);
        jeu.setDescription(description);
        jeu.setMaxParticipants(maxP);
        jeu.setActif(checkActif.isSelected());
        return jeu;
    }

    private Integer parseMaxParticipants() {
        String v = txtMaxParticipants.getText() == null ? "" : txtMaxParticipants.getText().trim();
        if (!v.matches("\\d+")) { showError(errorMaxParticipants, "Nombre entier requis."); return null; }
        int max = Integer.parseInt(v);
        if (max != 3) { showError(errorMaxParticipants, "Chaque scénario accepte exactement 3 participants."); return null; }
        return max;
    }

    private void resetForm() {
        selectedJeu = null;
<<<<<<< Updated upstream
        txtTitre.clear();
        txtDescription.clear();
        txtSceneImagePath.clear();
        txtCheminsImages.clear();
        txtInterpretationBase.clear();
        txtTagsComportement.clear();
=======
        txtTitre.clear(); txtDescription.clear();
        txtImagesAuto.clear(); txtInterpretationBase.clear();
>>>>>>> Stashed changes
        txtMaxParticipants.setText("3");
        checkActif.setSelected(true);
        clearMessages();
        tableJeux.getSelectionModel().clearSelection();
    }

<<<<<<< Updated upstream
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

=======
>>>>>>> Stashed changes
    private void populateForm(Jeu jeu) {
        if (jeu == null) return;
        txtTitre.setText(jeu.getTitre());
        txtDescription.setText(jeu.getDescription());
<<<<<<< Updated upstream
        txtSceneImagePath.setText(jeu.getSceneImagePath());
        txtCheminsImages.setText(jeu.getImages().stream().map(ImageCarte::getImagePath).collect(Collectors.joining("\n")));
        txtInterpretationBase.setText(jeu.getImages().stream().map(ImageCarte::getInterpretationPsy).collect(Collectors.joining("\n")));
        txtTagsComportement.setText(jeu.getImages().stream().map(ImageCarte::getComportementTag).collect(Collectors.joining("\n")));
=======
        updateAutoImagesPreview(jeu.getTitre());
        txtInterpretationBase.setText(
                jeu.getImages().stream()
                        .map(ImageCarte::getInterpretationPsy)
                        .collect(Collectors.joining("\n"))
        );
>>>>>>> Stashed changes
        txtMaxParticipants.setText(String.valueOf(jeu.getMaxParticipants()));
        checkActif.setSelected(jeu.isActif());
        clearMessages();
    }

    private void refreshTable() {
        List<Jeu> jeux = serviceJeu.afficherTout();
        jeux.forEach(j -> j.setImages(serviceImageCarte.findByJeuId(j.getId())));
        tableJeux.setItems(FXCollections.observableArrayList(jeux));
    }

<<<<<<< Updated upstream
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
=======
    private boolean saveImagesForJeu(int jeuId, String titre) {
        List<String> paths   = getStaticImagesForTitre(titre);
        List<String> interps = parseMultilineValues(txtInterpretationBase.getText());
        if (paths.size() != interps.size()) return false;
        for (int i = 0; i < paths.size(); i++) {
            ImageCarte ic = new ImageCarte();
            ic.setJeuId(jeuId);
            ic.setImagePath(paths.get(i));
            ic.setInterpretationPsy(interps.get(i));
            if (!serviceImageCarte.ajouter(ic)) return false;
>>>>>>> Stashed changes
        }
        return true;
    }

<<<<<<< Updated upstream
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
=======
    private void updateAutoImagesPreview(String titre) {
        List<String> paths = getStaticImagesForTitre(titre);
        String theme = resolveThemeFromTitre(titre);
        txtImagesAuto.setText("Thème détecté : " + theme + "\n" + String.join("\n", paths));
    }

    private List<String> getStaticImagesForTitre(String titre) {
        return STATIC_THEME_IMAGES.getOrDefault(
                resolveThemeFromTitre(titre), STATIC_THEME_IMAGES.get(THEME_ANIMAUX)
        );
    }

    private String resolveThemeFromTitre(String titre) {
        String t = titre == null ? "" : titre.toLowerCase();
        if (t.contains("émot") || t.contains("emot") || t.contains("sentiment") || t.contains("ressent"))
            return THEME_EMOTIONS;
        if (t.contains("famille") || t.contains("parent") || t.contains("maison"))
            return THEME_FAMILLE;
        if (t.contains("école") || t.contains("ecole") || t.contains("classe") || t.contains("ami"))
            return THEME_ECOLE;
        if (t.contains("nature") || t.contains("soleil") || t.contains("ciel") || t.contains("arbre"))
            return THEME_NATURE;
        if (t.contains("situation") || t.contains("enfant") || t.contains("groupe"))
            return THEME_SITUATION;
        return THEME_ANIMAUX;
    }

    private List<String> parseMultilineValues(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> vals = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String n = normalizeText(line);
            if (n != null && !n.isBlank()) vals.add(n);
        }
        return vals;
    }

    private boolean isValidText(String v, int min, int max) {
        return v != null && v.length() >= min && v.length() <= max && TEXT_PATTERN.matcher(v).matches();
    }

    private String normalizeText(String v) {
        return v == null ? null : v.trim().replaceAll("\\s+", " ");
>>>>>>> Stashed changes
    }

    private List<String> parseTags(String rawText) {
        List<String> values = parseMultilineValues(rawText);
        return values.stream()
                .map(value -> value.trim().toLowerCase().replace(' ', '_'))
                .collect(Collectors.toList());
    }

    private void clearMessages() {
<<<<<<< Updated upstream
        errorTitre.setText("");
        errorDescription.setText("");
        errorScene.setText("");
        errorImages.setText("");
        errorInterpretation.setText("");
        errorTags.setText("");
        errorMaxParticipants.setText("");
        errorGlobal.setText("");
=======
        errorTitre.setText(""); errorDescription.setText("");
        errorImages.setText(""); errorInterpretation.setText("");
        errorMaxParticipants.setText(""); errorGlobal.setText("");
>>>>>>> Stashed changes
        msgSuccess.setText("");
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