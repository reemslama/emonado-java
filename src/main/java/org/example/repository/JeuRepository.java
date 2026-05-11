package org.example.repository;

import org.example.entities.Jeu;

import java.util.ArrayList;
import java.util.List;

public class JeuRepository {

    public static List<Jeu> getJeux() {

        List<Jeu> jeux = new ArrayList<>();

        jeux.add(new Jeu(
                "1",
                "Reconnaître le chien",
                "SON",
                "sounds/dog.mp3",
                List.of(
                        "images/chien.png",
                        "images/chat.png",
                        "images/vache.png"
                ),
                5,
                10
        ));

        return jeux;
    }
}