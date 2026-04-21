package entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Disponibilite {

    private int id;
    private int psychologueId;
    private String psychologueNomComplet;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private boolean libre;

    public Disponibilite() {
    }

    public Disponibilite(int id, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        this.id = id;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPsychologueId() {
        return psychologueId;
    }

    public void setPsychologueId(int psychologueId) {
        this.psychologueId = psychologueId;
    }

    public String getPsychologueNomComplet() {
        return psychologueNomComplet;
    }

    public void setPsychologueNomComplet(String psychologueNomComplet) {
        this.psychologueNomComplet = psychologueNomComplet;
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

    public boolean isLibre() {
        return libre;
    }

    public void setLibre(boolean libre) {
        this.libre = libre;
    }

    @Override
    public String toString() {
        if (psychologueNomComplet == null || psychologueNomComplet.isBlank()) {
            return date + " | " + heureDebut + " - " + heureFin;
        }
        return psychologueNomComplet + " | " + date + " | " + heureDebut + " - " + heureFin;
    }
}
