package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.entities.Question;
import org.example.entities.Reponse;
import org.example.service.QuestionService;
import org.example.service.ReponseService;

public class QuestionsReponsesController {

    // ── Questions ─────────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private TableView<Question> tableView;
    @FXML private TableColumn<Question, String> colId, colQuestion, colCategorie, colActions;
    @FXML private VBox modalBox;
    @FXML private Label modalTitle;
    @FXML private TextField fieldQuestion, fieldCategorie, fieldType, fieldOrdre;

    // ── Réponses ──────────────────────────────────────────────────────────────
    @FXML private Label labelReponses;
    @FXML private Button btnAjouterReponse;
    @FXML private TableView<Reponse> tableReponses;
    @FXML private TableColumn<Reponse, String> colRepId, colRepTexte, colRepValeur, colRepOrdre, colRepActions;
    @FXML private VBox modalReponseBox;
    @FXML private Label modalReponseTitle;
    @FXML private TextArea fieldRepTexte;
    @FXML private TextField fieldRepValeur, fieldRepOrdre;

    private final QuestionService questionService = new QuestionService();
    private final ReponseService reponseService   = new ReponseService();

    private ObservableList<Question> dataQuestions = FXCollections.observableArrayList();
    private ObservableList<Reponse>  dataReponses  = FXCollections.observableArrayList();

    private Question selectedQuestion = null;
    private Question questionEnCours  = null;
    private Reponse  selectedReponse  = null;

    // ── Constantes validation ─────────────────────────────────────────────────
    private static final int QUESTION_MIN  = 10;
    private static final int QUESTION_MAX  = 300;
    // BUG CORRIGÉ #2 : CATEGORIE_MIN abaissé à 2 pour accepter "iq", "qa", etc.
    private static final int CATEGORIE_MIN = 2;
    private static final int CATEGORIE_MAX = 50;
    private static final int TYPE_MAX      = 50;
    private static final int REPONSE_MIN   = 2;
    private static final int REPONSE_MAX   = 200;
    // BUG CORRIGÉ #1 : VALEUR_MIN reste 0, mais la validation ne bloquera plus sur 0
    private static final int VALEUR_MIN    = 0;
    private static final int VALEUR_MAX    = 100;
    private static final int ORDRE_MIN     = 1;
    private static final int ORDRE_MAX     = 999;

    // ── Initialize ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            String css = getClass().getResource("/styles/admin.css").toExternalForm();
            tableView.getStylesheets().add(css);
            tableReponses.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("CSS introuvable : " + e.getMessage());
        }

        setupColumnsQuestions();
        setupColumnsReponses();
        loadQuestions();
        setupValidationListeners();

        tableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        questionEnCours = newVal;
                        labelReponses.setText("Réponses de : " + newVal.getTexte());
                        btnAjouterReponse.setDisable(false);
                        loadReponses(newVal.getId());
                    }
                }
        );
    }

    // ── Listeners validation en temps réel ────────────────────────────────────
    private void setupValidationListeners() {
        fieldQuestion.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldQuestion,
                !n.trim().isEmpty()
                        && n.trim().length() >= QUESTION_MIN
                        && n.trim().length() <= QUESTION_MAX
        ));

        fieldCategorie.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldCategorie,
                // BUG CORRIGÉ #2 : CATEGORIE_MIN = 2 donc "iq" (2 chars) passe
                !n.trim().isEmpty()
                        && n.trim().length() >= CATEGORIE_MIN
                        && n.trim().length() <= CATEGORIE_MAX
                        && n.matches("[a-zA-ZÀ-ÿ\\s\\-]+")
        ));

        fieldType.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldType,
                n.trim().isEmpty() || n.trim().length() <= TYPE_MAX
        ));

        fieldOrdre.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldOrdre,
                n.trim().isEmpty() || estEntierDansIntervalle(n, ORDRE_MIN, ORDRE_MAX)
        ));

        fieldRepTexte.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldRepTexte,
                !n.trim().isEmpty()
                        && n.trim().length() >= REPONSE_MIN
                        && n.trim().length() <= REPONSE_MAX
        ));

        // BUG CORRIGÉ #1 : on accepte "0" explicitement — isEmpty OU entier valide
        fieldRepValeur.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldRepValeur,
                n.trim().isEmpty() || estEntierDansIntervalle(n, VALEUR_MIN, VALEUR_MAX)
        ));

        fieldRepOrdre.textProperty().addListener((obs, o, n) -> validerChamp(
                fieldRepOrdre,
                n.trim().isEmpty() || estEntierDansIntervalle(n, ORDRE_MIN, ORDRE_MAX)
        ));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //                          CRUD QUESTIONS
    // ═════════════════════════════════════════════════════════════════════════

    private void setupColumnsQuestions() {
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId()))
        );

        colQuestion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTexte())
        );
        colQuestion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null
                        || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null);
                } else {
                    Question q = getTableRow().getItem();
                    Label lblQ = new Label(q.getTexte());
                    lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
                    lblQ.setWrapText(true);
                    lblQ.setMaxWidth(260);
                    Label lblType = new Label(q.getTypeQuestion() != null ? q.getTypeQuestion() : "");
                    lblType.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    VBox box = new VBox(3, lblQ, lblType);
                    box.setPadding(new Insets(5, 0, 5, 0));
                    setGraphic(box);
                }
            }
        });

        colCategorie.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategorie())
        );
        colCategorie.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("badge");
                if (item.toLowerCase().contains("anxiété"))
                    badge.getStyleClass().add("badge-anxiete");
                else if (item.toLowerCase().contains("dépression"))
                    badge.getStyleClass().add("badge-depression");
                else
                    badge.getStyleClass().add("badge-general");
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button bMod  = new Button("Modifier");
            private final Button bSupp = new Button("Supprimer");
            private final HBox box     = new HBox(8, bMod, bSupp);
            {
                bMod.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;");
                bSupp.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;");
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(0, 0, 0, 5));
                bMod.setOnAction(e -> ouvrirModalQuestion(getTableView().getItems().get(getIndex())));
                bSupp.setOnAction(e -> confirmerSuppressionQuestion(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadQuestions() {
        dataQuestions.setAll(questionService.afficherTout());
        tableView.setItems(dataQuestions);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        dataQuestions.setAll(
                keyword.isEmpty() ? questionService.afficherTout()
                        : questionService.rechercher(keyword)
        );
        tableView.setItems(dataQuestions);
    }

    @FXML
    private void handleAjouter() {
        selectedQuestion = null;
        modalTitle.setText("Nouvelle Question");
        viderChampsQuestion();
        resetStyles();
        afficherModal(modalBox, true);
    }

    private void ouvrirModalQuestion(Question q) {
        selectedQuestion = q;
        modalTitle.setText("Modifier Question #" + q.getId());
        fieldQuestion.setText(q.getTexte());
        fieldCategorie.setText(q.getCategorie() != null ? q.getCategorie() : "");
        fieldType.setText(q.getTypeQuestion() != null ? q.getTypeQuestion() : "");
        fieldOrdre.setText(q.getOrdre() != null ? String.valueOf(q.getOrdre()) : "");
        resetStyles();
        afficherModal(modalBox, true);
    }

    @FXML
    private void handleSauvegarder() {
        String texte     = fieldQuestion.getText().trim();
        String categorie = fieldCategorie.getText().trim();
        String type      = fieldType.getText().trim();
        String ordreStr  = fieldOrdre.getText().trim();

        boolean valid = true;
        StringBuilder erreurs = new StringBuilder();

        // ── Validation question ──
        if (texte.isEmpty()) {
            marquerErreur(fieldQuestion);
            erreurs.append("• La question est obligatoire.\n");
            valid = false;
        } else if (texte.length() < QUESTION_MIN) {
            marquerErreur(fieldQuestion);
            erreurs.append("• Question : minimum " + QUESTION_MIN + " caractères.\n");
            valid = false;
        } else if (texte.length() > QUESTION_MAX) {
            marquerErreur(fieldQuestion);
            erreurs.append("• Question : maximum " + QUESTION_MAX + " caractères.\n");
            valid = false;
        } else {
            marquerSucces(fieldQuestion);
        }

        // ── Validation catégorie ──
        // BUG CORRIGÉ #2 : CATEGORIE_MIN = 2, "iq" est maintenant accepté
        if (categorie.isEmpty()) {
            marquerErreur(fieldCategorie);
            erreurs.append("• La catégorie est obligatoire.\n");
            valid = false;
        } else if (categorie.length() < CATEGORIE_MIN) {
            marquerErreur(fieldCategorie);
            erreurs.append("• Catégorie : minimum " + CATEGORIE_MIN + " caractères.\n");
            valid = false;
        } else if (categorie.length() > CATEGORIE_MAX) {
            marquerErreur(fieldCategorie);
            erreurs.append("• Catégorie : maximum " + CATEGORIE_MAX + " caractères.\n");
            valid = false;
        } else if (!categorie.matches("[a-zA-ZÀ-ÿ\\s\\-]+")) {
            marquerErreur(fieldCategorie);
            erreurs.append("• Catégorie : lettres et espaces uniquement.\n");
            valid = false;
        } else {
            marquerSucces(fieldCategorie);
        }

        // ── Validation type (optionnel) ──
        if (!type.isEmpty() && type.length() > TYPE_MAX) {
            marquerErreur(fieldType);
            erreurs.append("• Type : maximum " + TYPE_MAX + " caractères.\n");
            valid = false;
        } else {
            marquerSucces(fieldType);
        }

        // ── Validation ordre (optionnel) ──
        Integer ordre = null;
        if (!ordreStr.isEmpty()) {
            if (!estEntierDansIntervalle(ordreStr, ORDRE_MIN, ORDRE_MAX)) {
                marquerErreur(fieldOrdre);
                erreurs.append("• Ordre : entier entre " + ORDRE_MIN + " et " + ORDRE_MAX + ".\n");
                valid = false;
            } else {
                ordre = Integer.parseInt(ordreStr);
                marquerSucces(fieldOrdre);
            }
        }

        if (!valid) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de saisie", erreurs.toString());
            return;
        }

        if (selectedQuestion == null) {
            questionService.ajouter(new Question(texte, ordre, type, categorie));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Question ajoutée !");
        } else {
            selectedQuestion.setTexte(texte);
            selectedQuestion.setCategorie(categorie);
            selectedQuestion.setTypeQuestion(type);
            selectedQuestion.setOrdre(ordre);
            questionService.modifier(selectedQuestion);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Question modifiée !");
        }

        afficherModal(modalBox, false);
        loadQuestions();
    }

    private void confirmerSuppressionQuestion(Question q) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer cette question ?");
        alert.setContentText("« " + q.getTexte() + " »\nToutes ses réponses seront supprimées.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                questionService.supprimer(q.getId());
                dataReponses.clear();
                labelReponses.setText("Réponses — sélectionnez une question");
                btnAjouterReponse.setDisable(true);
                questionEnCours = null;
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Question supprimée.");
                loadQuestions();
            }
        });
    }

    @FXML
    private void handleAnnuler() {
        afficherModal(modalBox, false);
        viderChampsQuestion();
        resetStyles();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //                          CRUD RÉPONSES
    // ═════════════════════════════════════════════════════════════════════════

    private void setupColumnsReponses() {
        colRepId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId()))
        );

        colRepTexte.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTexte())
        );
        colRepTexte.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");
                lbl.setWrapText(true);
                lbl.setMaxWidth(290);
                VBox box = new VBox(lbl);
                box.setPadding(new Insets(5, 0, 5, 0));
                setGraphic(box);
            }
        });

        colRepValeur.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getValeur()))
        );

        colRepOrdre.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getOrdre() != null ? String.valueOf(c.getValue().getOrdre()) : "-"
                )
        );

        colRepActions.setCellFactory(col -> new TableCell<>() {
            private final Button bMod  = new Button("Modifier");
            private final Button bSupp = new Button("Supprimer");
            private final HBox box     = new HBox(8, bMod, bSupp);
            {
                bMod.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;");
                bSupp.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;");
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(0, 0, 0, 5));
                bMod.setOnAction(e -> ouvrirModalReponse(getTableView().getItems().get(getIndex())));
                bSupp.setOnAction(e -> confirmerSuppressionReponse(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadReponses(int questionId) {
        dataReponses.setAll(reponseService.getReponsesByQuestion(questionId));
        tableReponses.setItems(dataReponses);
    }

    @FXML
    private void handleAjouterReponse() {
        selectedReponse = null;
        modalReponseTitle.setText("Nouvelle Réponse");
        viderChampsReponse();
        resetStylesReponse();
        afficherModal(modalReponseBox, true);
    }

    private void ouvrirModalReponse(Reponse r) {
        selectedReponse = r;
        modalReponseTitle.setText("Modifier Réponse #" + r.getId());
        fieldRepTexte.setText(r.getTexte());
        fieldRepValeur.setText(String.valueOf(r.getValeur()));
        fieldRepOrdre.setText(r.getOrdre() != null ? String.valueOf(r.getOrdre()) : "");
        resetStylesReponse();
        afficherModal(modalReponseBox, true);
    }

    @FXML
    private void handleSauvegarderReponse() {
        String texte     = fieldRepTexte.getText().trim();
        String valeurStr = fieldRepValeur.getText().trim();
        String ordreStr  = fieldRepOrdre.getText().trim();

        boolean valid = true;
        StringBuilder erreurs = new StringBuilder();

        // ── Validation texte réponse ──
        if (texte.isEmpty()) {
            marquerErreur(fieldRepTexte);
            erreurs.append("• Le texte de la réponse est obligatoire.\n");
            valid = false;
        } else if (texte.length() < REPONSE_MIN) {
            marquerErreur(fieldRepTexte);
            erreurs.append("• Réponse : minimum " + REPONSE_MIN + " caractères.\n");
            valid = false;
        } else if (texte.length() > REPONSE_MAX) {
            marquerErreur(fieldRepTexte);
            erreurs.append("• Réponse : maximum " + REPONSE_MAX + " caractères.\n");
            valid = false;
        } else {
            marquerSucces(fieldRepTexte);
        }

        // ── Validation valeur (optionnel, 0 est une valeur valide) ──
        // BUG CORRIGÉ #1 : valeurStr vide → valeur = 0 par défaut, pas d'erreur
        int valeur = 0;
        if (!valeurStr.isEmpty()) {
            if (!estEntierDansIntervalle(valeurStr, VALEUR_MIN, VALEUR_MAX)) {
                marquerErreur(fieldRepValeur);
                erreurs.append("• Valeur : entier entre " + VALEUR_MIN + " et " + VALEUR_MAX + ".\n");
                valid = false;
            } else {
                valeur = Integer.parseInt(valeurStr);
                marquerSucces(fieldRepValeur);
            }
        } else {
            // Champ vide = 0 accepté silencieusement
            marquerSucces(fieldRepValeur);
        }

        // ── Validation ordre (optionnel) ──
        Integer ordre = null;
        if (!ordreStr.isEmpty()) {
            if (!estEntierDansIntervalle(ordreStr, ORDRE_MIN, ORDRE_MAX)) {
                marquerErreur(fieldRepOrdre);
                erreurs.append("• Ordre : entier entre " + ORDRE_MIN + " et " + ORDRE_MAX + ".\n");
                valid = false;
            } else {
                ordre = Integer.parseInt(ordreStr);
                marquerSucces(fieldRepOrdre);
            }
        }

        if (!valid) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de saisie", erreurs.toString());
            return;
        }

        if (selectedReponse == null) {
            reponseService.ajouter(new Reponse(texte, valeur, ordre, questionEnCours));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse ajoutée !");
        } else {
            selectedReponse.setTexte(texte);
            selectedReponse.setValeur(valeur);
            selectedReponse.setOrdre(ordre);
            reponseService.modifier(selectedReponse);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse modifiée !");
        }

        afficherModal(modalReponseBox, false);
        loadReponses(questionEnCours.getId());
    }

    private void confirmerSuppressionReponse(Reponse r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer cette réponse ?");
        alert.setContentText("« " + r.getTexte() + " »");
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                reponseService.supprimer(r.getId());
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse supprimée.");
                loadReponses(questionEnCours.getId());
            }
        });
    }

    @FXML
    private void handleAnnulerReponse() {
        afficherModal(modalReponseBox, false);
        viderChampsReponse();
        resetStylesReponse();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //                        HELPERS VALIDATION
    // ═════════════════════════════════════════════════════════════════════════

    // Validation silencieuse temps réel — bordure seulement, sans popup
    private void validerChamp(Control champ, boolean valide) {
        if (valide) marquerSucces(champ);
        else        marquerErreur(champ);
    }

    // Bordure rouge sans popup — la popup n'apparaît qu'à la sauvegarde
    private void marquerErreur(Control champ) {
        champ.setStyle(
                "-fx-border-color: #ef4444; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 5; -fx-background-radius: 5;"
        );
    }

    private void marquerSucces(Control champ) {
        champ.setStyle(
                "-fx-border-color: #22c55e; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 5; -fx-background-radius: 5;"
        );
    }

    private void resetStyles() {
        String style = "-fx-border-color: #d1d5db; -fx-border-width: 1;" +
                "-fx-border-radius: 5; -fx-background-radius: 5;";
        fieldQuestion.setStyle(style);
        fieldCategorie.setStyle(style);
        fieldType.setStyle(style);
        fieldOrdre.setStyle(style);
    }

    private void resetStylesReponse() {
        String style = "-fx-border-color: #d1d5db; -fx-border-width: 1;" +
                "-fx-border-radius: 5; -fx-background-radius: 5;";
        fieldRepTexte.setStyle(style);
        fieldRepValeur.setStyle(style);
        fieldRepOrdre.setStyle(style);
    }

    private boolean estEntierDansIntervalle(String valeur, int min, int max) {
        try {
            int v = Integer.parseInt(valeur.trim());
            return v >= min && v <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ── Helpers généraux ──────────────────────────────────────────────────────

    private void afficherModal(VBox modal, boolean v) {
        modal.setVisible(v);
        modal.setManaged(v);
    }

    private void viderChampsQuestion() {
        fieldQuestion.clear();
        fieldCategorie.clear();
        fieldType.clear();
        fieldOrdre.clear();
    }

    private void viderChampsReponse() {
        fieldRepTexte.clear();
        fieldRepValeur.clear();
        fieldRepOrdre.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}