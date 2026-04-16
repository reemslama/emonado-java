package org.example.controller;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.AntecedentMedical;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

final class AntecedentCardFactory {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AntecedentCardFactory() {
    }

    static VBox build(AntecedentMedical antecedentMedical,
                      Consumer<AntecedentMedical> onEdit,
                      Consumer<AntecedentMedical> onDelete) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #fff5f5; -fx-background-radius: 10; "
                + "-fx-border-color: #f5b5be; -fx-border-radius: 10;");

        Label title = new Label(antecedentMedical.getType());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #7f1d1d;");

        String dateText = antecedentMedical.getDateDiagnostic() == null
                ? "Date non precisee"
                : antecedentMedical.getDateDiagnostic().format(DATE_FORMATTER);
        Label date = new Label(dateText);
        date.setStyle("-fx-text-fill: #991b1b;");

        Label description = new Label(antecedentMedical.getDescription());
        description.setWrapText(true);

        Button editButton = new Button("Modifier");
        editButton.setStyle("-fx-background-color: #0d6efd; -fx-text-fill: white;");
        editButton.setOnAction(event -> onEdit.accept(antecedentMedical));

        Button deleteButton = new Button("Supprimer");
        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        deleteButton.setOnAction(event -> onDelete.accept(antecedentMedical));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, editButton, deleteButton, spacer);

        card.getChildren().addAll(title, date, description, actions);
        return card;
    }
}
