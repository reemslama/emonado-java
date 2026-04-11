package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.User;
import org.example.utils.DataSource;
import org.example.utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConsultationController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");

    @FXML private Label pageTitleLabel;
    @FXML private Label historyCountLabel;
    @FXML private DatePicker consultationDatePicker;
    @FXML private TextArea consultationNotesArea;
    @FXML private VBox consultationHistoryBox;
    @FXML private MenuButton filterMenuButton;
    @FXML private ToggleButton writeModeButton;
    @FXML private ToggleButton speakModeButton;

    private User currentUser;
    private FilterMode currentFilter = FilterMode.ALL;

    @FXML
    public void initialize() {
        ToggleGroup modeGroup = new ToggleGroup();
        writeModeButton.setToggleGroup(modeGroup);
        speakModeButton.setToggleGroup(modeGroup);
        writeModeButton.setSelected(true);
        consultationDatePicker.setValue(LocalDate.now());

        if (currentUser == null) {
            setUserData(UserSession.getInstance());
        }
    }

    public void setUserData(User user) {
        if (user == null) {
            return;
        }
        currentUser = user;
        pageTitleLabel.setText("Mes Consultations");
        consultationDatePicker.setValue(LocalDate.now());
        ensureConsultationTable();
        loadConsultations();
    }

    @FXML
    private void handleAddConsultation() {
        if (currentUser == null) {
            return;
        }

        LocalDate consultationDate = consultationDatePicker.getValue();
        String notes = consultationNotesArea.getText() == null ? "" : consultationNotesArea.getText().trim();

        if (consultationDate == null) {
            showError("Veuillez choisir une date de consultation.");
            return;
        }
        if (consultationDate.isBefore(LocalDate.now())) {
            showError("La date doit etre aujourd'hui ou dans le futur.");
            return;
        }
        if (notes.length() < 10) {
            showError("Le compte rendu doit contenir au moins 10 caracteres.");
            return;
        }

        String query = "INSERT INTO patient_consultation (patient_id, consultation_date, notes) VALUES (?, ?, ?)";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            pstmt.setDate(2, Date.valueOf(consultationDate));
            pstmt.setString(3, notes);
            pstmt.executeUpdate();

            consultationNotesArea.clear();
            consultationDatePicker.setValue(LocalDate.now());
            loadConsultations();
            showInfo("Consultation enregistree avec succes.");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de l'enregistrement de la consultation.");
        }
    }

    @FXML
    private void activateWriteMode() {
        writeModeButton.setSelected(true);
    }

    @FXML
    private void activateSpeechMode() {
        speakModeButton.setSelected(false);
        writeModeButton.setSelected(true);
        showInfo("La saisie vocale n'est pas encore disponible. Utilisez la saisie ecrite.");
    }

    @FXML
    private void showAllConsultations() {
        currentFilter = FilterMode.ALL;
        filterMenuButton.setText("Filtrer : toutes");
        loadConsultations();
    }

    @FXML
    private void showUpcomingConsultations() {
        currentFilter = FilterMode.UPCOMING;
        filterMenuButton.setText("Filtrer : a venir");
        loadConsultations();
    }

    @FXML
    private void showPastConsultations() {
        currentFilter = FilterMode.PAST;
        filterMenuButton.setText("Filtrer : passees");
        loadConsultations();
    }

    @FXML
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patient_dashboard.fxml"));
            Parent view = loader.load();
            PatientDashboardController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            pageTitleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToMedicalRecord() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/medical_record.fxml"));
            Parent view = loader.load();
            MedicalRecordController controller = loader.getController();
            controller.setUserData(UserSession.getInstance());
            pageTitleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.setInstance(null);
            Parent view = FXMLLoader.load(getClass().getResource("/login.fxml"));
            pageTitleLabel.getScene().setRoot(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConsultations() {
        consultationHistoryBox.getChildren().clear();

        List<ConsultationItem> items = new ArrayList<>();
        String query = "SELECT consultation_date, notes, created_at FROM patient_consultation " +
                "WHERE patient_id = ? ORDER BY consultation_date DESC, id DESC";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ConsultationItem(
                            rs.getDate("consultation_date").toLocalDate(),
                            rs.getString("notes"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible de charger les consultations.");
            return;
        }

        List<ConsultationItem> filteredItems = items.stream()
                .filter(this::matchesFilter)
                .toList();

        historyCountLabel.setText(String.valueOf(filteredItems.size()));

        if (filteredItems.isEmpty()) {
            Label emptyLabel = new Label("Aucune consultation a afficher pour ce filtre.");
            emptyLabel.setWrapText(true);
            emptyLabel.setStyle("-fx-text-fill: #5f6c7b; -fx-font-size: 14px; -fx-padding: 18;");
            consultationHistoryBox.getChildren().add(emptyLabel);
            return;
        }

        for (ConsultationItem item : filteredItems) {
            consultationHistoryBox.getChildren().add(buildConsultationCard(item));
        }
    }

    private boolean matchesFilter(ConsultationItem item) {
        return switch (currentFilter) {
            case ALL -> true;
            case UPCOMING -> !item.consultationDate().isBefore(LocalDate.now());
            case PAST -> item.consultationDate().isBefore(LocalDate.now());
        };
    }

    private VBox buildConsultationCard(ConsultationItem item) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #35a853; -fx-border-width: 0 0 0 4; -fx-border-radius: 12;");

        Label titleLabel = new Label("Consultation du " + item.consultationDate().format(DATE_FORMATTER));
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateBadge = new Label(item.consultationDate().format(DATE_FORMATTER));
        dateBadge.setStyle("-fx-background-color: #198754; -fx-background-radius: 8; "
                + "-fx-text-fill: white; -fx-padding: 6 12; -fx-font-weight: bold;");

        HBox titleRow = new HBox(10, titleLabel, spacer, dateBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        boolean pendingFollowUp = !item.consultationDate().isBefore(LocalDate.now());
        Label statusBadge = new Label(pendingFollowUp
                ? "Consultation personnelle (en attente de suivi)"
                : "Consultation personnelle (terminee)");
        statusBadge.setStyle(pendingFollowUp
                ? "-fx-background-color: #ffc107; -fx-background-radius: 8; -fx-text-fill: #1b1b1b; "
                + "-fx-padding: 5 10; -fx-font-weight: bold;"
                : "-fx-background-color: #dbeafe; -fx-background-radius: 8; -fx-text-fill: #1d4ed8; "
                + "-fx-padding: 5 10; -fx-font-weight: bold;");

        Label notesTitle = new Label("Compte rendu :");
        notesTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label notesLabel = new Label(item.notes());
        notesLabel.setWrapText(true);
        notesLabel.setStyle("-fx-background-color: #f5f8fc; -fx-background-radius: 10; "
                + "-fx-padding: 14; -fx-text-fill: #334155; -fx-font-size: 14px;");
        notesLabel.setMaxWidth(Double.MAX_VALUE);

        String createdAtText = item.createdAt() == null
                ? "-"
                : item.createdAt().toLocalDateTime().format(DATE_TIME_FORMATTER);
        Label createdAtLabel = new Label("Consultation enregistree le " + createdAtText);
        createdAtLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        card.getChildren().addAll(titleRow, statusBadge, notesTitle, notesLabel, createdAtLabel);
        return card;
    }

    private void ensureConsultationTable() {
        String createConsultationTable = "CREATE TABLE IF NOT EXISTS patient_consultation (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "patient_id INT NOT NULL, " +
                "consultation_date DATE NOT NULL, " +
                "notes TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_patient_consultation_user FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(createConsultationTable)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Impossible d'initialiser la table des consultations.");
        }
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }

    private enum FilterMode {
        ALL,
        UPCOMING,
        PAST
    }

    private record ConsultationItem(LocalDate consultationDate, String notes, Timestamp createdAt) {
    }
}
