package org.example;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

public class FxLoadProbe {
    public static void main(String[] args) {
        Platform.startup(() -> {
            probe("/AjouterRendezVous.fxml");
            probe("/medical_record.fxml");
            probe("/consultations.fxml");
            Platform.exit();
        });
    }

    private static void probe(String resource) {
        try {
            FXMLLoader loader = new FXMLLoader(FxLoadProbe.class.getResource(resource));
            loader.load();
            System.out.println("OK " + resource);
        } catch (Exception e) {
            System.out.println("FAIL " + resource);
            e.printStackTrace();
        }
    }
}
