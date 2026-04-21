package org.example.controllers;

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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import javafx.scene.layout.BorderPane;
import org.example.entities.User;
import org.example.service.UserService;
import org.example.service.MedicalValidationService;
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
import java.util.List;
import java.util.stream.Collectors;

public class RendezVousController {

    @FXML private Label welcomeLabel;
    @FXML private Label patientNomLabel;
    @FXML private Label patientPrenomLabel;
    @FXML private ComboBox<User> comboPsychiatres;
    @FXML private ComboBox<TypeRendezVous> comboTypes;
    @FXML private ComboBox<Disponibilite> comboDispos;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private TextField searchRendezVousField;
    @FXML private TextArea notesPatientArea;
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

    private final ServiceRendezVous srv = new ServiceRendezVous();
    private final ServiceDisponibilite sd = new ServiceDisponibilite();
    private final ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    private User currentUser;
    private RendezVous selectedRendezVous;
    private List<User> psychiatres;
    private final ObservableList<RendezVous> rendezVousItems = FXCollections.observableArrayList();
    private final FilteredList<RendezVous> filteredRendezVous = new FilteredList<>(rendezVousItems, item -> true);

    @FXML
    public void initialize() {
        configureTable();
        configureComboBoxes();
        configureFilters();
        currentUser = UserSession.getInstance();
        loadPsychiatres();
        loadAllowedTypes();
        safeRefreshView();
        Platform.runLater(this::initializeSelections);

        comboPsychiatres.valueProperty().addListener((obs, oldValue, newValue) -> refreshDisposForSelection());

        tableRendezVous.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRendezVous = newValue;
            populateForm(newValue);
            updateButtons();
        });

        updateButtons();
    }

    public void setUserData(User user) {
        this.currentUser = user;
        safeRefreshView();
    }

    @FXML
    public void onPsychiatreSelected() {
        refreshDisposForSelection();
    }

    @FXML
    public void onTypeSelected() {
        if (errorGlobal != null) {
            errorGlobal.setText("");
        }
    }

    @FXML
    public void validerRendezVous() {
        TypeRendezVous type = comboTypes.getValue();
        User psychologue = comboPsychiatres.getValue();
        Disponibilite dispo = comboDispos.getValue();
        if (type == null || psychologue == null || dispo == null) {
            showMessage("Psychiatre, type et disponibilite sont obligatoires", "red");
            return;
        }

        RendezVous rendezVous = new RendezVous(resolvePatientAge(), "Non renseignee", type.getId(), dispo.getId(), getCurrentUserId());
        rendezVous.setNotesPatient(MedicalValidationService.normalize(notesPatientArea.getText()));
        if (srv.ajouter(rendezVous)) {
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
        Disponibilite dispo = comboDispos.getValue();
        if (type == null || psychologue == null || dispo == null) {
            showMessage("Psychiatre, type et disponibilite sont obligatoires", "red");
            return;
        }

        selectedRendezVous.setAge(resolvePatientAge());
        selectedRendezVous.setAdresse("Non renseignee");
        selectedRendezVous.setTypeId(type.getId());
        selectedRendezVous.setDispoId(dispo.getId());
        selectedRendezVous.setUserId(getCurrentUserId());
        selectedRendezVous.setNotesPatient(MedicalValidationService.normalize(notesPatientArea.getText()));

        if (srv.modifier(selectedRendezVous)) {
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

        if (srv.supprimer(selectedRendezVous.getId())) {
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
        comboPsychiatres.getSelectionModel().clearSelection();
        comboTypes.getSelectionModel().clearSelection();
        comboDispos.getSelectionModel().clearSelection();
        comboDispos.getItems().clear();
        if (notesPatientArea != null) {
            notesPatientArea.clear();
        }
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
        comboDispos.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDate() + " | " + item.getHeureDebut() + " - " + item.getHeureFin());
            }
        });
        comboDispos.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDate() + " | " + item.getHeureDebut() + " - " + item.getHeureFin());
            }
        });
        comboDispos.setVisibleRowCount(10);
    }

    private void populateForm(RendezVous rendezVous) {
        if (rendezVous == null) {
            return;
        }
        comboPsychiatres.getSelectionModel().select(findPsychiatreById(rendezVous.getPsychologueId()));
        refreshDisposForSelection();
        comboTypes.getSelectionModel().select(findTypeById(rendezVous.getTypeId()));
        comboDispos.setItems(FXCollections.observableArrayList(
                sd.getDisposLibresIncluding(rendezVous.getDispoId()).stream()
                        .filter(dispo -> dispo.getId() == rendezVous.getDispoId()
                                || dispo.getPsychologueId() == rendezVous.getPsychologueId())
                        .collect(Collectors.toList())
        ));
        comboDispos.getSelectionModel().select(findDispoById(rendezVous.getDispoId()));
        if (notesPatientArea != null) {
            notesPatientArea.setText(MedicalValidationService.normalize(rendezVous.getNotesPatient()));
        }
    }

    private void refreshDisposForSelection() {
        User psychologue = comboPsychiatres.getValue();
        if (psychologue == null) {
            comboDispos.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Disponibilite> disponibilites = sd.getDisposLibresByPsychologue(psychologue.getId());
        comboDispos.setItems(FXCollections.observableArrayList(disponibilites));
        comboDispos.getSelectionModel().clearSelection();
        if (disponibilites.isEmpty()) {
            showMessage("Aucune disponibilite libre pour ce psychiatre", "red");
        } else {
            errorGlobal.setText("");
            comboDispos.getSelectionModel().selectFirst();
        }
    }

    private void refreshTable() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        if (currentUser != null) {
            rendezVousItems.setAll(srv.getRendezVousByUser(currentUser.getId()));
            applyFilters();
        }
    }

    private void loadAllowedTypes() {
        List<TypeRendezVous> allowedTypes = st.afficherTout().stream()
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
            refreshDisposForSelection();
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
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private TypeRendezVous findTypeById(int typeId) {
        return comboTypes.getItems().stream().filter(type -> type.getId() == typeId).findFirst().orElse(null);
    }

    private Disponibilite findDispoById(int dispoId) {
        return comboDispos.getItems().stream().filter(dispo -> dispo.getId() == dispoId).findFirst().orElse(null);
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
            int userId = getCurrentUserId();
            Connection cnx = DataSource.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement("SELECT nom, prenom, dateNaissance FROM user WHERE id = ?");
            ps.setInt(1, userId);
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
        }
        if (btnModifier != null) {
            btnModifier.setDisable(selectedRendezVous == null);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setDisable(selectedRendezVous == null);
        }
    }

    private void showMessage(String msg, String color) {
        errorGlobal.setText(msg);
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
        refreshDisposForSelection();
        if (comboDispos.getValue() == null && !comboDispos.getItems().isEmpty()) {
            comboDispos.getSelectionModel().selectFirst();
        }
    }

    private String resolveBusinessError(String defaultMessage) {
        String serviceMessage = srv.getLastValidationError();
        return serviceMessage == null || serviceMessage.isBlank() ? defaultMessage : serviceMessage;
    }
}
