package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.example.entities.*;
import org.example.service.GrokAIService;
import org.example.service.QuestionService;
import org.example.utils.UserSession;

import java.util.*;

public class PasserTestController {

    // =========================================================================
    // FXML
    // =========================================================================

    @FXML private Label       lblTitre, lblProgression, lblQuestion, lblErreur;
    @FXML private ProgressBar progressBar;
    @FXML private VBox        vboxReponses;
    @FXML private Button      btnSuivant, btnRetour;
    @FXML private ImageView   aiIcon;

    // =========================================================================
    // CONSTANTES
    // =========================================================================

    /** Nombre maximal de questions en mode IA. */
    private static final int MAX_QUESTIONS_IA = 15;

    /**
     * Valeur minimale (incluse) pour qu'une réponse soit considérée "bonne".
     * Échelle 0-3 : 0 = très mal, 1 = mal, 2 = moyen/bien, 3 = très bien.
     * Doit être identique à GrokAIService.SEUIL_BONNE_REPONSE.
     */
    private static final int SEUIL_BONNE_REPONSE = 2;

    /** Nombre de premières questions déclenchant la terminaison anticipée. */
    private static final int NB_QUESTIONS_TERMINAISON = 3;

    /**
     * Clé utilisée dans les Maps de l'historique pour stocker la valeur choisie.
     * DOIT être identique à la clé lue dans GrokAIService.doitTerminerPrecocement().
     */
    private static final String CLE_VALEUR = "valeurReponse";

    // =========================================================================
    // STYLES
    // =========================================================================

    private static final String STYLE_NORMAL =
            "-fx-background-color: #ffffff;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: #e8e4e0;" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-padding: 16 20;" +
                    "-fx-text-fill: #1a1a1a;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: normal;" +
                    "-fx-cursor: hand;";

    private static final String STYLE_SELECTED =
            "-fx-background-color: #fff8f6;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: #e8745a;" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-padding: 16 20;" +
                    "-fx-text-fill: #1a1a1a;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;";

    // =========================================================================
    // ÉTAT
    // =========================================================================

    private List<Question>       questions          = new ArrayList<>();
    private int                  indexCourant       = 0;
    private Map<Integer, Integer> reponsesClassiques = new HashMap<>();
    private String               categorie;
    private ToggleGroup          toggleGroup;

    private boolean      modeIA       = false;
    private TestAdaptatif testAdaptatif;

    // =========================================================================
    // SERVICES
    // =========================================================================

    private final QuestionService questionService = new QuestionService();
    private final GrokAIService   grokAIService   = new GrokAIService();

    // =========================================================================
    // INITIALISATION — MODE CLASSIQUE
    // =========================================================================

    public void setCategorie(String categorie) {
        this.categorie  = categorie;
        this.modeIA     = false;
        lblTitre.setText("Test - " + categorie.toUpperCase());
        this.questions  = questionService.getQuestionsByCategorie(categorie);
        indexCourant    = 0;
        reponsesClassiques.clear();
        afficherQuestion();
    }

    // =========================================================================
    // ACTIVATION MODE IA
    // =========================================================================

    @FXML
    public void activerTestAdaptatifIA() {
        if (!grokAIService.isConfigured()) {
            lblErreur.setText("⚠️ Clé API manquante !");
            return;
        }

        modeIA = true;
        questions.clear();
        indexCourant = 0;
        reponsesClassiques.clear();

        if (aiIcon != null) aiIcon.setVisible(false);

        testAdaptatif = new TestAdaptatif();
        testAdaptatif.setCategorie(categorie);
        testAdaptatif.setPatient(UserSession.getInstance());

        lblTitre.setText("Test IA - " + categorie.toUpperCase());
        chargerProchaineQuestionIA();
    }

    // =========================================================================
    // CHARGEMENT ASYNCHRONE D'UNE QUESTION IA
    // =========================================================================

    private void chargerProchaineQuestionIA() {
        lblErreur.setText("✨ Génération IA en cours…");
        btnSuivant.setDisable(true);
        vboxReponses.setDisable(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        final List<Map<String, Object>> historiqueSnapshot =
                new ArrayList<>(testAdaptatif.getQuestionsReponses());

        new Thread(() -> {
            try {
                // getProchaineQuestion retourne null si terminaison anticipée
                Question nextQ = grokAIService.getProchaineQuestion(
                        categorie,
                        historiqueSnapshot,
                        UserSession.getInstance()
                );

                Platform.runLater(() -> {
                    btnSuivant.setDisable(false);
                    vboxReponses.setDisable(false);
                    lblErreur.setText("");

                    if (nextQ != null
                            && nextQ.getTexte() != null
                            && !nextQ.getTexte().isBlank()) {
                        questions.add(nextQ);
                        indexCourant = questions.size() - 1;
                        afficherQuestion();
                    } else {
                        // null = terminaison anticipée demandée par le service
                        allerAuResultat();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblErreur.setText("⚠️ Erreur IA → bascule en mode classique");
                    modeIA    = false;
                    questions = questionService.getQuestionsByCategorie(categorie);
                    indexCourant = 0;
                    afficherQuestion();
                });
            }
        }).start();
    }

    // =========================================================================
    // AFFICHAGE DE LA QUESTION COURANTE
    // =========================================================================

    private void afficherQuestion() {
        if (questions.isEmpty() || indexCourant < 0 || indexCourant >= questions.size()) return;

        Question q = questions.get(indexCourant);

        if (modeIA) {
            int repondues = testAdaptatif.getQuestionsReponses().size();
            lblProgression.setText("IA – Question " + (repondues + 1) + " / " + MAX_QUESTIONS_IA);
            progressBar.setProgress((double)(repondues + 1) / MAX_QUESTIONS_IA);
        } else {
            lblProgression.setText("Question " + (indexCourant + 1) + " / " + questions.size());
            progressBar.setProgress((double)(indexCourant + 1) / questions.size());
        }

        lblQuestion.setText(q.getTexte());
        lblErreur.setText("");

        vboxReponses.getChildren().clear();
        toggleGroup = new ToggleGroup();

        for (Reponse r : q.getReponses()) {
            RadioButton rb = new RadioButton(r.getTexte());
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(r.getValeur());
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setStyle(STYLE_NORMAL);
            rb.setGraphic(null);

            rb.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    vboxReponses.getChildren().forEach(n -> n.setStyle(STYLE_NORMAL));
                    rb.setStyle(STYLE_SELECTED);
                }
            });

            vboxReponses.getChildren().add(rb);
        }

        btnRetour.setVisible(!modeIA && indexCourant > 0);

        if (modeIA) {
            int repondues = testAdaptatif.getQuestionsReponses().size();
            btnSuivant.setText(repondues >= MAX_QUESTIONS_IA - 1 ? "Terminer" : "Continuer  ›");
        } else {
            btnSuivant.setText(indexCourant == questions.size() - 1 ? "Terminer" : "Continuer  ›");
        }
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    @FXML
    private void retour() {
        if (modeIA || indexCourant <= 0) return;
        indexCourant--;
        afficherQuestion();
    }

    @FXML
    private void suivant() {
        if (toggleGroup == null || toggleGroup.getSelectedToggle() == null) {
            lblErreur.setText("⚠️ Veuillez choisir une réponse avant de continuer.");
            return;
        }

        RadioButton rb    = (RadioButton) toggleGroup.getSelectedToggle();
        int         valeur = (int) rb.getUserData();
        Question    q      = questions.get(indexCourant);

        if (modeIA) {
            // ── Enregistrement de la réponse dans l'historique ────────────────
            // La Map doit utiliser CLE_VALEUR pour que GrokAIService puisse la lire
            testAdaptatif.ajouterQuestionReponse(q.getTexte(), rb.getText(), valeur);

            int repondues = testAdaptatif.getQuestionsReponses().size();

            // ── Vérification de terminaison anticipée après 3 questions ───────
            if (repondues == NB_QUESTIONS_TERMINAISON && patientVaBien()) {
                afficherMessageTerminaisonPrecoce();
                return;
            }

            // ── Fin normale du questionnaire ──────────────────────────────────
            if (repondues >= MAX_QUESTIONS_IA) {
                allerAuResultat();
            } else {
                chargerProchaineQuestionIA();
            }

        } else {
            // ── Mode classique ────────────────────────────────────────────────
            reponsesClassiques.put(q.getId(), valeur);

            if (indexCourant < questions.size() - 1) {
                indexCourant++;
                afficherQuestion();
            } else {
                allerAuResultat();
            }
        }
    }

    // =========================================================================
    // TERMINAISON ANTICIPÉE
    // =========================================================================

    /**
     * Vérifie si les {@value #NB_QUESTIONS_TERMINAISON} premières réponses
     * indiquent que le patient va bien (toutes les valeurs >= {@value #SEUIL_BONNE_REPONSE}).
     *
     * <p>La clé lue dans l'historique est {@value #CLE_VALEUR}, identique à
     * celle écrite par {@link TestAdaptatif#ajouterQuestionReponse}.
     *
     * @return true si toutes les premières réponses sont "bonnes"
     */
    private boolean patientVaBien() {
        List<Map<String, Object>> historique = testAdaptatif.getQuestionsReponses();
        if (historique == null || historique.size() < NB_QUESTIONS_TERMINAISON) return false;

        long bonnesReponses = historique.subList(0, NB_QUESTIONS_TERMINAISON)
                .stream()
                .filter(h -> {
                    Object val = h.get(CLE_VALEUR);
                    if (val instanceof Number) {
                        return ((Number) val).intValue() >= SEUIL_BONNE_REPONSE;
                    }
                    return false;
                })
                .count();

        boolean vaBien = bonnesReponses == NB_QUESTIONS_TERMINAISON;

        System.out.println("[PasserTestController] Terminaison anticipée : "
                + bonnesReponses + "/" + NB_QUESTIONS_TERMINAISON
                + " bonnes réponses (seuil >= " + SEUIL_BONNE_REPONSE + ")"
                + " → " + (vaBien ? "OUI" : "NON"));

        return vaBien;
    }

    /**
     * Affiche un message de bonne santé pendant 1,5 s puis redirige
     * vers l'écran de résultat.
     */
    private void afficherMessageTerminaisonPrecoce() {
        lblErreur.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        lblErreur.setText("✅ Vos 3 premières réponses indiquent que vous allez bien !"
                + " Le questionnaire se termine ici.");
        btnSuivant.setDisable(true);
        vboxReponses.setDisable(true);

        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            Platform.runLater(this::allerAuResultat);
        }).start();
    }

    // =========================================================================
    // AFFICHAGE DU RÉSULTAT
    // =========================================================================

    private void allerAuResultat() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/test/Resultat.fxml"));
            Parent root = loader.load();
            ResultatController controller = loader.getController();

            if (modeIA) {
                testAdaptatif.setTermine(true);
                int score = testAdaptatif.getScoreTotal();
                int max   = testAdaptatif.getNombreQuestions() * 3;
                controller.setResultatAvecTestAdaptatif(score, max, categorie, testAdaptatif);
            } else {
                int score = reponsesClassiques.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();
                controller.setResultat(score, categorie, questions.size());
            }

            Stage stage = (Stage) btnSuivant.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");

        } catch (Exception e) {
            e.printStackTrace();
            lblErreur.setText("Erreur affichage résultat");
        }
    }
}