package org.example.controllers;

import entities.Disponibilite;
import entities.RendezVous;
import entities.TypeRendezVous;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.example.entities.User;
import org.example.utils.DataSource;
import org.example.utils.UserSession;
import services.ServiceDisponibilite;
import services.ServiceRendezVous;
import services.ServiceTypeRendezVous;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;

public class RendezVousController {
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("[A-Za-z0-9À-ÿ'.,\\-\\s]{5,150}");

    @FXML private Label welcomeLabel;
    @FXML private Label patientNomLabel;
    @FXML private Label patientPrenomLabel;
    @FXML private TextField txtAge;
    @FXML private TextField txtAdresse;
    @FXML private ComboBox<TypeRendezVous> comboTypes;
    @FXML private ComboBox<Disponibilite> comboDispos;
    @FXML private Label errorGlobal;
    @FXML private Button btnValider;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private TableView<RendezVous> tableRendezVous;
    @FXML private TableColumn<RendezVous, String> colAge;
    @FXML private TableColumn<RendezVous, String> colAdresse;
    @FXML private TableColumn<RendezVous, String> colType;
    @FXML private TableColumn<RendezVous, String> colDate;
    @FXML private TableColumn<RendezVous, String> colHeure;

    private final ServiceRendezVous srv = new ServiceRendezVous();
    private final ServiceDisponibilite sd = new ServiceDisponibilite();
    private final ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    private User currentUser;
    private RendezVous selectedRendezVous;

    @FXML
    public void initialize() {
        configureTable();
        comboTypes.setItems(FXCollections.observableArrayList(st.afficherTout()));
        currentUser = UserSession.getInstance();
        loadWelcomeLabel();
        refreshDispos();
        refreshTable();

        tableRendezVous.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedRendezVous = newValue;
            populateForm(newValue);
            updateButtons();
        });

        updateButtons();
    }

    public void setUserData(User user) {
        this.currentUser = user;
        loadWelcomeLabel();
        refreshTable();
    }

    @FXML
    public void validerRendezVous() {
        Integer age = parseAge();
        if (age == null) {
            return;
        }

        TypeRendezVous type = comboTypes.getValue();
        Disponibilite dispo = comboDispos.getValue();
        if (type == null || dispo == null) {
            showMessage("Selection obligatoire", "red");
            return;
        }

        RendezVous r = new RendezVous(age, txtAdresse.getText().trim(), type.getId(), dispo.getId(), getCurrentUserId());
        if (srv.ajouter(r)) {
            sd.rendreIndisponible(dispo.getId());
            showMessage("Rendez-vous ajoute", "green");
            resetForm();
            refreshDispos();
            refreshTable();
        } else {
            showMessage("Erreur lors de l'ajout", "red");
        }
    }

    @FXML
    public void modifierRendezVous() {
        if (selectedRendezVous == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }

        Integer age = parseAge();
        if (age == null) {
            return;
        }

        TypeRendezVous type = comboTypes.getValue();
        Disponibilite dispo = comboDispos.getValue();
        if (type == null || dispo == null) {
            showMessage("Selection obligatoire", "red");
            return;
        }

        int ancienneDispoId = selectedRendezVous.getDispoId();
        selectedRendezVous.setAge(age);
        selectedRendezVous.setAdresse(txtAdresse.getText().trim());
        selectedRendezVous.setTypeId(type.getId());
        selectedRendezVous.setDispoId(dispo.getId());
        selectedRendezVous.setUserId(getCurrentUserId());

        if (srv.modifier(selectedRendezVous)) {
            if (ancienneDispoId != dispo.getId()) {
                sd.rendreDisponible(ancienneDispoId);
                sd.rendreIndisponible(dispo.getId());
            }
            showMessage("Rendez-vous modifie", "green");
            resetForm();
            refreshDispos();
            refreshTable();
        } else {
            showMessage("Erreur lors de la modification", "red");
        }
    }

    @FXML
    public void supprimerRendezVous() {
        if (selectedRendezVous == null) {
            showMessage("Selectionnez un rendez-vous", "red");
            return;
        }

        if (srv.supprimer(selectedRendezVous.getId())) {
            sd.rendreDisponible(selectedRendezVous.getDispoId());
            showMessage("Rendez-vous supprime", "green");
            resetForm();
            refreshDispos();
            refreshTable();
        } else {
            showMessage("Suppression impossible", "red");
        }
    }

    @FXML
    public void resetForm() {
        selectedRendezVous = null;
        txtAge.clear();
        txtAdresse.clear();
        comboTypes.getSelectionModel().clearSelection();
        comboDispos.getSelectionModel().clearSelection();
        tableRendezVous.getSelectionModel().clearSelection();
        errorGlobal.setText("");
        refreshDispos();
        updateButtons();
    }

    @FXML
    public void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/patient_dashboard.fxml"));
            BorderPane mainContainer = (BorderPane) txtAge.getScene().lookup("#mainContainer");
            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                txtAge.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.out.println("Erreur retour dashboard: " + e.getMessage());
        }
    }

    private void configureTable() {
        colAge.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getAge())));
        colAdresse.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getAdresse()));
        colType.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTypeLibelle()));
        colDate.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDateDisponibilite()));
        colHeure.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getHeureDebut() + " - " + data.getValue().getHeureFin()));
    }

    private void populateForm(RendezVous rendezVous) {
        if (rendezVous == null) {
            return;
        }

        txtAge.setText(String.valueOf(rendezVous.getAge()));
        txtAdresse.setText(rendezVous.getAdresse());
        comboTypes.getSelectionModel().select(findTypeById(rendezVous.getTypeId()));
        comboDispos.setItems(FXCollections.observableArrayList(sd.getDisposLibresIncluding(rendezVous.getDispoId())));
        comboDispos.getSelectionModel().select(findDispoById(rendezVous.getDispoId()));
    }

    private TypeRendezVous findTypeById(int typeId) {
        return comboTypes.getItems().stream().filter(type -> type.getId() == typeId).findFirst().orElse(null);
    }

    private Disponibilite findDispoById(int dispoId) {
        return comboDispos.getItems().stream().filter(dispo -> dispo.getId() == dispoId).findFirst().orElse(null);
    }

    private Integer parseAge() {
        if (txtAge.getText().isBlank() || txtAdresse.getText().isBlank()) {
            showMessage("Champs obligatoires", "red");
            return null;
        }

        try {
            int age = Integer.parseInt(txtAge.getText().trim());
            if (age < 5 || age > 120) {
                showMessage("Age doit etre entre 5 et 120", "red");
                return null;
            }
            if (!ADDRESS_PATTERN.matcher(txtAdresse.getText().trim()).matches()) {
                showMessage("Adresse invalide ou trop courte", "red");
                return null;
            }
            return age;
        } catch (Exception e) {
            showMessage("Age invalide", "red");
            return null;
        }
    }

    private void refreshTable() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        if (currentUser != null) {
            tableRendezVous.setItems(FXCollections.observableArrayList(srv.getRendezVousByUser(currentUser.getId())));
        }
    }

    private void refreshDispos() {
        comboDispos.setItems(FXCollections.observableArrayList(sd.getDisposLibres()));
    }

    private void updateButtons() {
        if (btnValider != null) {
            btnValider.setDisable(false);
        }
        if (btnModifier != null) {
            btnModifier.setDisable(false);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setDisable(false);
        }
    }

    private void loadWelcomeLabel() {
        try {
            int userId = getCurrentUserId();
            Connection cnx = DataSource.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement("SELECT nom, prenom FROM user WHERE id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                welcomeLabel.setText("Rendez-vous patient");
                if (patientNomLabel != null) {
                    patientNomLabel.setText("Nom : " + nom);
                }
                if (patientPrenomLabel != null) {
                    patientPrenomLabel.setText("Prenom : " + prenom);
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur user: " + e.getMessage());
        }
    }

    private int getCurrentUserId() {
        if (currentUser == null) {
            currentUser = UserSession.getInstance();
        }
        return currentUser.getId();
    }

    private void showMessage(String msg, String color) {
        errorGlobal.setText(msg);
        errorGlobal.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}
