package org.example.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ConsultationPayment {
    private int id;
    private int rendezVousId;
    private Integer consultationId;
    private int patientId;
    private int psychologueId;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String checkoutUrl;
    private int amountCents;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String patientName;
    private String patientEmail;
    private String psychologueName;
    private String appointmentType;
    private LocalDate appointmentDate;
    private String appointmentTimeRange;
    private String patientAddress;
    private Double patientLatitude;
    private Double patientLongitude;
    private ConsultationQuestionnaire questionnaire;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRendezVousId() {
        return rendezVousId;
    }

    public void setRendezVousId(int rendezVousId) {
        this.rendezVousId = rendezVousId;
    }

    public Integer getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Integer consultationId) {
        this.consultationId = consultationId;
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

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public String getPsychologueName() {
        return psychologueName;
    }

    public void setPsychologueName(String psychologueName) {
        this.psychologueName = psychologueName;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTimeRange() {
        return appointmentTimeRange;
    }

    public void setAppointmentTimeRange(String appointmentTimeRange) {
        this.appointmentTimeRange = appointmentTimeRange;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public Double getPatientLatitude() {
        return patientLatitude;
    }

    public void setPatientLatitude(Double patientLatitude) {
        this.patientLatitude = patientLatitude;
    }

    public Double getPatientLongitude() {
        return patientLongitude;
    }

    public void setPatientLongitude(Double patientLongitude) {
        this.patientLongitude = patientLongitude;
    }

    public ConsultationQuestionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(ConsultationQuestionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public boolean isPaid() {
        return "paid".equalsIgnoreCase(status);
    }

    public boolean requiresPayment() {
        return !isPaid();
    }

    public String getFormattedAmount() {
        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase();
        if ("TND".equals(normalizedCurrency) || "DT".equals(normalizedCurrency)) {
            return String.format("%.2f DT", amountCents / 1000.0);
        }
        return String.format("%.2f %s", amountCents / 100.0, normalizedCurrency);
    }
}
