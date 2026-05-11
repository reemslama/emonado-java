package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.entities.Jeu;
import org.example.service.AnalyseService;

import java.util.HashMap;
import java.util.Map;

public class GameController {

    @FXML
    private ImageView choix1;

    @FXML
    private ImageView choix2;

    @FXML
    private ImageView choix3;

    private Map<String, String> reponses = new HashMap<>();

    public void initialize() {

        choix1.setImage(new Image("images/chien.png"));
        choix2.setImage(new Image("images/chat.png"));
        choix3.setImage(new Image("images/vache.png"));

        choix1.setOnMouseClicked(e -> choisir("chien"));
        choix2.setOnMouseClicked(e -> choisir("chat"));
        choix3.setOnMouseClicked(e -> choisir("vache"));
    }

    private void choisir(String choix) {
        reponses.put("q1", choix);

        String etat = AnalyseService.analyserEtat(reponses);
        String conseil = AnalyseService.genererConseil(etat);

        System.out.println("Etat: " + etat);
        System.out.println("Conseil: " + conseil);
    }
}