package org.example.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DossierMedical {
    private int id;
    private int patientId;
    private String reminderText;
    private String medicalHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AntecedentMedical> antecedentsMedicaux = new ArrayList<>();

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

    public String getReminderText() {
        return reminderText;
    }

    public void setReminderText(String reminderText) {
        this.reminderText = reminderText;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
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

    public List<AntecedentMedical> getAntecedentsMedicaux() {
        return antecedentsMedicaux;
    }

    public void setAntecedentsMedicaux(List<AntecedentMedical> antecedentsMedicaux) {
        this.antecedentsMedicaux = antecedentsMedicaux == null ? new ArrayList<>() : antecedentsMedicaux;
    }

    public void addAntecedentMedical(AntecedentMedical antecedentMedical) {
        if (antecedentMedical == null) {
            return;
        }
        antecedentMedical.setDossierMedical(this);
        antecedentsMedicaux.add(antecedentMedical);
    }
}
