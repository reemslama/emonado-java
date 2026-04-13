package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.entities.AntecedentMedical;
import org.example.entities.User;
import org.example.service.UserService;
import org.example.utils.DataSource;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MedicalRecordController {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ' -]{2,50}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern INVALID_TEXT_PATTERN = Pattern.compile("^[\\p{Punct}\\s]+$");
    private static final int REMINDER_MAX_LENGTH = 500;
    private static final int HISTORY_MAX_LENGTH = 3000;
    private static final int ANTECEDENT_TYPE_MAX_LENGTH = 100;
    private static final int ANTECEDENT_DESCRIPTION_MAX_LENGTH = 1000;

    @FXML private Label titleLabel;
    @FXML private Label consultationCountLabel;
    @FXML private Label dossierCreationLabel;
    @FXML private Label lastConsultationLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField sexeField;
    @FXML private DatePicker birthDatePicker;

    @FXML private TextArea reminderArea;
    @FXML private TextArea medicalHistoryArea;
    @FXML private TextField antecedentTypeField;
    @FXML private TextArea antecedentDescriptionArea;
    @FXML private DatePicker antecedentDatePicker;
    @FXML private ListView<String> antecedentsListView;

    private User currentUser;

    @FXML
    public void initialize() {
        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        fillPersonalInfo();
        ensureTables();
        loadMedicalRecord();
        loadAntecedents();
        loadConsultationStats();
    }

    @FXML
    private void handleSaveReminder() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        String reminderText = reminderArea.getText() == null ? "" : reminderArea.getText().trim();
        String historyText = medicalHistoryArea.getText() == null ? "" : medicalHistoryArea.getText().trim();
        String validationError = validateMedicalRecordTexts(reminderText, historyText, true);
        if (validationError != null) {
            showError(validationError);
            return;
        }
        saveOrUpdateMedicalRecord(reminderText, historyText);
    }

    @FXML
    private void handleSaveMedicalHistory() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        String reminderText = reminderArea.getText() == null ? "" : reminderArea.getText().trim();
        String historyText = medicalHistoryArea.getText() == null ? "" : medicalHistoryArea.getText().trim();
        String validationError = validateMedicalRecordTexts(reminderText, historyText, false);
        if (validationError != null) {
            showError(validationError);
            return;
        }
        saveOrUpdateMedicalRecord(reminderText, historyText);
    }

    @FXML
    private void handleAddAntecedent() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        String type = antecedentTypeField.getText() == null ? "" : antecedentTypeField.getText().trim();
        String description = antecedentDescriptionArea.getText() == null ? "" : antecedentDescriptionArea.getText().trim();
        LocalDate dateDiagnostic = antecedentDatePicker.getValue();
        String validationError = validateAntecedent(type, description, dateDiagnostic);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        int dossierMedicalId = ensureMedicalRecordExists();
        if (dossierMedicalId <= 0) {
            showError("Impossible de rattacher l'antecedent au dossier medical.");
            return;
        }

        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return;
        }

        String query = "INSERT INTO antecedent_medical (dossier_medical_id, type, description, date_diagnostic) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, dossierMedicalId);
            pstmt.setString(2, type);
            pstmt.setString(3, description);
            pstmt.setDate(4, dateDiagnostic != null ? Date.valueOf(dateDiagnostic) : null);
            pstmt.executeUpdate();

            antecedentTypeField.clear();
            antecedentDescriptionArea.clear();
            antecedentDatePicker.setValue(null);
            loadAntecedents();
            showInfo("Antecedent medical ajoute avec succes.");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ajout de l'antecedent medical.");
        }
    }

    @FXML
    private void handleSavePersonalInfo() {
        if (currentUser == null) {
            showError("Session utilisateur introuvable.");
            return;
        }

        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String prenom = prenomField.getText() == null ? "" : prenomField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim().replaceAll("\\D", "");
        String sexe = sexeField.getText() == null ? "" : sexeField.getText().trim();
        LocalDate birthDate = birthDatePicker.getValue();
        String validationError = validatePersonalInfo(nom, prenom, email, phone, sexe, birthDate);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        currentUser.setTelephone(phone);
        currentUser.setSexe(sexe);
        currentUser.setDateNaissance(birthDate);

        try {
            UserService.updatePatientProfile(currentUser);
            UserSession.setInstance(currentUser);
            showInfo("Informations personnelles mises a jour.");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de mettre a jour vos informations personnelles.");
        }
    }

    @FXML
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent view = loader.load();
            PatientDashboardController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToConsultations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/consultations.fxml"));
            Parent view = loader.load();
            ConsultationController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil_patient.fxml"));
            Parent view = loader.load();
            ProfilPatientController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent view = FXMLLoader.load(getClass().getResource("/login.fxml"));
            titleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillPersonalInfo() {
        titleLabel.setText("Mon Dossier Medical");
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getTelephone());
        sexeField.setText(currentUser.getSexe() == null ? "" : currentUser.getSexe());
        birthDatePicker.setValue(currentUser.getDateNaissance());
    }

    private void loadMedicalRecord() {
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return;
        }

        String query = "SELECT reminder_text, medical_history, created_at FROM patient_medical_record WHERE patient_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    reminderArea.setText(rs.getString("reminder_text"));
                    medicalHistoryArea.setText(rs.getString("medical_history"));
                    Date createdAt = rs.getDate("created_at");
                    dossierCreationLabel.setText(createdAt != null ? createdAt.toString() : "-");
                } else {
                    reminderArea.clear();
                    medicalHistoryArea.clear();
                    dossierCreationLabel.setText("-");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de charger le dossier medical.");
        }
    }

    private void saveOrUpdateMedicalRecord(String reminderText, String historyText) {
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return;
        }

        String query = "INSERT INTO patient_medical_record (patient_id, reminder_text, medical_history) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE reminder_text = VALUES(reminder_text), medical_history = VALUES(medical_history), updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            pstmt.setString(2, reminderText);
            pstmt.setString(3, historyText);
            pstmt.executeUpdate();
            showInfo("Dossier medical enregistre.");
            loadMedicalRecord();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de l'enregistrement du dossier medical.");
        }
    }

    private void loadAntecedents() {
        if (antecedentsListView == null) {
            return;
        }

        antecedentsListView.getItems().clear();
        List<AntecedentMedical> antecedents = findAntecedents();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (antecedents.isEmpty()) {
            antecedentsListView.getItems().add("Aucun antecedent medical enregistre.");
            return;
        }

        for (AntecedentMedical antecedent : antecedents) {
            String datePart = antecedent.getDateDiagnostic() == null
                    ? "Date non precisee"
                    : antecedent.getDateDiagnostic().format(formatter);
            antecedentsListView.getItems().add(
                    antecedent.getType() + " | " + datePart + System.lineSeparator() + antecedent.getDescription()
            );
        }
    }

    private List<AntecedentMedical> findAntecedents() {
        List<AntecedentMedical> antecedents = new ArrayList<>();
        int dossierMedicalId = findMedicalRecordId();
        if (dossierMedicalId <= 0) {
            return antecedents;
        }

        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return antecedents;
        }

        String query = "SELECT id, type, description, date_diagnostic FROM antecedent_medical WHERE dossier_medical_id = ? ORDER BY date_diagnostic DESC, id DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, dossierMedicalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AntecedentMedical antecedent = new AntecedentMedical();
                    antecedent.setId(rs.getInt("id"));
                    antecedent.setDossierMedicalId(dossierMedicalId);
                    antecedent.setType(rs.getString("type"));
                    antecedent.setDescription(rs.getString("description"));
                    Date dateDiagnostic = rs.getDate("date_diagnostic");
                    if (dateDiagnostic != null) {
                        antecedent.setDateDiagnostic(dateDiagnostic.toLocalDate());
                    }
                    antecedents.add(antecedent);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de charger les antecedents medicaux.");
        }
        return antecedents;
    }

    private int ensureMedicalRecordExists() {
        int existingId = findMedicalRecordId();
        if (existingId > 0) {
            return existingId;
        }

        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return -1;
        }

        String query = "INSERT INTO patient_medical_record (patient_id, reminder_text, medical_history) VALUES (?, '', '')";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, currentUser.getId());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int dossierId = generatedKeys.getInt(1);
                    dossierCreationLabel.setText(LocalDate.now().toString());
                    return dossierId;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de creer le dossier medical.");
        }
        return findMedicalRecordId();
    }

    private int findMedicalRecordId() {
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return -1;
        }

        String query = "SELECT id FROM patient_medical_record WHERE patient_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible d'identifier le dossier medical.");
        }
        return -1;
    }

    private void loadConsultationStats() {
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return;
        }

        String query = "SELECT consultation_date FROM patient_consultation WHERE patient_id = ? ORDER BY consultation_date DESC, id DESC";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int count = 0;
        LocalDate lastDate = null;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    count++;
                    LocalDate consultationDate = rs.getDate("consultation_date").toLocalDate();
                    if (lastDate == null) {
                        lastDate = consultationDate;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de charger les consultations.");
        }

        consultationCountLabel.setText(String.valueOf(count));
        lastConsultationLabel.setText(lastDate == null ? "-" : lastDate.format(formatter));
    }

    private void ensureTables() {
        Connection conn = DataSource.getInstance().getConnection();
        if (conn == null) {
            showError("Connexion a la base de donnees indisponible.");
            return;
        }

        String createMedicalRecordTable = "CREATE TABLE IF NOT EXISTS patient_medical_record ("
                + "id INT PRIMARY KEY AUTO_INCREMENT, "
                + "patient_id INT NOT NULL UNIQUE, "
                + "reminder_text TEXT, "
                + "medical_history TEXT, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_patient_medical_record_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE"
                + ")";

        String createConsultationTable = "CREATE TABLE IF NOT EXISTS patient_consultation ("
                + "id INT PRIMARY KEY AUTO_INCREMENT, "
                + "patient_id INT NOT NULL, "
                + "consultation_date DATE NOT NULL, "
                + "notes TEXT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_patient_consultation_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE"
                + ")";

        String createAntecedentTable = "CREATE TABLE IF NOT EXISTS antecedent_medical ("
                + "id INT PRIMARY KEY AUTO_INCREMENT, "
                + "dossier_medical_id INT NOT NULL, "
                + "type VARCHAR(100) NOT NULL, "
                + "description TEXT NOT NULL, "
                + "date_diagnostic DATE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_antecedent_medical_record FOREIGN KEY (dossier_medical_id) REFERENCES patient_medical_record(id) ON DELETE CASCADE"
                + ")";

        try (PreparedStatement createRecordStmt = conn.prepareStatement(createMedicalRecordTable);
             PreparedStatement createConsultStmt = conn.prepareStatement(createConsultationTable);
             PreparedStatement createAntecedentStmt = conn.prepareStatement(createAntecedentTable)) {
            createRecordStmt.executeUpdate();
            createConsultStmt.executeUpdate();
            createAntecedentStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible d'initialiser les tables du dossier medical.");
        }
    }

    private String validatePersonalInfo(String nom, String prenom, String email, String phone, String sexe, LocalDate birthDate) {
        if (!NAME_PATTERN.matcher(nom).matches()) {
            return "Le nom doit contenir entre 2 et 50 caracteres alphabetiques.";
        }
        if (!NAME_PATTERN.matcher(prenom).matches()) {
            return "Le prenom doit contenir entre 2 et 50 caracteres alphabetiques.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Le format de l'email est invalide.";
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "Le telephone doit contenir entre 8 et 15 chiffres.";
        }
        if (!sexe.isBlank() && !sexe.equalsIgnoreCase("Homme") && !sexe.equalsIgnoreCase("Femme")) {
            return "Le sexe doit etre 'Homme' ou 'Femme'.";
        }
        if (birthDate == null) {
            return "Veuillez saisir une date de naissance.";
        }
        if (birthDate.isAfter(LocalDate.now())) {
            return "La date de naissance ne peut pas etre dans le futur.";
        }
        if (birthDate.isAfter(LocalDate.now().minusYears(5))) {
            return "La date de naissance semble invalide.";
        }
        return null;
    }

    private String validateMedicalRecordTexts(String reminderText, String historyText, boolean reminderAction) {
        if (reminderAction) {
            if (reminderText.isBlank()) {
                return "Veuillez saisir un rappel avant de l'enregistrer.";
            }
            if (reminderText.length() < 5) {
                return "Le rappel doit contenir au moins 5 caracteres.";
            }
            if (reminderText.length() > REMINDER_MAX_LENGTH) {
                return "Le rappel ne doit pas depasser 500 caracteres.";
            }
            if (INVALID_TEXT_PATTERN.matcher(reminderText).matches()) {
                return "Le rappel doit contenir du texte explicite.";
            }
        } else {
            if (historyText.isBlank()) {
                return "Veuillez saisir un historique medical avant de l'enregistrer.";
            }
            if (historyText.length() < 10) {
                return "L'historique medical doit contenir au moins 10 caracteres.";
            }
            if (historyText.length() > HISTORY_MAX_LENGTH) {
                return "L'historique medical ne doit pas depasser 3000 caracteres.";
            }
            if (INVALID_TEXT_PATTERN.matcher(historyText).matches()) {
                return "L'historique medical doit contenir du texte explicite.";
            }
        }
        if (historyText.length() > HISTORY_MAX_LENGTH) {
            return "L'historique medical ne doit pas depasser 3000 caracteres.";
        }
        return null;
    }

    private String validateAntecedent(String type, String description, LocalDate dateDiagnostic) {
        if (type.isBlank()) {
            return "Veuillez saisir le type de l'antecedent medical.";
        }
        if (type.length() > ANTECEDENT_TYPE_MAX_LENGTH) {
            return "Le type de l'antecedent ne doit pas depasser 100 caracteres.";
        }
        if (INVALID_TEXT_PATTERN.matcher(type).matches()) {
            return "Le type de l'antecedent doit contenir des lettres ou des chiffres.";
        }
        if (description.length() < 5) {
            return "Veuillez saisir une description plus precise pour l'antecedent.";
        }
        if (description.length() > ANTECEDENT_DESCRIPTION_MAX_LENGTH) {
            return "La description de l'antecedent ne doit pas depasser 1000 caracteres.";
        }
        if (INVALID_TEXT_PATTERN.matcher(description).matches()) {
            return "La description de l'antecedent doit contenir du texte explicite.";
        }
        if (dateDiagnostic != null && dateDiagnostic.isAfter(LocalDate.now())) {
            return "La date du diagnostic ne peut pas etre dans le futur.";
        }
        return null;
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
