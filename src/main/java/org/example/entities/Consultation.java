package org.example.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Consultation {
    private int id;
    private int patientId;
    private LocalDate consultationDate;
    private String notesPatient;
    private String notesPsychologue;
    private Integer psychologueId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public LocalDate getConsultationDate() {
        return consultationDate;
    }

    public void setConsultationDate(LocalDate consultationDate) {
        this.consultationDate = consultationDate;
    }

    public String getNotesPatient() {
        return notesPatient;
    }

    public void setNotesPatient(String notesPatient) {
        this.notesPatient = notesPatient;
    }

    public String getNotesPsychologue() {
        return notesPsychologue;
    }

    public void setNotesPsychologue(String notesPsychologue) {
        this.notesPsychologue = notesPsychologue;
    }

    public Integer getPsychologueId() {
        return psychologueId;
    }

    public void setPsychologueId(Integer psychologueId) {
        this.psychologueId = psychologueId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
