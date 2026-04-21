package org.example.entities;

import java.time.LocalDate;

public class AntecedentMedical {
    private int id;
    private int dossierMedicalId;
    private String type;
    private String description;
    private LocalDate dateDiagnostic;
    private DossierMedical dossierMedical;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDossierMedicalId() {
        return dossierMedicalId;
    }

    public void setDossierMedicalId(int dossierMedicalId) {
        this.dossierMedicalId = dossierMedicalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateDiagnostic() {
        return dateDiagnostic;
    }

    public void setDateDiagnostic(LocalDate dateDiagnostic) {
        this.dateDiagnostic = dateDiagnostic;
    }

    public DossierMedical getDossierMedical() {
        return dossierMedical;
    }

    public void setDossierMedical(DossierMedical dossierMedical) {
        this.dossierMedical = dossierMedical;
        if (dossierMedical != null) {
            this.dossierMedicalId = dossierMedical.getId();
        }
    }
}
