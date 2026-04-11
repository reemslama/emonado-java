package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MedicalRecordController {

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
        loadConsultationStats();
    }

    @FXML
    private void handleSaveReminder() {
        if (currentUser == null) {
            return;
        }
        String reminderText = reminderArea.getText() == null ? "" : reminderArea.getText().trim();
        String historyText = medicalHistoryArea.getText() == null ? "" : medicalHistoryArea.getText().trim();
        saveOrUpdateMedicalRecord(reminderText, historyText);
    }

    @FXML
    private void handleSaveMedicalHistory() {
        if (currentUser == null) {
            return;
        }
        String reminderText = reminderArea.getText() == null ? "" : reminderArea.getText().trim();
        String historyText = medicalHistoryArea.getText() == null ? "" : medicalHistoryArea.getText().trim();
        saveOrUpdateMedicalRecord(reminderText, historyText);
    }

    @FXML
    private void handleSavePersonalInfo() {
        if (currentUser == null) {
            return;
        }

        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        currentUser.setEmail(emailField.getText().trim());
        currentUser.setTelephone(phoneField.getText().trim());
        currentUser.setSexe(sexeField.getText().trim());
        currentUser.setDateNaissance(birthDatePicker.getValue());

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
        String query = "SELECT reminder_text, medical_history, created_at FROM patient_medical_record WHERE patient_id = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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
        String query = "INSERT INTO patient_medical_record (patient_id, reminder_text, medical_history) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE reminder_text = VALUES(reminder_text), medical_history = VALUES(medical_history), updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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

    private void loadConsultationStats() {
        String query = "SELECT consultation_date FROM patient_consultation WHERE patient_id = ? ORDER BY consultation_date DESC, id DESC";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int count = 0;
        LocalDate lastDate = null;

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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
        String createMedicalRecordTable = "CREATE TABLE IF NOT EXISTS patient_medical_record (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "patient_id INT NOT NULL UNIQUE, " +
                "reminder_text TEXT, " +
                "medical_history TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_patient_medical_record_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";

        String createConsultationTable = "CREATE TABLE IF NOT EXISTS patient_consultation (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "patient_id INT NOT NULL, " +
                "consultation_date DATE NOT NULL, " +
                "notes TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_patient_consultation_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";

        String createAntecedentTable = "CREATE TABLE IF NOT EXISTS antecedent_medical (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "dossier_medical_id INT NOT NULL, " +
                "type VARCHAR(100) NOT NULL, " +
                "description TEXT NOT NULL, " +
                "date_diagnostic DATE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_antecedent_medical_record FOREIGN KEY (dossier_medical_id) REFERENCES patient_medical_record(id) ON DELETE CASCADE" +
                ")";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement createRecordStmt = conn.prepareStatement(createMedicalRecordTable);
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

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
