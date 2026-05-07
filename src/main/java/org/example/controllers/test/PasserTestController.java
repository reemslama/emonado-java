package org.example.controllers.test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Question;
import org.example.entities.Reponse;
import org.example.service.QuestionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PasserTestController {

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblProgression;
    @FXML
    private Label lblNumeroQ;
    @FXML
    private Label lblQuestion;
    @FXML
    private Label lblErreur;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private VBox vboxReponses;
    @FXML
    private Button btnSuivant;
    @FXML
    private Button btnRetour;
    @FXML
    private ImageView imageView;
    @FXML
    private HBox hboxImage;

    private final QuestionService questionService = new QuestionService();
    private final Map<Integer, Integer> reponsesSelectionnees = new HashMap<>();
    private final Map<Integer, String> imageMap = new HashMap<>();

    private List<Question> questions = new ArrayList<>();
    private ToggleGroup toggleGroup;
    private String categorie;
    private int indexCourant;

    public PasserTestController() {
        imageMap.put(10, "/images/depression/image1.jpg");
        imageMap.put(11, "/images/depression/image2.jpg");
        imageMap.put(12, "/images/depression/image3.jpg");
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
        this.questions = chargerQuestions(categorie);
        this.indexCourant = 0;

        lblTitre.setText("Test - " + formatCategorie(categorie));
        if (questions.isEmpty()) {
            lblProgression.setText("Aucune question disponible");
            lblNumeroQ.setText("QUESTION");
            lblQuestion.setText("Ce test n'a pas encore ete configure.");
            vboxReponses.getChildren().clear();
            btnSuivant.setDisable(true);
            btnRetour.setVisible(false);
            btnRetour.setManaged(false);
            progressBar.setProgress(0);
            return;
        }

        afficherQuestion();
    }

    @FXML
    private void suivant() {
        if (toggleGroup == null || toggleGroup.getSelectedToggle() == null) {
            lblErreur.setText("Veuillez choisir une reponse.");
            return;
        }

        Question question = questions.get(indexCourant);
        int valeur = (int) toggleGroup.getSelectedToggle().getUserData();
        reponsesSelectionnees.put(question.getId(), valeur);

        if (indexCourant < questions.size() - 1) {
            indexCourant++;
            afficherQuestion();
            return;
        }

        ouvrirResultat();
    }

    @FXML
    private void retour() {
        if (questions.isEmpty()) {
            changerScene("/fxml/test/ChoixCategorie.fxml", "Choix du test");
            return;
        }

        memoriserSelectionCourante();
        if (indexCourant > 0) {
            indexCourant--;
            afficherQuestion();
        } else {
            changerScene("/fxml/test/ChoixCategorie.fxml", "Choix du test");
        }
    }

    private void afficherQuestion() {
        Question question = questions.get(indexCourant);
        lblProgression.setText("Question " + (indexCourant + 1) + " / " + questions.size());
        lblNumeroQ.setText(String.format("QUESTION %02d", indexCourant + 1));
        lblQuestion.setText(question.getTexte());
        lblErreur.setText("");
        progressBar.setProgress((double) indexCourant / questions.size());

        afficherImage(question);
        afficherReponses(question);

        boolean premierEcran = indexCourant == 0;
        btnRetour.setVisible(!premierEcran);
        btnRetour.setManaged(!premierEcran);
        btnSuivant.setText(indexCourant == questions.size() - 1 ? "Terminer" : "Suivant ->");
    }

    private void afficherImage(Question question) {
        String type = question.getTypeQuestion();
        String imagePath = imageMap.get(question.getOrdre());

        if (type != null && type.equalsIgnoreCase("image") && imagePath != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream(imagePath));
                imageView.setImage(image);
                hboxImage.setVisible(true);
                hboxImage.setManaged(true);
                return;
            } catch (Exception ignored) {
            }
        }

        imageView.setImage(null);
        hboxImage.setVisible(false);
        hboxImage.setManaged(false);
    }

    private void afficherReponses(Question question) {
        vboxReponses.getChildren().clear();
        toggleGroup = new ToggleGroup();

        List<Reponse> reponses = new ArrayList<>(question.getReponses());
        reponses.sort(Comparator.comparing(r -> r.getOrdre() == null ? Integer.MAX_VALUE : r.getOrdre()));

        Integer selection = reponsesSelectionnees.get(question.getId());
        for (Reponse reponse : reponses) {
            RadioButton radio = new RadioButton(reponse.getTexte());
            radio.setToggleGroup(toggleGroup);
            radio.setUserData(reponse.getValeur());
            radio.setWrapText(true);
            radio.setMaxWidth(Double.MAX_VALUE);
            radio.setStyle(
                    "-fx-font-size: 14px; " +
                    "-fx-padding: 12 14 12 14; " +
                    "-fx-background-color: #f8fbf2; " +
                    "-fx-background-radius: 10; " +
                    "-fx-border-color: #d9e8bf; " +
                    "-fx-border-radius: 10;"
            );

            if (selection != null && selection == reponse.getValeur()) {
                radio.setSelected(true);
            }

            vboxReponses.getChildren().add(radio);
        }
    }

    private List<Question> chargerQuestions(String categorie) {
        List<Question> resultat = new ArrayList<>();
        String cible = normaliserCategorie(categorie);

        for (Question question : questionService.afficherTout()) {
            if (normaliserCategorie(question.getCategorie()).equals(cible)) {
                resultat.add(question);
            }
        }

        resultat.sort(Comparator.comparing(q -> q.getOrdre() == null ? Integer.MAX_VALUE : q.getOrdre()));
        return resultat;
    }

    private void memoriserSelectionCourante() {
        if (questions.isEmpty() || toggleGroup == null || toggleGroup.getSelectedToggle() == null) {
            return;
        }

        Question question = questions.get(indexCourant);
        int valeur = (int) toggleGroup.getSelectedToggle().getUserData();
        reponsesSelectionnees.put(question.getId(), valeur);
    }

    private void ouvrirResultat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test/Resultat.fxml"));
            Parent root = loader.load();

            ResultatController controller = loader.getController();
            int score = reponsesSelectionnees.values().stream().mapToInt(Integer::intValue).sum();
            controller.setResultat(score, calculerScoreMax(), categorie);

            changerScene(root, "Resultat du test");
        } catch (IOException e) {
            System.err.println("Erreur chargement resultat : " + e.getMessage());
        }
    }

    private int calculerScoreMax() {
        int total = 0;
        for (Question question : questions) {
            int max = 0;
            for (Reponse reponse : question.getReponses()) {
                max = Math.max(max, reponse.getValeur());
            }
            total += max;
        }
        return Math.max(total, questions.size() * 3);
    }

    private String formatCategorie(String categorie) {
        return switch (normaliserCategorie(categorie)) {
            case "stress" -> "Stress";
            case "depression" -> "Depression";
            case "anxiete" -> "Anxiete";
            case "iq" -> "IQ";
            default -> categorie == null ? "Test" : categorie;
        };
    }

    private String normaliserCategorie(String valeur) {
        if (valeur == null) {
            return "";
        }

        return valeur.toLowerCase(Locale.ROOT).trim()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ù", "u")
                .replace("ï", "i")
                .replace("î", "i")
                .replace("ç", "c");
    }

    private void changerScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            changerScene(root, title);
        } catch (IOException e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }

    private void changerScene(Parent root, String title) {
        Stage stage = (Stage) btnSuivant.getScene().getWindow();
        Scene currentScene = btnSuivant.getScene();
        Scene scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
    }
}
