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
import org.example.services.QuestionService;
import org.example.services.ReponseService;

import java.util.List;

public class QuestionsReponsesController {

    @FXML private TextField searchField;
    @FXML private TableView<Question> tableView;
    @FXML private TableColumn<Question, String> colId, colQuestion, colCategorie, colActions;

    @FXML private VBox modalBox;
    @FXML private Label modalTitle;
    @FXML private TextField fieldQuestion, fieldCategorie, fieldType, fieldOrdre;
    @FXML private TextArea fieldReponse;

    private final QuestionService questionService = new QuestionService();
    private final ReponseService reponseService = new ReponseService();
    private ObservableList<Question> data = FXCollections.observableArrayList();
    private Question selectedQuestion = null;

    @FXML
    public void initialize() {
        try {
            String css = getClass().getResource("/styles/admin.css").toExternalForm();
            tableView.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Impossible de charger admin.css : " + e.getMessage());
        }

        setupColumns();
        loadData();
    }

    private void setupColumns() {

        // ID
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId()))
        );

        // Question + Réponse
        colQuestion.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Question q = getTableRow().getItem();

                    Label lblQ = new Label(q.getTexte());
                    lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                    lblQ.setWrapText(true);
                    lblQ.setMaxWidth(500);

                    String rep = (q.getReponses() != null && !q.getReponses().isEmpty())
                            ? q.getReponses().get(0).getTexte()
                            : "Pas de réponse";

                    Label lblR = new Label(rep);
                    lblR.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                    lblR.setWrapText(true);
                    lblR.setMaxWidth(500);

                    VBox box = new VBox(5, lblQ, lblR);
                    box.setPadding(new Insets(8, 0, 8, 0));
                    box.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(box);
                }
            }
        });

        // Catégorie
        colCategorie.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategorie())
        );

        colCategorie.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");

                    if (item.toLowerCase().contains("anxiété")) {
                        badge.getStyleClass().add("badge-anxiete");
                    } else if (item.toLowerCase().contains("dépression")) {
                        badge.getStyleClass().add("badge-depression");
                    } else {
                        badge.getStyleClass().add("badge-general");
                    }

                    setGraphic(badge);
                }
            }
        });

        // Actions
        colActions.setCellFactory(param -> new TableCell<>() {

            private final Button bMod = new Button("Modifier");
            private final Button bSupp = new Button("Supprimer");
            private final HBox box = new HBox(10, bMod, bSupp);

            {
                bMod.getStyleClass().add("btn-action");
                bSupp.getStyleClass().add("btn-action");

                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(5));

                bMod.setOnAction(e ->
                        ouvrirModalModification(getTableView().getItems().get(getIndex()))
                );

                bSupp.setOnAction(e ->
                        confirmerSuppression(getTableView().getItems().get(getIndex()))
                );
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadData() {
        data.setAll(questionService.afficherTout());
        tableView.setItems(data);
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().trim();

        if (q.isEmpty()) {
            loadData();
        } else {
            data.setAll(questionService.rechercher(q));
        }
    }

    @FXML
    private void handleAjouter() {
        selectedQuestion = null;
        modalTitle.setText("Nouvelle Question");
        viderChamps();
        afficherModal(true);
    }

    private void ouvrirModalModification(Question q) {
        selectedQuestion = q;

        modalTitle.setText("Modifier la Question #" + q.getId());
        fieldQuestion.setText(q.getTexte());
        fieldCategorie.setText(q.getCategorie());
        fieldType.setText(q.getTypeQuestion());
        fieldOrdre.setText(q.getOrdre() != null ? String.valueOf(q.getOrdre()) : "");

        if (q.getReponses() != null && !q.getReponses().isEmpty()) {
            fieldReponse.setText(q.getReponses().get(0).getTexte());
        } else {
            fieldReponse.clear();
        }

        afficherModal(true);
    }

    @FXML
    private void handleSauvegarder() {
        try {
            String q = fieldQuestion.getText().trim();
            String r = fieldReponse.getText().trim();

            if (q.isEmpty() || r.isEmpty()) return;

            if (selectedQuestion == null) {

                Question nQ = new Question(
                        q,
                        Integer.parseInt(fieldOrdre.getText()),
                        fieldType.getText(),
                        fieldCategorie.getText()
                );

                questionService.ajouter(nQ);

                List<Question> list = questionService.rechercher(q);
                if (!list.isEmpty()) {
                    reponseService.ajouter(new Reponse(r, 0, 1, list.get(0)));
                }

            } else {

                selectedQuestion.setTexte(q);
                selectedQuestion.setCategorie(fieldCategorie.getText());

                questionService.modifier(selectedQuestion);
                // (optionnel) modifier réponse
            }

            afficherModal(false);
            loadData();

        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        afficherModal(false);
    }

    private void afficherModal(boolean v) {
        modalBox.setVisible(v);
        modalBox.setManaged(v);
    }

    private void viderChamps() {
        fieldQuestion.clear();
        fieldReponse.clear();
        fieldOrdre.clear();
        fieldType.clear();
        fieldCategorie.clear();
    }

    private void confirmerSuppression(Question q) {
        questionService.supprimer(q.getId());
        loadData();
    }
}