package controllers;

import entities.TypeRendezVous;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import services.ServiceTypeRendezVous;

public class AjouterTypeController {

    @FXML
    private TextField txtLibelle;

    @FXML
    private Label errorLibelle; // Correspond au fx:id du nouveau Label

    private ServiceTypeRendezVous st = new ServiceTypeRendezVous();

    @FXML
    void ajouterType() {
        String libelle = txtLibelle.getText();

        // On réinitialise l'erreur et le style à chaque clic
        errorLibelle.setText("");
        txtLibelle.setStyle("");

        if (libelle.trim().isEmpty()) {
            // Affichage style "Symfony"
            errorLibelle.setText("Ce champ est obligatoire.");
            txtLibelle.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else {
            // Si c'est bon
            TypeRendezVous t = new TypeRendezVous();
            t.setLibelle(libelle);
            st.ajouter(t);

            txtLibelle.clear();
            txtLibelle.setStyle("-fx-border-color: green;");
            System.out.println("Succès : Type ajouté !");
        }
    }
}