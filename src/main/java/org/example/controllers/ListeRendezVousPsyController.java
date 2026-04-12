package org.example.controllers;

import entities.RendezVousPsy;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import services.ServiceRendezVous;
import java.io.IOException;

public class ListeRendezVousPsyController {

    @FXML private TableView<RendezVousPsy> tableRdv;

    @FXML private TableColumn<RendezVousPsy, String> colNom;
    @FXML private TableColumn<RendezVousPsy, String> colPrenom;
    @FXML private TableColumn<RendezVousPsy, String> colType;
    @FXML private TableColumn<RendezVousPsy, String> colDate;
    @FXML private TableColumn<RendezVousPsy, String> colHeure;

    ServiceRendezVous srv = new ServiceRendezVous();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNom()));
        colPrenom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getPrenom()));
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTypeRdv()));
        colDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDate()));

        colHeure.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getHeureDebut() + " - " + d.getValue().getHeureFin()
                )
        );

        loadData();
    }

    /**
     * Méthode pour retourner au Dashboard du Psychologue
     */
    @FXML
    private void returnToDashboard() {
        try {
            // Chargement du dashboard spécifique au Psy
            Parent root = FXMLLoader.load(getClass().getResource("/psy_dashboard.fxml"));

            // Récupération du conteneur principal (BorderPane)
            BorderPane mainContainer = (BorderPane) tableRdv.getScene().lookup("#mainContainer");

            if (mainContainer != null) {
                mainContainer.setCenter(root);
            } else {
                // Fallback si le mainContainer n'est pas trouvé
                tableRdv.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard Psy : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadData() {
        // Chargement des données depuis le service
        tableRdv.setItems(
                FXCollections.observableArrayList(
                        srv.getRendezVousForPsy()
                )
        );
    }
}