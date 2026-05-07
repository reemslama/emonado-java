package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import org.example.entities.ConsultationPayment;
import org.example.entities.ConsultationQuestionnaire;
import org.example.entities.User;
import org.example.service.ConsultationWorkflowService;
import org.example.service.MedicalValidationService;
import org.example.utils.UserSession;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class PaymentWorkflowController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{4}\\s\\d{4}\\s\\d{4}\\s\\d{4}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3,4}$");

    @FXML private Label summaryTitleLabel;
    @FXML private Label summarySubtitleLabel;
    @FXML private Label paymentSummaryLabel;
    @FXML private Label questionnaireSummaryLabel;
    @FXML private ComboBox<String> paymentStatusFilterCombo;
    @FXML private ComboBox<String> sortDateCombo;
    @FXML private VBox paymentsBox;
    @FXML private Button payNowButton;
    @FXML private Button syncPaymentButton;
    @FXML private Button localPayButton;
    @FXML private VBox simulatedCardBox;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private Label simulatedCardErrorLabel;
    @FXML private Button simulateCardPaymentButton;
    @FXML private Label formStateLabel;
    @FXML private Label questionnaireValidationLabel;
    @FXML private TextArea chiefComplaintArea;
    @FXML private TextArea symptomSummaryArea;
    @FXML private TextArea voiceTranscriptArea;
    @FXML private Label voiceStatusLabel;
    @FXML private Button startVoiceButton;
    @FXML private Button stopVoiceButton;
    @FXML private ComboBox<Integer> stressCombo;
    @FXML private ComboBox<Integer> anxietyCombo;
    @FXML private ComboBox<Integer> moodCombo;
    @FXML private ComboBox<Integer> sleepCombo;
    @FXML private ComboBox<Integer> energyCombo;
    @FXML private ComboBox<Integer> supportCombo;
    @FXML private ComboBox<Integer> urgencyCombo;
    @FXML private ComboBox<String> selfHarmRiskCombo;
    @FXML private TextArea additionalContextArea;
    @FXML private Button submitQuestionnaireButton;
    @FXML private WebView speechBridgeView;

    private final ConsultationWorkflowService workflowService = new ConsultationWorkflowService();
    private static final String FILTER_ALL = "Tous";
    private static final String FILTER_PAID = "Payees";
    private static final String FILTER_UNPAID = "Non payees";
    private static final String SORT_DATE_DESC = "Date la plus recente";
    private static final String SORT_DATE_ASC = "Date la plus ancienne";
    private User currentUser;
    private ConsultationPayment selectedPayment;
    private WebEngine speechBridgeEngine;
    private boolean speechBridgeReady;
    private boolean speechListening;
    private boolean nativeSpeechFallback;

    @FXML
    public void initialize() {
        configureScales();
        configureSimulatedCardFields();
        configureQuestionnaireValidation();
        configureSpeechBridge();
        configureListFilters();
        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;
        loadPayments();
    }

    @FXML
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent view = loader.load();
            PatientDashboardController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            summaryTitleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            showError("Impossible de revenir au tableau de bord.");
        }
    }

    @FXML
    private void openPaymentLink() {
        if (selectedPayment == null || selectedPayment.getCheckoutUrl() == null || selectedPayment.getCheckoutUrl().isBlank()) {
            showError("Aucun lien de paiement disponible.");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(selectedPayment.getCheckoutUrl()));
        } catch (Exception e) {
            showError("Impossible d'ouvrir le lien Stripe: " + e.getMessage());
        }
    }

    @FXML
    private void synchronizePayment() {
        if (selectedPayment == null) {
            showError("Selectionnez d'abord un paiement.");
            return;
        }
        try {
            workflowService.synchronizePaymentStatus(selectedPayment.getId());
            loadPayments();
            showInfo("Le statut du paiement a ete synchronise.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void markPaymentAsPaidLocally() {
        if (selectedPayment == null) {
            showError("Selectionnez d'abord un paiement.");
            return;
        }
        try {
            workflowService.markPaymentAsPaidLocally(selectedPayment.getId());
            loadPayments();
            showInfo("Paiement marque comme valide en mode local.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void submitSimulatedCardPayment() {
        if (selectedPayment == null) {
            setSimulatedCardError("Selectionnez d'abord un paiement.");
            return;
        }
        String number = normalize(cardNumberField == null ? "" : cardNumberField.getText());
        String expiry = normalize(expiryField == null ? "" : expiryField.getText());
        String cvv = normalize(cvvField == null ? "" : cvvField.getText());

        if (!CARD_NUMBER_PATTERN.matcher(number).matches()) {
            setSimulatedCardError("Numero invalide. Format attendu: 4242 4242 4242 4242");
            return;
        }
        if (!EXPIRY_PATTERN.matcher(expiry).matches()) {
            setSimulatedCardError("Expiry invalide. Format attendu: MM/AA");
            return;
        }
        if (!CVV_PATTERN.matcher(cvv).matches()) {
            setSimulatedCardError("CVV invalide. Utilisez 3 ou 4 chiffres.");
            return;
        }

        setSimulatedCardError("");
        markPaymentAsPaidLocally();
        clearSimulatedCardForm();
    }

    @FXML
    private void submitQuestionnaire() {
        if (selectedPayment == null) {
            showError("Selectionnez d'abord une consultation.");
            return;
        }
        ConsultationQuestionnaire questionnaire = selectedPayment.getQuestionnaire();
        if (questionnaire == null) {
            questionnaire = new ConsultationQuestionnaire();
            questionnaire.setPaymentId(selectedPayment.getId());
            questionnaire.setRendezVousId(selectedPayment.getRendezVousId());
            questionnaire.setPatientId(selectedPayment.getPatientId());
            questionnaire.setPsychologueId(selectedPayment.getPsychologueId());
        }
        questionnaire.setChiefComplaint(normalize(chiefComplaintArea.getText()));
        questionnaire.setSymptomSummary(normalize(symptomSummaryArea.getText()));
        questionnaire.setStressLevel(valueOf(stressCombo));
        questionnaire.setAnxietyLevel(valueOf(anxietyCombo));
        questionnaire.setMoodLevel(valueOf(moodCombo));
        questionnaire.setSleepQuality(valueOf(sleepCombo));
        questionnaire.setEnergyLevel(valueOf(energyCombo));
        questionnaire.setSupportLevel(valueOf(supportCombo));
        questionnaire.setUrgencyLevel(valueOf(urgencyCombo));
        questionnaire.setSelfHarmRisk(selfHarmRiskCombo.getValue());
        questionnaire.setAdditionalContext(normalize(additionalContextArea.getText()));
        questionnaire.setVoiceTranscript(normalize(voiceTranscriptArea == null ? "" : voiceTranscriptArea.getText()));

        String validationError = validateQuestionnaireForm();
        if (validationError != null) {
            setQuestionnaireValidation(validationError, true);
            return;
        }

        try {
            workflowService.saveQuestionnaire(questionnaire);
            loadPayments();
            showInfo("Formulaire clinique enregistre.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void configureScales() {
        for (ComboBox<Integer> combo : List.of(stressCombo, anxietyCombo, moodCombo, sleepCombo, energyCombo, supportCombo, urgencyCombo)) {
            if (combo != null) {
                combo.getItems().setAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
                combo.setValue(5);
            }
        }
        if (selfHarmRiskCombo != null) {
            selfHarmRiskCombo.getItems().setAll("none", "moderate", "high");
            selfHarmRiskCombo.setValue("none");
        }
    }

    private void configureQuestionnaireValidation() {
        if (chiefComplaintArea != null) {
            chiefComplaintArea.textProperty().addListener((obs, oldValue, newValue) -> refreshQuestionnaireValidation());
        }
        if (symptomSummaryArea != null) {
            symptomSummaryArea.textProperty().addListener((obs, oldValue, newValue) -> refreshQuestionnaireValidation());
        }
        if (additionalContextArea != null) {
            additionalContextArea.textProperty().addListener((obs, oldValue, newValue) -> refreshQuestionnaireValidation());
        }
        if (voiceTranscriptArea != null) {
            voiceTranscriptArea.textProperty().addListener((obs, oldValue, newValue) -> refreshQuestionnaireValidation());
        }
        for (ComboBox<Integer> combo : List.of(stressCombo, anxietyCombo, moodCombo, sleepCombo, energyCombo, supportCombo, urgencyCombo)) {
            if (combo != null) {
                combo.valueProperty().addListener((obs, oldValue, newValue) -> refreshQuestionnaireValidation());
            }
        }
        if (selfHarmRiskCombo != null) {
            selfHarmRiskCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshQuestionnaireValidation());
        }
    }

    private void configureSpeechBridge() {
        if (speechBridgeView == null) {
            return;
        }
        speechBridgeView.setContextMenuEnabled(false);
        speechBridgeEngine = speechBridgeView.getEngine();
        speechBridgeEngine.setJavaScriptEnabled(true);
        speechBridgeEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> handleSpeechBridgeTitle(newTitle));
        speechBridgeEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                speechBridgeReady = true;
                setVoiceStatus("Dictée vocale prête.");
            }
        });
        speechBridgeEngine.load(getClass().getResource("/speech/speech_capture_bridge.html").toExternalForm());
    }

    private void configureSimulatedCardFields() {
        if (cardNumberField != null) {
            cardNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
                String digits = newValue == null ? "" : newValue.replaceAll("\\D", "");
                if (digits.length() > 16) {
                    digits = digits.substring(0, 16);
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        builder.append(' ');
                    }
                    builder.append(digits.charAt(i));
                }
                String formatted = builder.toString();
                if (!formatted.equals(newValue)) {
                    cardNumberField.setText(formatted);
                }
            });
        }
        if (expiryField != null) {
            expiryField.textProperty().addListener((obs, oldValue, newValue) -> {
                String digits = newValue == null ? "" : newValue.replaceAll("\\D", "");
                if (digits.length() > 4) {
                    digits = digits.substring(0, 4);
                }
                String formatted = digits.length() > 2
                        ? digits.substring(0, 2) + "/" + digits.substring(2)
                        : digits;
                if (!formatted.equals(newValue)) {
                    expiryField.setText(formatted);
                }
            });
        }
        if (cvvField != null) {
            cvvField.textProperty().addListener((obs, oldValue, newValue) -> {
                String digits = newValue == null ? "" : newValue.replaceAll("\\D", "");
                if (digits.length() > 4) {
                    digits = digits.substring(0, 4);
                }
                if (!digits.equals(newValue)) {
                    cvvField.setText(digits);
                }
            });
        }
    }

    private void loadPayments() {
        if (currentUser == null) {
            return;
        }
        try {
            workflowService.ensurePaymentsExistForAcceptedRendezVousByPatient(currentUser.getId());
            List<ConsultationPayment> payments = workflowService.getPaymentsByPatient(currentUser.getId());
            paymentsBox.getChildren().clear();

            long pendingCount = payments.stream().filter(ConsultationPayment::requiresPayment).count();
            long completedForms = payments.stream().filter(payment -> payment.getQuestionnaire() != null).count();
            paymentSummaryLabel.setText(payments.size() + " consultation(s) | " + pendingCount + " paiement(s) en attente");
            questionnaireSummaryLabel.setText(completedForms + " formulaire(s) cliniques envoyes");

            List<ConsultationPayment> visiblePayments = applyListFilters(payments);
            if (visiblePayments.isEmpty()) {
                Label empty = new Label("Aucune consultation acceptee n'attend de paiement pour le moment.");
                empty.setWrapText(true);
                empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
                paymentsBox.getChildren().add(empty);
                selectedPayment = visiblePayments.isEmpty() ? null : selectedPayment;
                refreshSelectionUI();
                return;
            }

            for (ConsultationPayment payment : visiblePayments) {
                paymentsBox.getChildren().add(buildPaymentCard(payment));
            }

            int selectionId = selectedPayment == null ? -1 : selectedPayment.getId();
            selectedPayment = visiblePayments.stream().filter(payment -> payment.getId() == selectionId).findFirst().orElse(visiblePayments.get(0));
            refreshSelectionUI();
        } catch (SQLException e) {
            showError("Impossible de charger les paiements: " + e.getMessage());
        }
    }

    private void configureListFilters() {
        if (paymentStatusFilterCombo != null) {
            paymentStatusFilterCombo.getItems().setAll(FILTER_ALL, FILTER_PAID, FILTER_UNPAID);
            paymentStatusFilterCombo.setValue(FILTER_ALL);
            paymentStatusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadPayments());
        }
        if (sortDateCombo != null) {
            sortDateCombo.getItems().setAll(SORT_DATE_DESC, SORT_DATE_ASC);
            sortDateCombo.setValue(SORT_DATE_DESC);
            sortDateCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadPayments());
        }
    }

    private List<ConsultationPayment> applyListFilters(List<ConsultationPayment> payments) {
        List<ConsultationPayment> filtered = new ArrayList<>();
        String statusFilter = paymentStatusFilterCombo == null || paymentStatusFilterCombo.getValue() == null
                ? FILTER_ALL
                : paymentStatusFilterCombo.getValue();

        for (ConsultationPayment payment : payments) {
            boolean keep = switch (statusFilter) {
                case FILTER_PAID -> payment.isPaid();
                case FILTER_UNPAID -> !payment.isPaid();
                default -> true;
            };
            if (keep) {
                filtered.add(payment);
            }
        }

        Comparator<ConsultationPayment> comparator = Comparator.comparing(
                ConsultationPayment::getAppointmentDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
        String sortMode = sortDateCombo == null || sortDateCombo.getValue() == null
                ? SORT_DATE_DESC
                : sortDateCombo.getValue();
        if (SORT_DATE_DESC.equals(sortMode)) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator.thenComparing(ConsultationPayment::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return filtered;
    }

    private VBox buildPaymentCard(ConsultationPayment payment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(cardStyle(payment));

        Label title = new Label(payment.getAppointmentType() + " - " + payment.getAppointmentDate().format(DATE_FORMATTER));
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label details = new Label(payment.getPsychologueName() + " | " + payment.getAppointmentTimeRange());
        details.setStyle("-fx-text-fill: #475569;");

        Label state = new Label("Paiement: " + payment.getStatus() + " | Montant: " + payment.getFormattedAmount());
        state.setStyle("-fx-text-fill: #1e293b; -fx-font-weight: bold;");

        Label formState = new Label(payment.getQuestionnaire() == null
                ? (payment.isPaid() ? "Formulaire en attente" : "Formulaire verrouille jusqu'au paiement")
                : "Formulaire deja transmis");
        formState.setStyle("-fx-text-fill: #64748b;");

        card.getChildren().addAll(title, details, state, formState);
        card.setOnMouseClicked(event -> {
            selectedPayment = payment;
            refreshSelectionUI();
        });
        return card;
    }

    private void refreshSelectionUI() {
        boolean hasSelection = selectedPayment != null;
        payNowButton.setDisable(!hasSelection || selectedPayment.isPaid() || selectedPayment.getCheckoutUrl() == null || selectedPayment.getCheckoutUrl().isBlank());
        syncPaymentButton.setDisable(!hasSelection || selectedPayment.getStripeSessionId() == null || selectedPayment.getStripeSessionId().isBlank());
        if (localPayButton != null) {
            boolean showLocalPay = hasSelection && !selectedPayment.isPaid() && !workflowService.isStripeConfigured();
            localPayButton.setVisible(showLocalPay);
            localPayButton.setManaged(showLocalPay);
            localPayButton.setDisable(!showLocalPay);
        }
        if (simulatedCardBox != null) {
            boolean showSimulatedCard = hasSelection && !selectedPayment.isPaid() && !workflowService.isStripeConfigured();
            simulatedCardBox.setVisible(showSimulatedCard);
            simulatedCardBox.setManaged(showSimulatedCard);
            if (simulateCardPaymentButton != null) {
                simulateCardPaymentButton.setDisable(!showSimulatedCard);
            }
            if (!showSimulatedCard) {
                clearSimulatedCardForm();
            }
        }

        if (!hasSelection) {
            summarySubtitleLabel.setText("Selectionnez une consultation acceptee pour continuer.");
            formStateLabel.setText("Le formulaire sera disponible ici apres un paiement Stripe valide.");
            clearForm();
            disableForm(true);
            setQuestionnaireValidation("Selectionnez une consultation pour remplir le formulaire.", false);
            return;
        }

        summarySubtitleLabel.setText(selectedPayment.getAppointmentType() + " avec " + selectedPayment.getPsychologueName()
                + " le " + selectedPayment.getAppointmentDate().format(DATE_FORMATTER)
                + " a " + selectedPayment.getAppointmentTimeRange());

        ConsultationQuestionnaire questionnaire = selectedPayment.getQuestionnaire();
        if (!selectedPayment.isPaid()
                && (selectedPayment.getCheckoutUrl() == null || selectedPayment.getCheckoutUrl().isBlank())
                && !workflowService.isStripeConfigured()) {
            formStateLabel.setText("Stripe n'est pas configure en local. Utilisez la carte simulee ci-dessus pour debloquer le formulaire.");
            clearForm();
            disableForm(true);
            setQuestionnaireValidation("Paiement requis avant la saisie clinique.", false);
            return;
        }
        if (!selectedPayment.isPaid()) {
            formStateLabel.setText("Paiement en attente. Ouvrez Stripe puis synchronisez le statut.");
            clearForm();
            disableForm(true);
            setQuestionnaireValidation("Le formulaire se debloque apres validation du paiement.", false);
            return;
        }

        disableForm(false);
        if (questionnaire == null) {
            formStateLabel.setText("Paiement recu. Remplissez maintenant le formulaire clinique.");
            clearForm();
            setQuestionnaireValidation("Motif principal et symptomes: minimum 10 caracteres chacun.", false);
            return;
        }

        formStateLabel.setText("Formulaire deja soumis. Vous pouvez le mettre a jour si necessaire.");
        fillForm(questionnaire);
    }

    private String cardStyle(ConsultationPayment payment) {
        boolean selected = selectedPayment != null && selectedPayment.getId() == payment.getId();
        String accent = payment.isPaid() ? "#16a34a" : "#2563eb";
        String background = selected ? "#eff6ff" : "#ffffff";
        return "-fx-background-color: " + background + "; -fx-background-radius: 18; -fx-border-radius: 18; "
                + "-fx-border-color: " + accent + "; -fx-border-width: 0 0 0 4;";
    }

    private void fillForm(ConsultationQuestionnaire questionnaire) {
        chiefComplaintArea.setText(questionnaire.getChiefComplaint());
        symptomSummaryArea.setText(questionnaire.getSymptomSummary());
        stressCombo.setValue(questionnaire.getStressLevel());
        anxietyCombo.setValue(questionnaire.getAnxietyLevel());
        moodCombo.setValue(questionnaire.getMoodLevel());
        sleepCombo.setValue(questionnaire.getSleepQuality());
        energyCombo.setValue(questionnaire.getEnergyLevel());
        supportCombo.setValue(questionnaire.getSupportLevel());
        urgencyCombo.setValue(questionnaire.getUrgencyLevel());
        selfHarmRiskCombo.setValue(questionnaire.getSelfHarmRisk());
        additionalContextArea.setText(questionnaire.getAdditionalContext());
        if (voiceTranscriptArea != null) {
            voiceTranscriptArea.setText(questionnaire.getVoiceTranscript());
        }
        refreshQuestionnaireValidation();
    }

    private void clearForm() {
        chiefComplaintArea.clear();
        symptomSummaryArea.clear();
        additionalContextArea.clear();
        if (voiceTranscriptArea != null) {
            voiceTranscriptArea.clear();
        }
        configureScales();
        stopVoiceCapture();
        refreshQuestionnaireValidation();
    }

    private void disableForm(boolean disabled) {
        for (var node : List.of(chiefComplaintArea, symptomSummaryArea, stressCombo, anxietyCombo, moodCombo,
                sleepCombo, energyCombo, supportCombo, urgencyCombo, selfHarmRiskCombo, additionalContextArea, voiceTranscriptArea)) {
            node.setDisable(disabled);
        }
        if (startVoiceButton != null) {
            startVoiceButton.setDisable(disabled || (!speechBridgeReady && !nativeSpeechFallback) || speechListening);
        }
        if (stopVoiceButton != null) {
            stopVoiceButton.setDisable(disabled || (!speechListening && !nativeSpeechFallback));
        }
        if (submitQuestionnaireButton != null && selectedPayment != null) {
            submitQuestionnaireButton.setDisable(disabled || !selectedPayment.isPaid() || validateQuestionnaireForm() != null);
        }
    }

    private int valueOf(ComboBox<Integer> comboBox) {
        return comboBox.getValue() == null ? 0 : comboBox.getValue();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearSimulatedCardForm() {
        if (cardNumberField != null) {
            cardNumberField.clear();
        }
        if (expiryField != null) {
            expiryField.clear();
        }
        if (cvvField != null) {
            cvvField.clear();
        }
        setSimulatedCardError("");
    }

    private void setSimulatedCardError(String message) {
        if (simulatedCardErrorLabel != null) {
            simulatedCardErrorLabel.setText(message == null ? "" : message);
        }
    }

    @FXML
    private void startVoiceCapture() {
        if (nativeSpeechFallback) {
            startNativeWindowsDictation();
            return;
        }
        if (speechBridgeEngine == null || !speechBridgeReady) {
            setVoiceStatus("Dictée vocale indisponible sur cette machine.");
            return;
        }
        speechBridgeEngine.executeScript("startRecognition()");
    }

    @FXML
    private void stopVoiceCapture() {
        if (nativeSpeechFallback) {
            stopNativeWindowsDictation();
            return;
        }
        if (speechBridgeEngine == null || !speechBridgeReady) {
            return;
        }
        speechBridgeEngine.executeScript("stopRecognition()");
    }

    private void handleSpeechBridgeTitle(String title) {
        if (title == null || !title.startsWith("speech:")) {
            return;
        }
        if (title.startsWith("speech:text:")) {
            String payload = decodePayload(title.substring("speech:text:".length()));
            if (voiceTranscriptArea != null) {
                voiceTranscriptArea.setText(payload);
            }
            setVoiceStatus(payload.isBlank() ? "Aucun texte reconnu." : "Transcription vocale capturee.");
            return;
        }
        if (title.startsWith("speech:status:")) {
            String status = decodePayload(title.substring("speech:status:".length()));
            speechListening = "listening".equalsIgnoreCase(status);
            if ("ready".equalsIgnoreCase(status)) {
                setVoiceStatus("Cliquez sur Demarrer la dictée pour parler.");
            } else if ("listening".equalsIgnoreCase(status)) {
                setVoiceStatus("Dictée en cours... parlez maintenant.");
            } else if ("stopped".equalsIgnoreCase(status)) {
                setVoiceStatus("Dictée arretee.");
            }
            refreshQuestionnaireValidation();
            return;
        }
        if (title.startsWith("speech:error:")) {
            String error = decodePayload(title.substring("speech:error:".length()));
            if (error.toLowerCase().contains("non compatible")) {
                enableNativeSpeechFallback();
                return;
            }
            setVoiceStatus("Dictée indisponible: " + error);
            speechListening = false;
            refreshQuestionnaireValidation();
        }
    }

    private String decodePayload(String payload) {
        return URLDecoder.decode(payload == null ? "" : payload, StandardCharsets.UTF_8);
    }

    private void refreshQuestionnaireValidation() {
        String validationError = validateQuestionnaireForm();
        if (validationError == null) {
            setQuestionnaireValidation("Formulaire pret a etre enregistre.", false);
        } else {
            setQuestionnaireValidation(validationError, false);
        }
        if (submitQuestionnaireButton != null) {
            boolean canSubmit = selectedPayment != null && selectedPayment.isPaid() && validationError == null;
            submitQuestionnaireButton.setDisable(!canSubmit);
        }
        if (startVoiceButton != null) {
            startVoiceButton.setDisable(selectedPayment == null || !selectedPayment.isPaid() || (!speechBridgeReady && !nativeSpeechFallback) || speechListening);
        }
        if (stopVoiceButton != null) {
            stopVoiceButton.setDisable(selectedPayment == null || !selectedPayment.isPaid() || (!speechListening && !nativeSpeechFallback));
        }
    }

    private String validateQuestionnaireForm() {
        if (selectedPayment == null || !selectedPayment.isPaid()) {
            return "Le formulaire sera disponible apres paiement.";
        }
        String chiefComplaint = normalize(chiefComplaintArea == null ? "" : chiefComplaintArea.getText());
        String symptomSummary = normalize(symptomSummaryArea == null ? "" : symptomSummaryArea.getText());
        if (chiefComplaint.length() < 10) {
            return "Le motif principal doit contenir au moins 10 caracteres.";
        }
        if (symptomSummary.length() < 10) {
            return "Le resume des symptomes doit contenir au moins 10 caracteres.";
        }
        if (selfHarmRiskCombo == null || selfHarmRiskCombo.getValue() == null || selfHarmRiskCombo.getValue().isBlank()) {
            return "Selectionnez le niveau de risque auto-agression.";
        }
        int urgency = valueOf(urgencyCombo);
        if (urgency >= 8 && normalize(additionalContextArea == null ? "" : additionalContextArea.getText()).length() < 10) {
            return "Ajoutez un contexte complementaire quand l'urgence ressentie est elevee.";
        }
        return null;
    }

    private void setQuestionnaireValidation(String message, boolean error) {
        if (questionnaireValidationLabel != null) {
            questionnaireValidationLabel.setText(message == null ? "" : message);
            questionnaireValidationLabel.setStyle("-fx-text-fill: " + (error ? "#b91c1c" : "#475569") + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    private void setVoiceStatus(String message) {
        if (voiceStatusLabel != null) {
            voiceStatusLabel.setText(message == null ? "" : message);
        }
    }

    private void enableNativeSpeechFallback() {
        nativeSpeechFallback = true;
        speechListening = false;
        if (startVoiceButton != null) {
            startVoiceButton.setText("Lancer Win+H");
        }
        if (stopVoiceButton != null) {
            stopVoiceButton.setText("Fermer la dictée");
        }
        setVoiceStatus("Le navigateur embarqué ne supporte pas la reconnaissance vocale. Utilisez la dictée Windows avec Win+H depuis ce bouton.");
        refreshQuestionnaireValidation();
    }

    private void startNativeWindowsDictation() {
        if (voiceTranscriptArea != null) {
            voiceTranscriptArea.requestFocus();
            voiceTranscriptArea.positionCaret(voiceTranscriptArea.getText() == null ? 0 : voiceTranscriptArea.getText().length());
        }
        try {
            Robot robot = new Robot();
            robot.delay(150);
            robot.keyPress(KeyEvent.VK_WINDOWS);
            robot.keyPress(KeyEvent.VK_H);
            robot.keyRelease(KeyEvent.VK_H);
            robot.keyRelease(KeyEvent.VK_WINDOWS);
            speechListening = true;
            setVoiceStatus("Dictée Windows ouverte. Parlez, le texte sera saisi dans la zone de transcription.");
            refreshQuestionnaireValidation();
        } catch (AWTException e) {
            setVoiceStatus("Impossible d'ouvrir la dictée Windows automatiquement. Utilisez le raccourci Win+H.");
        }
    }

    private void stopNativeWindowsDictation() {
        try {
            Robot robot = new Robot();
            robot.delay(100);
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
        } catch (AWTException ignored) {
        }
        speechListening = false;
        setVoiceStatus("Dictée Windows fermée.");
        refreshQuestionnaireValidation();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
