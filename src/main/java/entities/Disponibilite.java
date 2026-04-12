package entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Disponibilite {

    private int id;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;

    // ✔ CONSTRUCTEUR VIDE (obligatoire)
    public Disponibilite() {
    }

    // ✔ CONSTRUCTEUR AVEC PARAMÈTRES
    public Disponibilite(int id, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        this.id = id;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    // ✔ GETTERS & SETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    // 🔥 IMPORTANT POUR AFFICHAGE DANS ComboBox
    @Override
    public String toString() {
        return date + " | " + heureDebut + " - " + heureFin;
    }
}