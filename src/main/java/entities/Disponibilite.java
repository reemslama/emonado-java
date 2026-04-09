package entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Disponibilite {
    private int id;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private boolean estLibre;

    public Disponibilite() {
        this.estLibre = true; // Par défaut, un créneau créé est libre
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public boolean isEstLibre() { return estLibre; }
    public void setEstLibre(boolean estLibre) { this.estLibre = estLibre; }
}