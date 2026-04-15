package org.example.controllers.test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Question;
import org.example.entities.Reponse;
import org.example.service.QuestionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PasserTestController {

    @FXML private Label lblTitre;
    @FXML private Label lblProgression;
    @FXML private Label lblNumeroQ;
    @FXML private Label lblQuestion;
    @FXML private Label lblErreur;
    @FXML private ProgressBar progressBar;
    @FXML private VBox vboxReponses;
    @FXML private Button btnSuivant;
    @FXML private Button btnRetour;
    @FXML private ImageView imageView;
    @FXML private HBox hboxImage;

    private List<Question> questions;
    private int indexCourant = 0;
    private Map<Integer, Integer> reponses = new HashMap<>();
    private String categorie;
    private ToggleGroup toggleGroup;

    private final QuestionService questionService = new QuestionService();

    private final Map<Integer, String> imageMap = new HashMap<>() {{
        put(10, "/images/depression/image1.jpg");
        put(11, "/images/depression/image2.jpg");
        put(12, "/images/depression/image3.jpg");
    }};

    private static final String STYLE_NORMAL =
            "-fx-background-color: #f5faea;" +
                    "-fx-background-radius: 9;" +
                    "-fx-border-color: #c2dc98;" +
                    "-fx-border-radius: 9;" +
                    "-fx-border-width: 1.2;" +
                    "-fx-padding: 11 15 11 15;" +
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: #253320;" +
                    "-fx-cursor: hand;";

    private static final String STYLE_SELECTED =
            "-fx-background-color: #33502a;" +
                    "-fx-background-radius: 9;" +
                    "-fx-border-color: #33502a;" +
                    "-fx-border-radius: 9;" +
                    "-fx-border-width: 1.2;" +
                    "-fx-padding: 11 15 11 15;" +
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;";

    public void setCategorie(String categorie) {
        this.categorie = categorie;
        lblTitre.setText("Test - " + categorie.toUpperCase());
        questions = questionService.getQuestionsByCategorie(categorie);
        afficherQuestion();
    }

    private void afficherQuestion() {
        if (questions.isEmpty()) return;

        Question q = questions.get(indexCourant);

        lblProgression.setText("Question " + (indexCourant + 1) + " / " + questions.size());
        progressBar.setProgress((double) (indexCourant + 1) / questions.size());
        lblQuestion.setText((indexCourant + 1) + ". " + q.getTexte());
        lblErreur.setText("");

        if (lblNumeroQ != null)
            lblNumeroQ.setText("QUESTION " + String.format("%02d", indexCourant + 1));

        if ("image".equals(q.getTypeQuestion())) {
            String imagePath = imageMap.get(q.getOrdre());
            if (imagePath != null) {
                try {
                    imageView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
                    hboxImage.setVisible(true);
                    hboxImage.setManaged(true);
                } catch (Exception e) {
                    hboxImage.setVisible(false);
                    hboxImage.setManaged(false);
                }
            }
        } else {
            hboxImage.setVisible(false);
            hboxImage.setManaged(false);
            imageView.setImage(null);
        }

        vboxReponses.getChildren().clear();
        toggleGroup = new ToggleGroup();

        for (Reponse r : q.getReponses()) {
            RadioButton rb = new RadioButton(r.getTexte());
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(r.getValeur());
            rb.setMaxWidth(Double.MAX_VALUE);

            boolean isSelected = reponses.containsKey(q.getId())
                    && reponses.get(q.getId()) == r.getValeur();
            rb.setStyle(isSelected ? STYLE_SELECTED : STYLE_NORMAL);
            if (isSelected) rb.setSelected(true);

            rb.selectedProperty().addListener((obs, was, now) -> {
                if (now) {
                    vboxReponses.getChildren().forEach(node -> {
                        if (node instanceof RadioButton)
                            ((RadioButton) node).setStyle(STYLE_NORMAL);
                    });
                    rb.setStyle(STYLE_SELECTED);
                }
            });

            vboxReponses.getChildren().add(rb);
        }

        // Retour : gérer visible ET managed ensemble
        boolean showRetour = indexCourant > 0;
        btnRetour.setVisible(showRetour);
        btnRetour.setManaged(showRetour);

        btnSuivant.setText(indexCourant == questions.size() - 1 ? "Terminer ✓" : "Suivant →");
    }

    @FXML
    private void suivant() {
        if (toggleGroup.getSelectedToggle() == null) {
            lblErreur.setText("⚠  Veuillez choisir une réponse avant de continuer !");
            return;
        }
        Question q = questions.get(indexCourant);
        reponses.put(q.getId(), (int) toggleGroup.getSelectedToggle().getUserData());

        if (indexCourant < questions.size() - 1) {
            indexCourant++;
            afficherQuestion();
        } else {
            allerAuResultat();
        }
    }

    @FXML
    private void retour() {
        if (indexCourant > 0) {
            if (toggleGroup.getSelectedToggle() != null) {
                Question q = questions.get(indexCourant);
                reponses.put(q.getId(), (int) toggleGroup.getSelectedToggle().getUserData());
            }
            indexCourant--;
            afficherQuestion();
        }
    }

    private void allerAuResultat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/Resultat.fxml"));
            Parent root = loader.load();

            ResultatController controller = loader.getController();
            int score = reponses.values().stream().mapToInt(Integer::intValue).sum();
            controller.setResultat(score, categorie, questions.size());

            Stage stage = (Stage) btnSuivant.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("Résultat");
            stage.setMaximized(true);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");

        } catch (Exception e) {
            System.out.println("Erreur navigation : " + e.getMessage());
        }
    }
}