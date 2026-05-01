package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.entities.User;
import org.example.service.UserService;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class AdminTableController {

    @FXML private TableView<User> userTable;

    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colSpecialite;
    @FXML private TableColumn<User, String> colSexe;

    @FXML private TableColumn<User, LocalDate> colDateNaissance;

    @FXML private TableColumn<User, Void> colActions;

    private String currentRole;

    // ===================== INIT =====================
    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colSexe.setCellValueFactory(new PropertyValueFactory<>("sexe"));
        colDateNaissance.setCellValueFactory(new PropertyValueFactory<>("date_naissance"));
        colSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));

        addActionsToTable();
    }

    // ===================== LOAD DATA =====================
    public void loadData(String role) {
        this.currentRole = role;

        userTable.setItems(UserService.getByRole(role));

        if (colSpecialite != null) {
            colSpecialite.setVisible(role.equalsIgnoreCase("ROLE_PSYCHOLOGUE"));
        }
    }

    // ===================== ADD USER =====================
    @FXML
    private void handleAddUser() {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/admin_add_user.fxml"));

            javafx.scene.Parent root = loader.load();

            AdminAddUserController controller = loader.getController();
            controller.setDefaultRole(currentRole);

            javafx.scene.layout.StackPane contentArea =
                    (javafx.scene.layout.StackPane) userTable.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== ACTIONS COLUMN (DELETE ONLY) =====================
    private void addActionsToTable() {

        colActions.setCellFactory(param -> new TableCell<>() {

            private final Button btnDelete = new Button("Supprimer");
            private final HBox pane = new HBox(btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                pane.setStyle("-fx-alignment: CENTER; -fx-spacing: 10;");

                btnDelete.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    // ===================== DELETE USER =====================
    private void handleDelete(User user) {

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer " + user.getNom() + " ?",
                ButtonType.YES,
                ButtonType.NO
        );

        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {

            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt =
                         conn.prepareStatement("DELETE FROM user WHERE id=?")) {

                pstmt.setInt(1, user.getId());
                pstmt.executeUpdate();

                loadData(currentRole);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
