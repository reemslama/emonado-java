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
import org.example.services.QuestionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PasserTestController {

    @FXML private Label lblTitre;
    @FXML private Label lblProgression;
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
    private final Map<Integer, Integer> reponses = new HashMap<>();
    private String categorie;
    private ToggleGroup toggleGroup;

    private final QuestionService questionService = new QuestionService();

    private final Map<Integer, String> imageMap = new HashMap<>() {{
        put(10, "/images/depression/image1.jpg");
        put(11, "/images/depression/image2.jpg");
        put(12, "/images/depression/image3.jpg");
    }};

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
        progressBar.setProgress((double) indexCourant / questions.size());
        lblQuestion.setText((indexCourant + 1) + ". " + q.getTexte());
        lblErreur.setText("");

        if ("image".equals(q.getTypeQuestion())) {
            String imagePath = imageMap.get(q.getOrdre());
            if (imagePath != null) {
                try {
                    Image image = new Image(getClass().getResourceAsStream(imagePath));
                    imageView.setImage(image);
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
            rb.setStyle("-fx-font-size: 15px; -fx-padding: 6;");

            if (reponses.containsKey(q.getId()) && reponses.get(q.getId()) == r.getValeur()) {
                rb.setSelected(true);
            }

            vboxReponses.getChildren().add(rb);
        }

        btnRetour.setVisible(indexCourant > 0);

        if (indexCourant == questions.size() - 1) {
            btnSuivant.setText("Terminer");
        } else {
            btnSuivant.setText("Suivant");
        }
    }

    @FXML
    private void suivant() {
        if (toggleGroup.getSelectedToggle() == null) {
            lblErreur.setText("Veuillez choisir une reponse.");
            return;
        }

        Question q = questions.get(indexCourant);
        int valeur = (int) toggleGroup.getSelectedToggle().getUserData();
        reponses.put(q.getId(), valeur);

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
                int valeur = (int) toggleGroup.getSelectedToggle().getUserData();
                reponses.put(q.getId(), valeur);
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
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Resultat");
        } catch (Exception e) {
            System.out.println("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    private void retourDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnSuivant.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Espace Patient");
        } catch (Exception e) {
            System.out.println("Erreur retour : " + e.getMessage());
        }
    }
}
