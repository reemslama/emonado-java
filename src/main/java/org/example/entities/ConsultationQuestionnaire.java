package org.example.entities;

import java.time.LocalDateTime;

public class ConsultationQuestionnaire {
    private int id;
    private int paymentId;
    private int rendezVousId;
    private int patientId;
    private int psychologueId;
    private String chiefComplaint;
    private String symptomSummary;
    private int stressLevel;
    private int anxietyLevel;
    private int moodLevel;
    private int sleepQuality;
    private int energyLevel;
    private int supportLevel;
    private int urgencyLevel;
    private String selfHarmRisk;
    private String additionalContext;
    private String voiceTranscript;
    private int riskScore;
    private String predictedState;
    private LocalDateTime submittedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getRendezVousId() {
        return rendezVousId;
    }

    public void setRendezVousId(int rendezVousId) {
        this.rendezVousId = rendezVousId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getPsychologueId() {
        return psychologueId;
    }

    public void setPsychologueId(int psychologueId) {
        this.psychologueId = psychologueId;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getSymptomSummary() {
        return symptomSummary;
    }

    public void setSymptomSummary(String symptomSummary) {
        this.symptomSummary = symptomSummary;
    }

    public int getStressLevel() {
        return stressLevel;
    }

    public void setStressLevel(int stressLevel) {
        this.stressLevel = stressLevel;
    }

    public int getAnxietyLevel() {
        return anxietyLevel;
    }

    public void setAnxietyLevel(int anxietyLevel) {
        this.anxietyLevel = anxietyLevel;
    }

    public int getMoodLevel() {
        return moodLevel;
    }

    public void setMoodLevel(int moodLevel) {
        this.moodLevel = moodLevel;
    }

    public int getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(int sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setEnergyLevel(int energyLevel) {
        this.energyLevel = energyLevel;
    }

    public int getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(int supportLevel) {
        this.supportLevel = supportLevel;
    }

    public int getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(int urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getSelfHarmRisk() {
        return selfHarmRisk;
    }

    public void setSelfHarmRisk(String selfHarmRisk) {
        this.selfHarmRisk = selfHarmRisk;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
    }

    public String getVoiceTranscript() {
        return voiceTranscript;
    }

    public void setVoiceTranscript(String voiceTranscript) {
        this.voiceTranscript = voiceTranscript;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getPredictedState() {
        return predictedState;
    }

    public void setPredictedState(String predictedState) {
        this.predictedState = predictedState;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
