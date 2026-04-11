package org.example.controllers;

import entities.RendezVousPsy;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ServiceRendezVous;

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

    private void loadData() {
        tableRdv.setItems(
                FXCollections.observableArrayList(
                        srv.getRendezVousForPsy()
                )
        );
    }
}