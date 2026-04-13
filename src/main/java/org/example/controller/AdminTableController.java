package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.example.entities.User;
import org.example.service.UserService;
import org.example.utils.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class AdminTableController {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom, colPrenom, colEmail, colPhone, colSpecialite, colSexe;
    @FXML private TableColumn<User, LocalDate> colDateNaissance;
    @FXML private TableColumn<User, Void> colActions;

    private String currentRole;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colSexe.setCellValueFactory(new PropertyValueFactory<>("sexe"));
        colDateNaissance.setCellValueFactory(new PropertyValueFactory<>("dateNaissance"));
        colSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));
        addActionsToTable();
    }

    public void loadData(String role) {
        this.currentRole = role;
        userTable.setItems(UserService.getByRole(role));
        if (colSpecialite != null) {
            colSpecialite.setVisible(role.equalsIgnoreCase("ROLE_PSYCHOLOGUE"));
        }
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin_add_user.fxml"));
            Parent root = loader.load();

            AdminAddUserController controller = loader.getController();
            controller.setDefaultRole(currentRole);

            StackPane contentArea = (StackPane) userTable.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addActionsToTable() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: #ffc107;");
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                btnEdit.setOnAction(event -> openEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void openEditDialog(User user) {
        try {
            String fxml = user.getRole().equalsIgnoreCase("ROLE_PATIENT") ? "/profil_patient.fxml" : "/profil_psy.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (user.getRole().equalsIgnoreCase("ROLE_PATIENT")) {
                ProfilPatientController ctrl = loader.getController();
                ctrl.setUserData(user);
            } else {
                ProfilPsyController ctrl = loader.getController();
                ctrl.setUserData(user);
            }

            StackPane contentArea = (StackPane) userTable.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleDelete(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + user.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE id=?")) {
                pstmt.setInt(1, user.getId());
                pstmt.executeUpdate();
                loadData(currentRole);
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
