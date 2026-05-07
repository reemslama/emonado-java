package org.example.service;

import com.stripe.exception.StripeException;
import org.example.entities.Consultation;
import org.example.entities.ConsultationPayment;
import org.example.entities.ConsultationQuestionnaire;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConsultationWorkflowService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicalDataService medicalDataService = new MedicalDataService();
    private final StripePaymentService stripePaymentService = new StripePaymentService();
    private final EmailService emailService = new EmailService();

    public void ensureSchema() throws SQLException {
        DatabaseInitializer.initialize();
    }

    public PaymentCreationResult createOrRefreshPaymentForAcceptedRendezVous(int rendezVousId) throws SQLException {
        medicalDataService.ensureSchema();
        Consultation consultation = medicalDataService.getConsultationByRendezVousId(rendezVousId);
        PaymentContext context = getPaymentContext(rendezVousId);
        if (context == null) {
            throw new SQLException("Impossible de charger les informations du rendez-vous accepte.");
        }

        ConsultationPayment existing = getPaymentByRendezVousId(rendezVousId);
        if (existing != null && existing.isPaid()) {
            return PaymentCreationResult.alreadyPaid(existing);
        }

        ConsultationPayment payment = existing == null ? new ConsultationPayment() : existing;
        payment.setRendezVousId(rendezVousId);
        payment.setConsultationId(consultation == null ? null : consultation.getId());
        payment.setPatientId(context.patientId);
        payment.setPsychologueId(context.psychologueId);
        payment.setAmountCents(stripePaymentService.getDefaultAmountCents());
        payment.setCurrency(stripePaymentService.getDefaultCurrency());
        payment.setStatus("pending");

        if (payment.getId() == 0) {
            payment = insertPayment(payment);
        } else {
            updatePaymentMetadata(payment);
        }

        if (!stripePaymentService.isConfigured()) {
            return PaymentCreationResult.configurationMissing(payment);
        }

        try {
            StripePaymentService.StripeCheckoutSession checkoutSession = stripePaymentService.createCheckoutSession(
                    context.patientEmail,
                    context.patientName,
                    context.appointmentType + " du " + context.appointmentDate,
                    payment.getAmountCents(),
                    payment.getCurrency(),
                    "emonado-payment-" + payment.getId(),
                    payment.getId(),
                    rendezVousId
            );
            payment.setStripeSessionId(checkoutSession.getId());
            payment.setStripePaymentIntentId(checkoutSession.getPaymentIntentId());
            payment.setCheckoutUrl(checkoutSession.getUrl());
            payment.setStatus(resolvePaymentStatus(checkoutSession.getPaymentStatus()));
            updatePaymentStripeData(payment);
            sendPaymentRequestEmail(context, payment);
            return PaymentCreationResult.created(payment);
        } catch (StripeException | RuntimeException e) {
            throw new SQLException("Impossible de generer la session Stripe: " + e.getMessage(), e);
        }
    }

    public ConsultationPayment synchronizePaymentStatus(int paymentId) throws SQLException {
        ConsultationPayment payment = getPaymentById(paymentId);
        if (payment == null) {
            throw new SQLException("Paiement introuvable.");
        }
        if (payment.getStripeSessionId() == null || payment.getStripeSessionId().isBlank()) {
            throw new SQLException("Aucune session Stripe n'est liee a ce paiement.");
        }
        if (!stripePaymentService.isConfigured()) {
            throw new SQLException("Configuration Stripe manquante.");
        }

        try {
            StripePaymentService.StripeCheckoutSession session = stripePaymentService.retrieveCheckoutSession(payment.getStripeSessionId());
            payment.setStripePaymentIntentId(session.getPaymentIntentId());
            payment.setStatus(resolvePaymentStatus(session.getPaymentStatus()));
            if ("paid".equalsIgnoreCase(payment.getStatus()) && payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
            updatePaymentStripeData(payment);
            return getPaymentById(paymentId);
        } catch (StripeException e) {
            throw new SQLException("Synchronisation Stripe impossible: " + e.getMessage(), e);
        }
    }

    public ConsultationPayment markPaymentAsPaidLocally(int paymentId) throws SQLException {
        ConsultationPayment payment = getPaymentById(paymentId);
        if (payment == null) {
            throw new SQLException("Paiement introuvable.");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE consultation_payment SET status = 'paid', paid_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, paymentId);
            ps.executeUpdate();
        }
        return getPaymentById(paymentId);
    }

    public ConsultationQuestionnaire saveQuestionnaire(ConsultationQuestionnaire questionnaire) throws SQLException {
        validateQuestionnaire(questionnaire);
        questionnaire.setRiskScore(calculateRiskScore(questionnaire));
        questionnaire.setPredictedState(buildPredictedState(questionnaire));

        if (questionnaire.getId() > 0) {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE consultation_questionnaire SET chief_complaint = ?, symptom_summary = ?, stress_level = ?, anxiety_level = ?, "
                                 + "mood_level = ?, sleep_quality = ?, energy_level = ?, support_level = ?, urgency_level = ?, self_harm_risk = ?, "
                                 + "additional_context = ?, voice_transcript = ?, risk_score = ?, predicted_state = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                fillQuestionnaireStatement(ps, questionnaire);
                ps.setInt(15, questionnaire.getId());
                ps.executeUpdate();
            }
        } else {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO consultation_questionnaire (payment_id, rendez_vous_id, patient_id, psychologue_id, chief_complaint, symptom_summary, "
                                 + "stress_level, anxiety_level, mood_level, sleep_quality, energy_level, support_level, urgency_level, self_harm_risk, additional_context, voice_transcript, risk_score, predicted_state) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, questionnaire.getPaymentId());
                ps.setInt(2, questionnaire.getRendezVousId());
                ps.setInt(3, questionnaire.getPatientId());
                ps.setInt(4, questionnaire.getPsychologueId());
                ps.setString(5, questionnaire.getChiefComplaint());
                ps.setString(6, questionnaire.getSymptomSummary());
                ps.setInt(7, questionnaire.getStressLevel());
                ps.setInt(8, questionnaire.getAnxietyLevel());
                ps.setInt(9, questionnaire.getMoodLevel());
                ps.setInt(10, questionnaire.getSleepQuality());
                ps.setInt(11, questionnaire.getEnergyLevel());
                ps.setInt(12, questionnaire.getSupportLevel());
                ps.setInt(13, questionnaire.getUrgencyLevel());
                ps.setString(14, questionnaire.getSelfHarmRisk());
                ps.setString(15, questionnaire.getAdditionalContext());
                ps.setString(16, questionnaire.getVoiceTranscript());
                ps.setInt(17, questionnaire.getRiskScore());
                ps.setString(18, questionnaire.getPredictedState());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        questionnaire.setId(keys.getInt(1));
                    }
                }
            }
        }
        return getQuestionnaireByPaymentId(questionnaire.getPaymentId());
    }

    public ConsultationQuestionnaire getQuestionnaireByPaymentId(int paymentId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, payment_id, rendez_vous_id, patient_id, psychologue_id, chief_complaint, symptom_summary, stress_level, anxiety_level, mood_level, "
                             + "sleep_quality, energy_level, support_level, urgency_level, self_harm_risk, additional_context, voice_transcript, risk_score, predicted_state, submitted_at "
                             + "FROM consultation_questionnaire WHERE payment_id = ?")) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapQuestionnaire(rs);
                }
            }
        }
        return null;
    }

    public List<ConsultationPayment> getPaymentsByPatient(int patientId) throws SQLException {
        return loadPayments(
                "WHERE cp.patient_id = ? ORDER BY d.date DESC, d.heure_debut DESC",
                statement -> statement.setInt(1, patientId)
        );
    }

    public List<ConsultationPayment> getPaymentsByPsychologue(int psychologueId) throws SQLException {
        return loadPayments(
                "WHERE cp.psychologue_id = ? ORDER BY d.date DESC, d.heure_debut DESC",
                statement -> statement.setInt(1, psychologueId)
        );
    }

    public List<ConsultationPayment> getAllPayments() throws SQLException {
        return loadPayments("ORDER BY cp.created_at DESC", statement -> {
        });
    }

    public ConsultationPayment getPaymentByRendezVousId(int rendezVousId) throws SQLException {
        List<ConsultationPayment> payments = loadPayments(
                "WHERE cp.rendez_vous_id = ? ORDER BY cp.id DESC",
                statement -> statement.setInt(1, rendezVousId)
        );
        return payments.isEmpty() ? null : payments.get(0);
    }

    public ConsultationPayment getPaymentById(int paymentId) throws SQLException {
        List<ConsultationPayment> payments = loadPayments(
                "WHERE cp.id = ?",
                statement -> statement.setInt(1, paymentId)
        );
        return payments.isEmpty() ? null : payments.get(0);
    }

    public boolean isStripeConfigured() {
        return stripePaymentService.isConfigured();
    }

    public void ensurePaymentsExistForAcceptedRendezVousByPatient(int patientId) throws SQLException {
        String sql = "SELECT r.id "
                + "FROM rendez_vous r "
                + "LEFT JOIN consultation_payment cp ON cp.rendez_vous_id = r.id "
                + "WHERE r.user_id = ? AND r.statut = 'acceptee' AND cp.id IS NULL "
                + "ORDER BY r.id";

        List<Integer> rendezVousIds = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rendezVousIds.add(rs.getInt("id"));
                }
            }
        }

        for (Integer rendezVousId : rendezVousIds) {
            createOrRefreshPaymentForAcceptedRendezVous(rendezVousId);
        }
    }

    private List<ConsultationPayment> loadPayments(String whereClause, SqlConsumer<PreparedStatement> binder) throws SQLException {
        String sql = "SELECT cp.id, cp.rendez_vous_id, cp.consultation_id, cp.patient_id, cp.psychologue_id, cp.stripe_session_id, cp.stripe_payment_intent_id, "
                + "cp.checkout_url, cp.amount_cents, cp.currency, cp.status, cp.paid_at, cp.created_at, cp.updated_at, "
                + "CONCAT(COALESCE(p.prenom, ''), ' ', COALESCE(p.nom, '')) AS patient_name, p.email AS patient_email, "
                + "CONCAT(COALESCE(psy.prenom, ''), ' ', COALESCE(psy.nom, '')) AS psychologue_name, "
                + "t.libelle AS appointment_type, d.date AS appointment_date, d.heure_debut, d.heure_fin, "
                + "r.adresse AS patient_address, r.latitude AS patient_latitude, r.longitude AS patient_longitude "
                + "FROM consultation_payment cp "
                + "JOIN rendez_vous r ON r.id = cp.rendez_vous_id "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "JOIN type_rendez_vous t ON t.id = r.type_id "
                + "JOIN user p ON p.id = cp.patient_id "
                + "JOIN user psy ON psy.id = cp.psychologue_id "
                + whereClause;

        List<ConsultationPayment> payments = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConsultationPayment payment = mapPayment(rs);
                    payments.add(payment);
                }
            }
        }
        for (ConsultationPayment payment : payments) {
            payment.setQuestionnaire(getQuestionnaireByPaymentId(payment.getId()));
        }
        return payments;
    }

    private ConsultationPayment insertPayment(ConsultationPayment payment) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO consultation_payment (rendez_vous_id, consultation_id, patient_id, psychologue_id, amount_cents, currency, status) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getRendezVousId());
            if (payment.getConsultationId() == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, payment.getConsultationId());
            }
            ps.setInt(3, payment.getPatientId());
            ps.setInt(4, payment.getPsychologueId());
            ps.setInt(5, payment.getAmountCents());
            ps.setString(6, payment.getCurrency());
            ps.setString(7, payment.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setId(keys.getInt(1));
                }
            }
        }
        return payment;
    }

    private void updatePaymentMetadata(ConsultationPayment payment) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE consultation_payment SET consultation_id = ?, patient_id = ?, psychologue_id = ?, amount_cents = ?, currency = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            if (payment.getConsultationId() == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, payment.getConsultationId());
            }
            ps.setInt(2, payment.getPatientId());
            ps.setInt(3, payment.getPsychologueId());
            ps.setInt(4, payment.getAmountCents());
            ps.setString(5, payment.getCurrency());
            ps.setInt(6, payment.getId());
            ps.executeUpdate();
        }
    }

    private void updatePaymentStripeData(ConsultationPayment payment) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE consultation_payment SET stripe_session_id = ?, stripe_payment_intent_id = ?, checkout_url = ?, status = ?, paid_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, payment.getStripeSessionId());
            ps.setString(2, payment.getStripePaymentIntentId());
            ps.setString(3, payment.getCheckoutUrl());
            ps.setString(4, payment.getStatus());
            if (payment.getPaidAt() == null) {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
            }
            ps.setInt(6, payment.getId());
            ps.executeUpdate();
        }
    }

    private ConsultationPayment mapPayment(ResultSet rs) throws SQLException {
        ConsultationPayment payment = new ConsultationPayment();
        payment.setId(rs.getInt("id"));
        payment.setRendezVousId(rs.getInt("rendez_vous_id"));
        int consultationId = rs.getInt("consultation_id");
        if (!rs.wasNull()) {
            payment.setConsultationId(consultationId);
        }
        payment.setPatientId(rs.getInt("patient_id"));
        payment.setPsychologueId(rs.getInt("psychologue_id"));
        payment.setStripeSessionId(rs.getString("stripe_session_id"));
        payment.setStripePaymentIntentId(rs.getString("stripe_payment_intent_id"));
        payment.setCheckoutUrl(rs.getString("checkout_url"));
        payment.setAmountCents(rs.getInt("amount_cents"));
        payment.setCurrency(rs.getString("currency"));
        payment.setStatus(rs.getString("status"));
        Timestamp paidAt = rs.getTimestamp("paid_at");
        if (paidAt != null) {
            payment.setPaidAt(paidAt.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            payment.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            payment.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        payment.setPatientName(normalizeName(rs.getString("patient_name")));
        payment.setPatientEmail(rs.getString("patient_email"));
        payment.setPsychologueName(normalizeName(rs.getString("psychologue_name")));
        payment.setAppointmentType(rs.getString("appointment_type"));
        payment.setPatientAddress(rs.getString("patient_address"));
        double latitude = rs.getDouble("patient_latitude");
        if (!rs.wasNull()) {
            payment.setPatientLatitude(latitude);
        }
        double longitude = rs.getDouble("patient_longitude");
        if (!rs.wasNull()) {
            payment.setPatientLongitude(longitude);
        }
        Date appointmentDate = rs.getDate("appointment_date");
        if (appointmentDate != null) {
            payment.setAppointmentDate(appointmentDate.toLocalDate());
        }
        TimeRange timeRange = extractTimeRange(rs);
        payment.setAppointmentTimeRange(timeRange.label);
        return payment;
    }

    private ConsultationQuestionnaire mapQuestionnaire(ResultSet rs) throws SQLException {
        ConsultationQuestionnaire questionnaire = new ConsultationQuestionnaire();
        questionnaire.setId(rs.getInt("id"));
        questionnaire.setPaymentId(rs.getInt("payment_id"));
        questionnaire.setRendezVousId(rs.getInt("rendez_vous_id"));
        questionnaire.setPatientId(rs.getInt("patient_id"));
        questionnaire.setPsychologueId(rs.getInt("psychologue_id"));
        questionnaire.setChiefComplaint(rs.getString("chief_complaint"));
        questionnaire.setSymptomSummary(rs.getString("symptom_summary"));
        questionnaire.setStressLevel(rs.getInt("stress_level"));
        questionnaire.setAnxietyLevel(rs.getInt("anxiety_level"));
        questionnaire.setMoodLevel(rs.getInt("mood_level"));
        questionnaire.setSleepQuality(rs.getInt("sleep_quality"));
        questionnaire.setEnergyLevel(rs.getInt("energy_level"));
        questionnaire.setSupportLevel(rs.getInt("support_level"));
        questionnaire.setUrgencyLevel(rs.getInt("urgency_level"));
        questionnaire.setSelfHarmRisk(rs.getString("self_harm_risk"));
        questionnaire.setAdditionalContext(rs.getString("additional_context"));
        questionnaire.setVoiceTranscript(rs.getString("voice_transcript"));
        questionnaire.setRiskScore(rs.getInt("risk_score"));
        questionnaire.setPredictedState(rs.getString("predicted_state"));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            questionnaire.setSubmittedAt(submittedAt.toLocalDateTime());
        }
        return questionnaire;
    }

    private void fillQuestionnaireStatement(PreparedStatement ps, ConsultationQuestionnaire questionnaire) throws SQLException {
        ps.setString(1, questionnaire.getChiefComplaint());
        ps.setString(2, questionnaire.getSymptomSummary());
        ps.setInt(3, questionnaire.getStressLevel());
        ps.setInt(4, questionnaire.getAnxietyLevel());
        ps.setInt(5, questionnaire.getMoodLevel());
        ps.setInt(6, questionnaire.getSleepQuality());
        ps.setInt(7, questionnaire.getEnergyLevel());
        ps.setInt(8, questionnaire.getSupportLevel());
        ps.setInt(9, questionnaire.getUrgencyLevel());
        ps.setString(10, questionnaire.getSelfHarmRisk());
        ps.setString(11, questionnaire.getAdditionalContext());
        ps.setString(12, questionnaire.getVoiceTranscript());
        ps.setInt(13, questionnaire.getRiskScore());
        ps.setString(14, questionnaire.getPredictedState());
    }

    private void validateQuestionnaire(ConsultationQuestionnaire questionnaire) throws SQLException {
        if (questionnaire == null) {
            throw new SQLException("Formulaire invalide.");
        }
        if (questionnaire.getPaymentId() <= 0) {
            throw new SQLException("Paiement invalide.");
        }
        ConsultationPayment payment = getPaymentById(questionnaire.getPaymentId());
        if (payment == null || !payment.isPaid()) {
            throw new SQLException("Le formulaire n'est accessible qu'apres un paiement valide.");
        }
        if (isBlank(questionnaire.getChiefComplaint()) || questionnaire.getChiefComplaint().trim().length() < 10) {
            throw new SQLException("Veuillez decrire le motif principal avec au moins 10 caracteres.");
        }
        if (isBlank(questionnaire.getSymptomSummary()) || questionnaire.getSymptomSummary().trim().length() < 10) {
            throw new SQLException("Veuillez decrire les symptomes avec au moins 10 caracteres.");
        }
        validateRange(questionnaire.getStressLevel(), "stress");
        validateRange(questionnaire.getAnxietyLevel(), "anxiete");
        validateRange(questionnaire.getMoodLevel(), "humeur");
        validateRange(questionnaire.getSleepQuality(), "sommeil");
        validateRange(questionnaire.getEnergyLevel(), "energie");
        validateRange(questionnaire.getSupportLevel(), "soutien");
        validateRange(questionnaire.getUrgencyLevel(), "urgence");
        if (isBlank(questionnaire.getSelfHarmRisk())) {
            throw new SQLException("Veuillez preciser le risque d'auto-agression.");
        }
    }

    private void validateRange(int value, String fieldName) throws SQLException {
        if (value < 0 || value > 10) {
            throw new SQLException("La valeur pour " + fieldName + " doit etre comprise entre 0 et 10.");
        }
    }

    private int calculateRiskScore(ConsultationQuestionnaire questionnaire) {
        int moodRisk = 10 - questionnaire.getMoodLevel();
        int sleepRisk = 10 - questionnaire.getSleepQuality();
        int energyRisk = 10 - questionnaire.getEnergyLevel();
        int supportRisk = 10 - questionnaire.getSupportLevel();

        int score = questionnaire.getStressLevel() * 3
                + questionnaire.getAnxietyLevel() * 3
                + moodRisk * 2
                + sleepRisk * 2
                + energyRisk * 2
                + questionnaire.getUrgencyLevel() * 3
                + supportRisk;

        String selfHarmRisk = questionnaire.getSelfHarmRisk() == null ? "" : questionnaire.getSelfHarmRisk().toLowerCase();
        if ("moderate".equals(selfHarmRisk)) {
            score += 8;
        } else if ("high".equals(selfHarmRisk)) {
            score += 15;
        }
        return score;
    }

    private String buildPredictedState(ConsultationQuestionnaire questionnaire) {
        int score = questionnaire.getRiskScore();
        String selfHarmRisk = questionnaire.getSelfHarmRisk() == null ? "" : questionnaire.getSelfHarmRisk().toLowerCase();
        if ("high".equals(selfHarmRisk) || questionnaire.getUrgencyLevel() >= 8 || score >= 65) {
            return "Priorite elevee: detresse psychologique a evaluer rapidement.";
        }
        if ("moderate".equals(selfHarmRisk) || score >= 45) {
            return "Vigilance clinique: anxiete/stress significatifs avec besoin de suivi rapproche.";
        }
        if (score >= 28) {
            return "Etat fragile mais stable: explorer les facteurs declencheurs et le sommeil.";
        }
        return "Etat psychologique plutot stable selon le formulaire, avec suivi standard recommande.";
    }

    private String resolvePaymentStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return "pending";
        }
        return "paid".equalsIgnoreCase(stripeStatus) ? "paid" : "pending";
    }

    private void sendPaymentRequestEmail(PaymentContext context, ConsultationPayment payment) {
        if (isBlank(context.patientEmail) || isBlank(payment.getCheckoutUrl())) {
            return;
        }
        emailService.sendPaymentRequestedEmail(
                context.patientEmail,
                context.patientName,
                context.psychologueName,
                context.appointmentDate.toString(),
                context.timeRange,
                context.appointmentType,
                payment.getFormattedAmount(),
                payment.getCheckoutUrl()
        );
    }

    private PaymentContext getPaymentContext(int rendezVousId) throws SQLException {
        String sql = "SELECT r.user_id AS patient_id, d.psychologue_id, p.email AS patient_email, "
                + "CONCAT(COALESCE(p.prenom, ''), ' ', COALESCE(p.nom, '')) AS patient_name, "
                + "CONCAT(COALESCE(psy.prenom, ''), ' ', COALESCE(psy.nom, '')) AS psychologue_name, "
                + "t.libelle AS appointment_type, d.date AS appointment_date, d.heure_debut, d.heure_fin "
                + "FROM rendez_vous r "
                + "JOIN disponibilite d ON d.id = r.dispo_id "
                + "JOIN user p ON p.id = r.user_id "
                + "JOIN user psy ON psy.id = d.psychologue_id "
                + "JOIN type_rendez_vous t ON t.id = r.type_id "
                + "WHERE r.id = ? AND r.statut = 'acceptee'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                PaymentContext context = new PaymentContext();
                context.patientId = rs.getInt("patient_id");
                context.psychologueId = rs.getInt("psychologue_id");
                context.patientEmail = rs.getString("patient_email");
                context.patientName = normalizeName(rs.getString("patient_name"));
                context.psychologueName = normalizeName(rs.getString("psychologue_name"));
                context.appointmentType = rs.getString("appointment_type");
                context.appointmentDate = rs.getDate("appointment_date").toLocalDate();
                context.timeRange = extractTimeRange(rs).label;
                return context;
            }
        }
    }

    private TimeRange extractTimeRange(ResultSet rs) throws SQLException {
        LocalTime start = rs.getTime("heure_debut").toLocalTime();
        LocalTime end = rs.getTime("heure_fin").toLocalTime();
        return new TimeRange(TIME_FORMATTER.format(start) + " - " + TIME_FORMATTER.format(end));
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Connection getConnection() {
        return DataSource.getInstance().getConnection();
    }

    @FunctionalInterface
    private interface SqlConsumer<T> {
        void accept(T value) throws SQLException;
    }

    private static final class PaymentContext {
        private int patientId;
        private int psychologueId;
        private String patientEmail;
        private String patientName;
        private String psychologueName;
        private String appointmentType;
        private LocalDate appointmentDate;
        private String timeRange;
    }

    private record TimeRange(String label) {
    }

    public static final class PaymentCreationResult {
        private final ConsultationPayment payment;
        private final boolean stripeConfigured;
        private final boolean alreadyPaid;

        private PaymentCreationResult(ConsultationPayment payment, boolean stripeConfigured, boolean alreadyPaid) {
            this.payment = payment;
            this.stripeConfigured = stripeConfigured;
            this.alreadyPaid = alreadyPaid;
        }

        public static PaymentCreationResult created(ConsultationPayment payment) {
            return new PaymentCreationResult(payment, true, false);
        }

        public static PaymentCreationResult configurationMissing(ConsultationPayment payment) {
            return new PaymentCreationResult(payment, false, false);
        }

        public static PaymentCreationResult alreadyPaid(ConsultationPayment payment) {
            return new PaymentCreationResult(payment, true, true);
        }

        public ConsultationPayment getPayment() {
            return payment;
        }

        public boolean isStripeConfigured() {
            return stripeConfigured;
        }

        public boolean isAlreadyPaid() {
            return alreadyPaid;
        }
    }
}
