package org.example.controller;

import entities.Disponibilite;
import entities.RendezVous;
import entities.TypeRendezVous;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;
import org.example.entities.User;
import org.example.entities.AiTriageResult;
import org.example.service.AiTriageService;
import org.example.service.MedicalValidationService;
import org.example.service.UserService;
import org.example.utils.DataSource;
import org.example.utils.UserSession;
import services.ServiceDisponibilite;
import services.ServiceRendezVous;
import services.ServiceTypeRendezVous;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RendezVousController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label welcomeLabel;
    @FXML private Label patientNomLabel;
    @FXML private Label patientPrenomLabel;
    @FXML private ComboBox<User> comboPsychiatres;
    @FXML private ComboBox<TypeRendezVous> comboTypes;
    @FXML private DatePicker calendarDatePicker;
    @FXML private FlowPane slotsFlowPane;
    @FXML private Label selectedDateLabel;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private TextField searchRendezVousField;
    @FXML private Label appointmentsSummaryLabel;
    @FXML private TextArea notesPatientArea;
    @FXML private Button aiAssistantButton;
    @FXML private Label aiAssistantSummaryLabel;
    @FXML private Label aiAssistantQuestionsLabel;
    @FXML private WebView locationMapView;
    @FXML private Label selectedLocationLabel;
    @FXML private Label locationHintLabel;
    @FXML private Label selectionActionHintLabel;
    @FXML private Button clearLocationButton;
    @FXML private Label errorGlobal;
    @FXML private Button btnValider;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private TableView<RendezVous> tableRendezVous;
    @FXML private TableColumn<RendezVous, String> colType;
    @FXML private TableColumn<RendezVous, String> colPsychologue;
    @FXML private TableColumn<RendezVous, String> colDate;
    @FXML private TableColumn<RendezVous, String> colHeure;
    @FXML private TableColumn<RendezVous, String> colStatut;

    private final ServiceRendezVous serviceRendezVous = new ServiceRendezVous();
    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();
    private final ServiceTypeRendezVous serviceTypeRendezVous = new ServiceTypeRendezVous();
    private final AiTriageService aiTriageService = new AiTriageService();

    private User currentUser;
    private RendezVous selectedRendezVous;
    private Disponibilite selectedDisponibilite;
    private List<User> psychiatres;
    private final ObservableList<RendezVous> rendezVousItems = FXCollections.observableArrayList();
    private final FilteredList<RendezVous> filteredRendezVous = new FilteredList<>(rendezVousItems, item -> true);
    private WebEngine mapEngine;
    private boolean mapReady;
    private Double selectedLatitude;
    private Double selectedLongitude;
    private Set<LocalDate> availableDates = Collections.emptySet();

    @FXML
    public void initialize() {
        configureTable();
        configureComboBoxes();
        configureFilters();
        configureCalendar();
        configureMapPicker();
        currentUser = UserSession.getInstance();
        loadPsychiatres();
        loadAllowedTypes();
        safeRefreshView();
        Platform.runLater(this::initializeSelections);

        comboPsychiatres.valueProperty().addListener((obs, oldValue, newValue) -> refreshCalendarAndSlots());
        tableRendezVous.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRendezVous = newValue;
            populateForm(newValue);
            updateButtons();
        });
        updateButtons();
    }

    public void setUserData(User user) {
        currentUser = user;
        safeRefreshView();
    }

    @FXML
    public void onPsychiatreSelected() {
        selectedDisponibilite = null;
        refreshCalendarAndSlots();
    }

    @FXML
    public void onTypeSelected() {
        if (errorGlobal != null) {
            errorGlobal.setText("");
        }
    }

    @FXML
    public void onCalendarDateSelected() {
        refreshSlotsForSelection();
    }

    @FXML
    public void lancerAssistantIa() {
        List<User> availablePsychologues = psychiatres == null ? Collections.emptyList() : psychiatres;
        if (availablePsychologues.isEmpty()) {
            showMessage("Aucun psychologue disponible pour l'assistant IA", "red");
            return;
        }
        if (!aiTriageService.isConfigured()) {
            showMessage("Assistant IA indisponible: configurez OPENAI_API_KEY dans config/openai.properties", "red");
            return;
        }

        String initialNotes = MedicalValidationService.normalize(notesPatientArea.getText());
        if (initialNotes.isBlank()) {
            initialNotes = collectAssistantPrompt();
            if (initialNotes == null || initialNotes.isBlank()) {
                showMessage("L'assistant IA a besoin d'un motif principal pour analyser la demande", "red");
                return;
            }
            notesPatientArea.setText(initialNotes);
        }

        setAiAssistantBusy(true);
        final String prompt = initialNotes;
        CompletableFuture
                .supplyAsync(() -> aiTriageService.analyzeAppointmentRequest(prompt, new ArrayList<>(availablePsychologues)))
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    setAiAssistantBusy(false);
                    if (throwable != null) {
                        showMessage("Assistant IA indisponible: " + rootCauseMessage(throwable), "red");
                        return;
                    }
                    applyAiTriageResult(result);
                }));
    }

    @FXML
    public void validerRendezVous() {
        TypeRendezVous type = comboTypes.getValue();
        User psychologue = comboPsychiatres.getValue();
        Disponibilite dispo = selectedDisponibilite;
        if (type == null || psychologue == null || dispo == null) {
            showMessage("Psychiatre, type, date et creneau sont obligatoires", "red");
            return;
        }
        if (!hasSelectedLocation()) {
            showMessage("Selectionnez votre localisation sur la carte", "red");
            return;
        }

        RendezVous rendezVous = new RendezVous(resolvePatientAge(), buildAddressFromCoordinates(), type.getId(), dispo.getId(), getCurrentUserId());
        rendezVous.setLatitude(selectedLatitude);
        rendezVous.setLongitude(selectedLongitude);
        rendezVous.setNotesPatient(MedicalValidationService.normalize(notesPatientArea.getText()));
        if (serviceRendezVous.ajouter(rendezVous)) {
            showMessage("Rendez-vous reserve et en attente de reponse du psychiatre", "green");
            resetForm();
            refreshTable();
        } else {
            showMessage(resolveBusinessError("Reservation refusee"), "red");
        }
    }

    @FXML
    public void modifierRendezVous() {
        if (selectedRendezVous == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }
        TypeRendezVous type = comboTypes.getValue();
        User psychologue = comboPsychiatres.getValue();
        Disponibilite dispo = selectedDisponibilite;
        if (type == null || psychologue == null || dispo == null) {
            showMessage("Psychiatre, type, date et creneau sont obligatoires", "red");
            return;
        }
        if (!hasSelectedLocation()) {
            showMessage("Selectionnez votre localisation sur la carte", "red");
            return;
        }

        selectedRendezVous.setAge(resolvePatientAge());
        selectedRendezVous.setAdresse(buildAddressFromCoordinates());
        selectedRendezVous.setLatitude(selectedLatitude);
        selectedRendezVous.setLongitude(selectedLongitude);
        selectedRendezVous.setTypeId(type.getId());
        selectedRendezVous.setDispoId(dispo.getId());
        selectedRendezVous.setUserId(getCurrentUserId());
        selectedRendezVous.setNotesPatient(MedicalValidationService.normalize(notesPatientArea.getText()));

        if (serviceRendezVous.modifier(selectedRendezVous)) {
            showMessage("Rendez-vous modifie et renvoye au psychiatre", "green");
            resetForm();
            refreshTable();
        } else {
            showMessage(resolveBusinessError("Modification refusee"), "red");
        }
    }

    @FXML
    public void supprimerRendezVous() {
        if (selectedRendezVous == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }
        if (serviceRendezVous.supprimer(selectedRendezVous.getId())) {
            showMessage("Rendez-vous supprime", "green");
            resetForm();
            refreshTable();
        } else {
            showMessage(resolveBusinessError("Suppression impossible"), "red");
        }
    }

    @FXML
    public void resetForm() {
        selectedRendezVous = null;
        selectedDisponibilite = null;
        comboPsychiatres.getSelectionModel().clearSelection();
        comboTypes.getSelectionModel().clearSelection();
        if (calendarDatePicker != null) {
            calendarDatePicker.setValue(null);
        }
        if (slotsFlowPane != null) {
            slotsFlowPane.getChildren().clear();
        }
        if (selectedDateLabel != null) {
            selectedDateLabel.setText("Aucune date selectionnee");
        }
        if (notesPatientArea != null) {
            notesPatientArea.clear();
        }
        clearAiAssistantFeedback();
        clearLocationSelection();
        tableRendezVous.getSelectionModel().clearSelection();
        errorGlobal.setText("");
        updateButtons();
    }

    @FXML
    public void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/patient_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) comboTypes.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                comboTypes.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.out.println("Erreur retour dashboard: " + e.getMessage());
        }
    }

    private void configureTable() {
        tableRendezVous.setFixedCellSize(42);
        tableRendezVous.setPlaceholder(new Label("Aucun rendez-vous pour ce patient. Faites une reservation pour le voir apparaitre ici."));
        colType.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTypeLibelle()));
        colPsychologue.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getPsychologueNomComplet()));
        colDate.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDateDisponibilite()));
        colHeure.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getHeureDebut() + " - " + data.getValue().getHeureFin()));
        colStatut.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatut()));
    }

    private void configureComboBoxes() {
        comboTypes.setVisibleRowCount(6);
        comboTypes.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeRendezVous object) {
                return object == null ? "" : object.getLibelle();
            }

            @Override
            public TypeRendezVous fromString(String string) {
                return null;
            }
        });
        comboPsychiatres.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : buildPsychiatreLabel(item));
            }
        });
        comboPsychiatres.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : buildPsychiatreLabel(item));
            }
        });
        comboPsychiatres.setVisibleRowCount(8);
    }

    private void populateForm(RendezVous rendezVous) {
        if (rendezVous == null) {
            return;
        }
        selectedRendezVous = rendezVous;
        comboPsychiatres.getSelectionModel().select(findPsychiatreById(rendezVous.getPsychologueId()));
        comboTypes.getSelectionModel().select(findTypeById(rendezVous.getTypeId()));
        if (calendarDatePicker != null && rendezVous.getDateDisponibilite() != null && !rendezVous.getDateDisponibilite().isBlank()) {
            calendarDatePicker.setValue(LocalDate.parse(rendezVous.getDateDisponibilite()));
        }
        refreshSlotsForSelection();
        if (notesPatientArea != null) {
            notesPatientArea.setText(MedicalValidationService.normalize(rendezVous.getNotesPatient()));
        }
        clearAiAssistantFeedback();
        selectedLatitude = rendezVous.getLatitude();
        selectedLongitude = rendezVous.getLongitude();
        updateLocationLabels();
        syncLocationToMap();
    }

    private void refreshCalendarAndSlots() {
        User psychologue = comboPsychiatres.getValue();
        availableDates = psychologue == null ? Collections.emptySet() : loadAvailableDates(psychologue.getId());
        configureCalendar();
        if (psychologue == null) {
            if (calendarDatePicker != null) {
                calendarDatePicker.setValue(null);
            }
            selectedDisponibilite = null;
            if (slotsFlowPane != null) {
                slotsFlowPane.getChildren().clear();
            }
            if (selectedDateLabel != null) {
                selectedDateLabel.setText("Choisissez d'abord un psychiatre");
            }
            return;
        }
        if (calendarDatePicker != null) {
            LocalDate currentDate = calendarDatePicker.getValue();
            if (currentDate == null || !availableDates.contains(currentDate)) {
                calendarDatePicker.setValue(availableDates.isEmpty() ? null : availableDates.stream().sorted().findFirst().orElse(null));
            }
        }
        refreshSlotsForSelection();
    }

    private void refreshSlotsForSelection() {
        selectedDisponibilite = null;
        if (slotsFlowPane != null) {
            slotsFlowPane.getChildren().clear();
        }
        User psychologue = comboPsychiatres.getValue();
        LocalDate selectedDate = calendarDatePicker == null ? null : calendarDatePicker.getValue();
        if (psychologue == null || selectedDate == null) {
            if (selectedDateLabel != null) {
                selectedDateLabel.setText(psychologue == null ? "Choisissez d'abord un psychiatre" : "Choisissez une date disponible");
            }
            return;
        }

        List<Disponibilite> disponibilites;
        if (selectedRendezVous != null && selectedRendezVous.getPsychologueId() == psychologue.getId()
                && selectedDate.toString().equals(selectedRendezVous.getDateDisponibilite())) {
            disponibilites = serviceDisponibilite.getDisposLibresIncluding(selectedRendezVous.getDispoId()).stream()
                    .filter(dispo -> dispo.getPsychologueId() == psychologue.getId() && selectedDate.equals(dispo.getDate()))
                    .collect(Collectors.toList());
        } else {
            disponibilites = serviceDisponibilite.getDisposLibresByPsychologueAndDate(psychologue.getId(), selectedDate);
        }

        if (selectedDateLabel != null) {
            selectedDateLabel.setText("Date selectionnee : " + DATE_FORMATTER.format(selectedDate) + " | " + disponibilites.size() + " creneau(x)");
        }

        if (disponibilites.isEmpty()) {
            showMessage("Aucun creneau libre pour cette date", "red");
        } else {
            errorGlobal.setText("");
            for (Disponibilite disponibilite : disponibilites) {
                Button slotButton = new Button(disponibilite.getHeureDebut() + " - " + disponibilite.getHeureFin());
                slotButton.setStyle(buildSlotStyle(false));
                slotButton.setOnAction(event -> {
                    selectedDisponibilite = disponibilite;
                    highlightSelectedSlot();
                    if (errorGlobal != null && errorGlobal.getText() != null
                            && errorGlobal.getText().toLowerCase().contains("creneau")) {
                        errorGlobal.setText("");
                    }
                });
                slotButton.setUserData(disponibilite);
                slotButton.setPrefWidth(170);
                slotButton.setPrefHeight(46);
                slotsFlowPane.getChildren().add(slotButton);
            }
            if (selectedRendezVous != null) {
                selectedDisponibilite = disponibilites.stream()
                        .filter(dispo -> dispo.getId() == selectedRendezVous.getDispoId())
                        .findFirst()
                        .orElse(disponibilites.get(0));
            } else {
                selectedDisponibilite = disponibilites.get(0);
            }
            highlightSelectedSlot();
        }
    }

    private void refreshTable() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        if (currentUser != null) {
            List<RendezVous> rendezVousList = serviceRendezVous.getRendezVousByUser(currentUser.getId());
            rendezVousItems.setAll(rendezVousList);
            applyFilters();
            updateAppointmentsSummary(rendezVousList.size(), filteredRendezVous.size());
            System.out.println("RendezVous patient #" + currentUser.getId() + " charges: " + rendezVousList.size());
        } else {
            rendezVousItems.clear();
            filteredRendezVous.setPredicate(item -> false);
            updateAppointmentsSummary(0, 0);
        }
    }

    private void loadAllowedTypes() {
        List<TypeRendezVous> allowedTypes = serviceTypeRendezVous.afficherTout().stream()
                .filter(type -> {
                    String libelle = type.getLibelle() == null ? "" : type.getLibelle().trim().toLowerCase();
                    return libelle.equals("consultation") || libelle.equals("suivi");
                })
                .collect(Collectors.toList());
        comboTypes.setItems(FXCollections.observableArrayList(allowedTypes));
        if (!allowedTypes.isEmpty()) {
            comboTypes.getSelectionModel().selectFirst();
        }
    }

    private void loadPsychiatres() {
        psychiatres = UserService.getByRole("ROLE_PSYCHOLOGUE").stream().collect(Collectors.toList());
        comboPsychiatres.setItems(FXCollections.observableArrayList(psychiatres));
        if (!psychiatres.isEmpty()) {
            comboPsychiatres.getSelectionModel().selectFirst();
            refreshCalendarAndSlots();
        }
    }

    private void configureFilters() {
        tableRendezVous.setItems(filteredRendezVous);
        if (filterStatutCombo != null) {
            filterStatutCombo.setItems(FXCollections.observableArrayList("Tous", "en attente", "acceptee"));
            filterStatutCombo.getSelectionModel().selectFirst();
            filterStatutCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (searchRendezVousField != null) {
            searchRendezVousField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
    }

    private void applyFilters() {
        String search = searchRendezVousField == null ? "" : searchRendezVousField.getText();
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        String statut = filterStatutCombo == null || filterStatutCombo.getValue() == null
                ? "Tous"
                : filterStatutCombo.getValue().trim().toLowerCase();

        filteredRendezVous.setPredicate(rendezVous -> {
            boolean matchesStatut = "tous".equals(statut)
                    || MedicalValidationService.normalize(rendezVous.getStatut()).equalsIgnoreCase(statut);
            if (!matchesStatut) {
                return false;
            }
            if (normalizedSearch.isBlank()) {
                return true;
            }
            return contains(rendezVous.getTypeLibelle(), normalizedSearch)
                    || contains(rendezVous.getPsychologueNomComplet(), normalizedSearch)
                    || contains(rendezVous.getDateDisponibilite(), normalizedSearch)
                    || contains(rendezVous.getHeureDebut(), normalizedSearch)
                    || contains(rendezVous.getHeureFin(), normalizedSearch)
                    || contains(rendezVous.getStatut(), normalizedSearch);
        });
        updateAppointmentsSummary(rendezVousItems.size(), filteredRendezVous.size());
        if (tableRendezVous != null) {
            tableRendezVous.refresh();
        }
    }

    private void updateAppointmentsSummary(int totalCount, int visibleCount) {
        if (appointmentsSummaryLabel == null) {
            return;
        }
        if (currentUser == null) {
            appointmentsSummaryLabel.setText("Aucun patient en session.");
            return;
        }
        if (totalCount == 0) {
            appointmentsSummaryLabel.setText("Aucun rendez-vous enregistre pour " + currentUser.getPrenom() + " " + currentUser.getNom() + ".");
            return;
        }
        if (visibleCount != totalCount) {
            appointmentsSummaryLabel.setText(visibleCount + " rendez-vous affiche(s) sur " + totalCount + " pour " + currentUser.getPrenom() + " " + currentUser.getNom() + ".");
            return;
        }
        appointmentsSummaryLabel.setText(totalCount + " rendez-vous charge(s) pour " + currentUser.getPrenom() + " " + currentUser.getNom() + ".");
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private TypeRendezVous findTypeById(int typeId) {
        return comboTypes.getItems().stream().filter(type -> type.getId() == typeId).findFirst().orElse(null);
    }

    private User findPsychiatreById(int psychologueId) {
        return psychiatres.stream().filter(user -> user.getId() == psychologueId).findFirst().orElse(null);
    }

    private String buildPsychiatreLabel(User user) {
        if (user.getSpecialite() == null || user.getSpecialite().isBlank()) {
            return "Dr. " + user.getPrenom() + " " + user.getNom();
        }
        return "Dr. " + user.getPrenom() + " " + user.getNom() + " - " + user.getSpecialite();
    }

    private void loadWelcomeLabel() {
        try {
            Connection cnx = DataSource.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement("SELECT nom, prenom FROM user WHERE id = ?");
            ps.setInt(1, getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                welcomeLabel.setText("Reserver un rendez-vous");
                if (patientNomLabel != null) {
                    patientNomLabel.setText(rs.getString("nom"));
                }
                if (patientPrenomLabel != null) {
                    patientPrenomLabel.setText(rs.getString("prenom"));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur user: " + e.getMessage());
        }
    }

    private int resolvePatientAge() {
        try {
            Connection cnx = DataSource.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement("SELECT dateNaissance FROM user WHERE id = ?");
            ps.setInt(1, getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getDate("dateNaissance") != null) {
                LocalDate birthDate = rs.getDate("dateNaissance").toLocalDate();
                return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
            }
        } catch (Exception e) {
            System.out.println("Erreur calcul age patient: " + e.getMessage());
        }
        return 0;
    }

    private int getCurrentUserId() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        return currentUser.getId();
    }

    private void updateButtons() {
        if (btnValider != null) {
            btnValider.setDisable(false);
            btnValider.setText(selectedRendezVous == null ? "Reserver maintenant" : "Enregistrer les changements");
        }
        boolean hasSelection = selectedRendezVous != null;
        if (btnModifier != null) {
            btnModifier.setDisable(!hasSelection);
            btnModifier.setOpacity(hasSelection ? 1.0 : 0.45);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setDisable(!hasSelection);
            btnSupprimer.setOpacity(hasSelection ? 1.0 : 0.45);
        }
        if (clearLocationButton != null) {
            clearLocationButton.setDisable(!hasSelectedLocation());
        }
        if (aiAssistantButton != null && !aiAssistantButton.isDisabled()) {
            aiAssistantButton.setDisable(false);
        }
        if (selectionActionHintLabel != null) {
            selectionActionHintLabel.setText(hasSelection
                    ? "Mode modification actif : vous pouvez changer le creneau ou annuler ce rendez-vous."
                    : "Pour modifier ou annuler, selectionnez d'abord un rendez-vous dans le tableau ci-dessous.");
        }
    }

    private void showMessage(String message, String color) {
        errorGlobal.setText(message);
        errorGlobal.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void safeRefreshView() {
        try {
            loadWelcomeLabel();
            refreshTable();
        } catch (Exception e) {
            showMessage("Impossible de charger les rendez-vous pour le moment", "red");
            System.out.println("Erreur chargement rendez-vous: " + e.getMessage());
        }
    }

    private void initializeSelections() {
        if (comboTypes.getValue() == null && !comboTypes.getItems().isEmpty()) {
            comboTypes.getSelectionModel().selectFirst();
        }
        if (comboPsychiatres.getValue() == null && !comboPsychiatres.getItems().isEmpty()) {
            comboPsychiatres.getSelectionModel().selectFirst();
        }
        refreshCalendarAndSlots();
    }

    private String resolveBusinessError(String defaultMessage) {
        String serviceMessage = serviceRendezVous.getLastValidationError();
        return serviceMessage == null || serviceMessage.isBlank() ? defaultMessage : serviceMessage;
    }

    private void configureCalendar() {
        if (calendarDatePicker == null) {
            return;
        }
        calendarDatePicker.setEditable(false);
        calendarDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                boolean disabled = empty || item == null || item.isBefore(LocalDate.now())
                        || (!availableDates.isEmpty() && !availableDates.contains(item));
                setDisable(disabled);
                if (disabled) {
                    setStyle("-fx-background-color: #f1f1f1; -fx-text-fill: #999999;");
                } else {
                    setStyle("-fx-background-color: #dff5ea; -fx-text-fill: #1f513f; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void highlightSelectedSlot() {
        if (slotsFlowPane == null) {
            return;
        }
        for (var node : slotsFlowPane.getChildren()) {
            if (!(node instanceof Button button)) {
                continue;
            }
            Disponibilite disponibilite = (Disponibilite) button.getUserData();
            boolean selected = selectedDisponibilite != null && disponibilite != null
                    && disponibilite.getId() == selectedDisponibilite.getId();
            button.setStyle(buildSlotStyle(selected));
        }
    }

    private String buildSlotStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: linear-gradient(to right, #2a9d8f, #1f7a8c); "
                    + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 14; "
                    + "-fx-padding: 10 14; -fx-effect: dropshadow(gaussian, rgba(31,122,140,0.26), 16, 0.2, 0, 4);";
        }
        return "-fx-background-color: linear-gradient(to bottom, #ffffff, #f5fbf8); -fx-text-fill: #264653; -fx-font-weight: bold; "
                + "-fx-background-radius: 14; -fx-padding: 10 14; -fx-border-color: #d7e3dc; "
                + "-fx-border-radius: 14;";
    }

    @FXML
    private void clearSelectedLocation() {
        clearLocationSelection();
    }

    private void configureMapPicker() {
        if (locationMapView == null) {
            return;
        }
        locationMapView.setContextMenuEnabled(false);
        mapEngine = locationMapView.getEngine();
        mapEngine.setJavaScriptEnabled(true);
        mapEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> handleMapTitleMessage(newTitle));
        mapEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                mapReady = true;
                JSObject window = (JSObject) mapEngine.executeScript("window");
                window.setMember("javaConnector", new MapBridge());
                mapEngine.executeScript("if (typeof notifyUiReady === 'function') { notifyUiReady(); }");
                syncLocationToMap();
            }
        });
        String url = getClass().getResource("/map/location_picker.html").toExternalForm();
        mapEngine.load(url);
        updateLocationLabels();
    }

    private void handleMapTitleMessage(String title) {
        if (title == null || !title.startsWith("patient-location:")) {
            return;
        }
        String payload = title.substring("patient-location:".length());
        String[] parts = payload.split(",");
        if (parts.length != 2) {
            return;
        }
        try {
            double latitude = Double.parseDouble(parts[0]);
            double longitude = Double.parseDouble(parts[1]);
            applySelectedLocation(latitude, longitude);
        } catch (NumberFormatException ignored) {
        }
    }

    private boolean hasSelectedLocation() {
        return selectedLatitude != null && selectedLongitude != null;
    }

    private String buildAddressFromCoordinates() {
        if (!hasSelectedLocation()) {
            return "Localisation non selectionnee";
        }
        return String.format(Locale.US, "Localisation patient (%.6f, %.6f)", selectedLatitude, selectedLongitude);
    }

    private void updateLocationLabels() {
        if (selectedLocationLabel != null) {
            selectedLocationLabel.setText(hasSelectedLocation()
                    ? String.format("Position choisie : %.6f, %.6f", selectedLatitude, selectedLongitude)
                    : "Aucune position selectionnee");
        }
        if (locationHintLabel != null) {
            locationHintLabel.setText(hasSelectedLocation()
                    ? "La localisation du patient sera jointe a la reservation."
                    : "Cliquez sur la carte pour definir votre position.");
        }
        if (clearLocationButton != null) {
            clearLocationButton.setDisable(!hasSelectedLocation());
        }
    }

    private void clearLocationSelection() {
        selectedLatitude = null;
        selectedLongitude = null;
        updateLocationLabels();
        syncLocationToMap();
    }

    private void syncLocationToMap() {
        if (!mapReady || mapEngine == null) {
            return;
        }
        if (hasSelectedLocation()) {
            mapEngine.executeScript(String.format(Locale.US, "setMarkerFromJava(%f,%f);", selectedLatitude, selectedLongitude));
        } else {
            mapEngine.executeScript("clearMarkerFromJava();");
        }
    }

    public final class MapBridge {
        public void onLocationSelected(double latitude, double longitude) {
            applySelectedLocation(latitude, longitude);
        }
    }

    private void applySelectedLocation(double latitude, double longitude) {
        selectedLatitude = latitude;
        selectedLongitude = longitude;
        Platform.runLater(() -> {
            updateLocationLabels();
            syncLocationToMap();
            if (errorGlobal != null && errorGlobal.getText() != null
                    && errorGlobal.getText().toLowerCase().contains("localisation")) {
                errorGlobal.setText("");
            }
        });
    }

    private Set<LocalDate> loadAvailableDates(int psychologueId) {
        return serviceDisponibilite.getDisposLibresByPsychologue(psychologueId).stream()
                .map(Disponibilite::getDate)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void applyAiTriageResult(AiTriageResult result) {
        if (result == null) {
            showMessage("Assistant IA: reponse vide", "red");
            return;
        }

        if (result.getSynthesizedNotes() != null && !result.getSynthesizedNotes().isBlank()) {
            notesPatientArea.setText(MedicalValidationService.normalize(result.getSynthesizedNotes()));
        }
        selectRecommendedType(result.getRecommendedType());
        selectRecommendedPsychologue(result);

        if (aiAssistantSummaryLabel != null) {
            aiAssistantSummaryLabel.setText(buildAssistantSummary(result));
        }
        if (aiAssistantQuestionsLabel != null) {
            String questions = result.getFollowUpQuestions() == null || result.getFollowUpQuestions().isEmpty()
                    ? "Questions IA: aucune relance supplementaire."
                    : "Questions IA: " + String.join(" | ", result.getFollowUpQuestions());
            aiAssistantQuestionsLabel.setText(questions);
        }

        refreshCalendarAndSlots();
        if (result.isEmergency()) {
            showEmergencyAlert();
            showMessage("Assistant IA: demande sensible detectee, verification humaine recommandee", "red");
            return;
        }
        showMessage("Assistant IA: type et psychologue recommandes appliques au formulaire", "green");
    }

    private void selectRecommendedType(String recommendedType) {
        if (recommendedType == null || recommendedType.isBlank() || comboTypes == null) {
            return;
        }
        comboTypes.getItems().stream()
                .filter(type -> recommendedType.equalsIgnoreCase(MedicalValidationService.normalize(type.getLibelle())))
                .findFirst()
                .ifPresent(type -> comboTypes.getSelectionModel().select(type));
    }

    private void selectRecommendedPsychologue(AiTriageResult result) {
        if (comboPsychiatres == null || psychiatres == null || psychiatres.isEmpty()) {
            return;
        }

        String suggestedName = normalizeForMatch(result.getSuggestedPsychologueName());
        if (!suggestedName.isBlank()) {
            Optional<User> directMatch = psychiatres.stream()
                    .filter(user -> normalizeForMatch(user.getPrenom() + " " + user.getNom()).contains(suggestedName)
                            || suggestedName.contains(normalizeForMatch(user.getPrenom() + " " + user.getNom())))
                    .findFirst();
            if (directMatch.isPresent()) {
                comboPsychiatres.getSelectionModel().select(directMatch.get());
                return;
            }
        }

        String speciality = normalizeForMatch(result.getRecommendedSpeciality());
        psychiatres.stream()
                .filter(user -> !speciality.isBlank() && normalizeForMatch(user.getSpecialite()).contains(speciality))
                .findFirst()
                .ifPresent(user -> comboPsychiatres.getSelectionModel().select(user));
    }

    private String buildAssistantSummary(AiTriageResult result) {
        StringBuilder summary = new StringBuilder("Synthese IA: ");
        summary.append(result.getPriorityLevel() == null || result.getPriorityLevel().isBlank()
                ? "priorite non precisee"
                : "priorite " + result.getPriorityLevel());
        if (result.getRecommendedSpeciality() != null && !result.getRecommendedSpeciality().isBlank()) {
            summary.append(" | specialite conseillee: ").append(result.getRecommendedSpeciality());
        }
        if (result.getRationale() != null && !result.getRationale().isBlank()) {
            summary.append(" | ").append(result.getRationale());
        }
        return summary.toString();
    }

    private void clearAiAssistantFeedback() {
        if (aiAssistantSummaryLabel != null) {
            aiAssistantSummaryLabel.setText("Assistant IA inactif.");
        }
        if (aiAssistantQuestionsLabel != null) {
            aiAssistantQuestionsLabel.setText("Questions IA: aucune.");
        }
    }

    private void setAiAssistantBusy(boolean busy) {
        if (aiAssistantButton != null) {
            aiAssistantButton.setDisable(busy);
            aiAssistantButton.setText(busy ? "Analyse IA en cours..." : "Assistant IA");
        }
        if (aiAssistantSummaryLabel != null && busy) {
            aiAssistantSummaryLabel.setText("Assistant IA: analyse de la demande patient...");
        }
    }

    private String collectAssistantPrompt() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Assistant IA");
        dialog.setHeaderText("Quel est votre probleme principal ?");
        dialog.setContentText("Expliquez en une ou deux phrases :");
        Optional<String> response = dialog.showAndWait();
        return response.map(MedicalValidationService::normalize).orElse("");
    }

    private void showEmergencyAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Alerte IA");
        alert.setHeaderText("Demande potentiellement urgente");
        alert.setContentText("L'assistant IA a detecte une situation sensible. Une validation humaine immediate est recommandee.");
        alert.showAndWait();
    }

    private String normalizeForMatch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }
}
