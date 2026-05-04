package org.example.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class EmailService {

    private static final String DEFAULT_SMTP_HOST = "smtp.gmail.com";
    private static final String DEFAULT_SMTP_PORT = "587";
    private static final String DEFAULT_SMTP_SSL_PORT = "465";

    private final Properties fileConfig = loadFileConfig();
    private final String smtpHost;
    private final String smtpPort;
    private final String senderEmail;
    private final String senderPassword;

    public EmailService() {
        this.smtpHost = readConfig("MAIL_SMTP_HOST", DEFAULT_SMTP_HOST);
        this.smtpPort = readConfig("MAIL_SMTP_PORT", DEFAULT_SMTP_PORT);
        this.senderEmail = readConfig("MAIL_USERNAME", "");
        this.senderPassword = normalizePassword(readConfig("MAIL_PASSWORD", ""));

        System.out.println("[EmailService] Sender: "
                + (senderEmail.isBlank() ? "NOT CONFIGURED" : senderEmail)
                + " | SMTP: " + smtpHost + ":" + smtpPort);
    }

    public void sendPasswordResetCode(String recipientEmail, String resetCode) {
        sendTextEmail(
                recipientEmail,
                "Reinitialisation de votre mot de passe Emonado",
                buildResetBody(resetCode)
        );
    }

    public void sendAppointmentAcceptedEmail(String recipientEmail,
                                             String patientName,
                                             String psychologueName,
                                             String appointmentDate,
                                             String timeRange,
                                             String appointmentType) {
        sendHtmlEmail(
                recipientEmail,
                "Confirmation de votre rendez-vous EmoNado",
                buildAppointmentAcceptedBody(patientName, psychologueName,
                        appointmentDate, timeRange, appointmentType),
                buildAppointmentAcceptedHtml(patientName, psychologueName,
                        appointmentDate, timeRange, appointmentType)
        );
    }

    public void sendAppointmentReminderEmail(String recipientEmail,
                                             String patientName,
                                             String psychologueName,
                                             String appointmentDate,
                                             String timeRange,
                                             String appointmentType) {
        sendHtmlEmail(
                recipientEmail,
                "Rappel de rendez-vous EmoNado - dans 24h",
                buildAppointmentReminderBody(patientName, psychologueName, appointmentDate, timeRange, appointmentType),
                buildAppointmentReminderHtml(patientName, psychologueName, appointmentDate, timeRange, appointmentType)
        );
    }

    public void sendPaymentRequestedEmail(String recipientEmail,
                                          String patientName,
                                          String psychologueName,
                                          String appointmentDate,
                                          String timeRange,
                                          String appointmentType,
                                          String amount,
                                          String paymentUrl) {
        sendHtmlEmail(
                recipientEmail,
                "Paiement requis pour votre consultation EmoNado",
                buildPaymentRequestBody(patientName, psychologueName, appointmentDate, timeRange, appointmentType, amount, paymentUrl),
                buildPaymentRequestHtml(patientName, psychologueName, appointmentDate, timeRange, appointmentType, amount, paymentUrl)
        );
    }

    private void sendTextEmail(String recipientEmail, String subject, String body) {
        ensureConfigured();
        validateRecipient(recipientEmail);

        RuntimeException lastError = null;
        for (SmtpAttempt attempt : buildAttempts()) {
            try {
                Session session = createSession(attempt);
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail, true));
                message.setSubject(subject, StandardCharsets.UTF_8.name());
                message.setText(body, StandardCharsets.UTF_8.name());
                Transport.send(message);
                System.out.println("[EmailService] Email sent to " + recipientEmail
                        + " via " + attempt.host + ":" + attempt.port
                        + " (" + attempt.modeLabel() + ")");
                return;
            } catch (MessagingException e) {
                lastError = new RuntimeException(
                        "SMTP failure via " + attempt.host + ":" + attempt.port
                                + " (" + attempt.modeLabel() + ") - " + rootMessage(e), e);
                System.err.println("[EmailService] " + lastError.getMessage());
            }
        }

        throw (lastError != null) ? lastError
                : new RuntimeException("Impossible d'envoyer l'email.");
    }

    private void sendHtmlEmail(String recipientEmail, String subject, String textBody, String htmlBody) {
        ensureConfigured();
        validateRecipient(recipientEmail);

        RuntimeException lastError = null;
        for (SmtpAttempt attempt : buildAttempts()) {
            try {
                Session session = createSession(attempt);
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail, true));
                message.setSubject(subject, StandardCharsets.UTF_8.name());
                Multipart multipart = new MimeMultipart("alternative");

                MimeBodyPart plainPart = new MimeBodyPart();
                plainPart.setText(textBody, StandardCharsets.UTF_8.name());
                multipart.addBodyPart(plainPart);

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
                multipart.addBodyPart(htmlPart);

                message.setContent(multipart);
                message.saveChanges();
                Transport.send(message);
                System.out.println("[EmailService] Email sent to " + recipientEmail
                        + " via " + attempt.host + ":" + attempt.port
                        + " (" + attempt.modeLabel() + ")");
                return;
            } catch (MessagingException e) {
                lastError = new RuntimeException(
                        "SMTP failure via " + attempt.host + ":" + attempt.port
                                + " (" + attempt.modeLabel() + ") - " + rootMessage(e), e);
                System.err.println("[EmailService] " + lastError.getMessage());
            }
        }

        throw (lastError != null) ? lastError
                : new RuntimeException("Impossible d'envoyer l'email.");
    }

    private Session createSession(SmtpAttempt attempt) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", attempt.host);
        props.put("mail.smtp.port", attempt.port);
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        props.put("mail.smtp.ssl.trust", attempt.host);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.checkserveridentity", "true");

        if (attempt.ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        final String email = senderEmail;
        final String password = senderPassword;
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    private SmtpAttempt[] buildAttempts() {
        if (DEFAULT_SMTP_SSL_PORT.equals(smtpPort)) {
            return new SmtpAttempt[]{
                    new SmtpAttempt(smtpHost, smtpPort, true),
                    new SmtpAttempt(smtpHost, DEFAULT_SMTP_PORT, false)
            };
        }
        return new SmtpAttempt[]{
                new SmtpAttempt(smtpHost, smtpPort, false),
                new SmtpAttempt(smtpHost, DEFAULT_SMTP_SSL_PORT, true)
        };
    }

    private String buildResetBody(String resetCode) {
        return "Bonjour,\n\n"
                + "Voici votre code de verification pour reinitialiser votre mot de passe Emonado : "
                + resetCode + "\n\n"
                + "Ce code est personnel. Si vous n'avez pas demande cette operation, ignorez cet email.\n\n"
                + "Equipe Emonado";
    }

    private String buildAppointmentAcceptedBody(String patientName,
                                                String psychologueName,
                                                String appointmentDate,
                                                String timeRange,
                                                String appointmentType) {
        return "Bonjour " + fallback(patientName, "cher patient") + ",\n\n"
                + "Votre rendez-vous a ete accepte sur EmoNado.\n\n"
                + "Type       : " + fallback(appointmentType, "Consultation") + "\n"
                + "Psychologue: " + fallback(psychologueName, "Votre psychologue") + "\n"
                + "Date       : " + fallback(appointmentDate, "-") + "\n"
                + "Horaire    : " + fallback(timeRange, "-") + "\n\n"
                + "Merci de vous presenter a l'heure prevue.\n\n"
                + "Equipe EmoNado";
    }

    private String buildAppointmentAcceptedHtml(String patientName,
                                                String psychologueName,
                                                String appointmentDate,
                                                String timeRange,
                                                String appointmentType) {
        String safePatientName = escapeHtml(fallback(patientName, "cher patient"));
        String safePsychologueName = escapeHtml(fallback(psychologueName, "Votre psychologue"));
        String safeAppointmentDate = escapeHtml(fallback(appointmentDate, "-"));
        String safeTimeRange = escapeHtml(fallback(timeRange, "-"));
        String safeAppointmentType = escapeHtml(fallback(appointmentType, "Consultation"));

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Confirmation de rendez-vous</title>
                </head>
                <body style="margin:0;padding:0;background-color:#eef4f8;font-family:Arial,'Helvetica Neue',sans-serif;color:#16313b;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:linear-gradient(180deg,#eef4f8 0%%,#dfeef1 100%%);padding:32px 16px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background-color:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 50px rgba(18,55,63,0.14);">
                                    <tr>
                                        <td style="padding:0;background:linear-gradient(135deg,#1f7a8c 0%%,#42b883 100%%);">
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                                <tr>
                                                    <td style="padding:36px 40px 28px 40px;">
                                                        <div style="display:inline-block;padding:8px 14px;border-radius:999px;background-color:rgba(255,255,255,0.18);color:#ffffff;font-size:12px;font-weight:bold;letter-spacing:0.08em;text-transform:uppercase;">
                                                            EmoNado
                                                        </div>
                                                        <h1 style="margin:18px 0 10px 0;color:#ffffff;font-size:32px;line-height:1.2;font-weight:700;">
                                                            Rendez-vous confirme
                                                        </h1>
                                                        <p style="margin:0;color:rgba(255,255,255,0.92);font-size:16px;line-height:1.6;">
                                                            Bonjour %s, votre demande de consultation a ete acceptee. Voici le recapitulatif de votre seance.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px 40px 10px 40px;">
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f6fbfc;border:1px solid #d8eaee;border-radius:18px;">
                                                <tr>
                                                    <td style="padding:24px 24px 8px 24px;">
                                                        <div style="font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#5b7c84;margin-bottom:16px;">
                                                            Details du rendez-vous
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding:0 24px 24px 24px;">
                                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                                            <tr>
                                                                <td style="padding:12px 0;border-bottom:1px solid #dfecef;width:36%%;color:#64818a;font-size:14px;">Type</td>
                                                                <td style="padding:12px 0;border-bottom:1px solid #dfecef;color:#18363f;font-size:15px;font-weight:600;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:12px 0;border-bottom:1px solid #dfecef;color:#64818a;font-size:14px;">Psychologue</td>
                                                                <td style="padding:12px 0;border-bottom:1px solid #dfecef;color:#18363f;font-size:15px;font-weight:600;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:12px 0;border-bottom:1px solid #dfecef;color:#64818a;font-size:14px;">Date</td>
                                                                <td style="padding:12px 0;border-bottom:1px solid #dfecef;color:#18363f;font-size:15px;font-weight:600;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:12px 0;color:#64818a;font-size:14px;">Horaire</td>
                                                                <td style="padding:12px 0;color:#18363f;font-size:15px;font-weight:600;">%s</td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:22px 40px 8px 40px;">
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#fff8ed;border-radius:18px;border:1px solid #f3dfbd;">
                                                <tr>
                                                    <td style="padding:20px 24px;">
                                                        <div style="font-size:16px;font-weight:700;color:#8a5a12;margin-bottom:8px;">
                                                            Bon a savoir
                                                        </div>
                                                        <p style="margin:0;color:#74542a;font-size:14px;line-height:1.7;">
                                                            Merci de vous presenter quelques minutes avant l'heure prevue et de verifier votre disponibilite pour assurer le bon deroulement de la consultation.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:26px 40px 40px 40px;">
                                            <p style="margin:0 0 10px 0;color:#45616a;font-size:14px;line-height:1.7;">
                                                Si vous avez besoin d'aide ou d'un changement, contactez votre psychologue depuis la plateforme EmoNado.
                                            </p>
                                            <p style="margin:0;color:#18363f;font-size:14px;font-weight:700;">
                                                Equipe EmoNado
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                safePatientName,
                safeAppointmentType,
                safePsychologueName,
                safeAppointmentDate,
                safeTimeRange
        );
    }

    private String buildPaymentRequestBody(String patientName,
                                           String psychologueName,
                                           String appointmentDate,
                                           String timeRange,
                                           String appointmentType,
                                           String amount,
                                           String paymentUrl) {
        return "Bonjour " + fallback(patientName, "cher patient") + ",\n\n"
                + "Votre consultation a ete acceptee. Le paiement est maintenant requis pour confirmer la prise en charge.\n\n"
                + "Type       : " + fallback(appointmentType, "Consultation") + "\n"
                + "Psychologue: " + fallback(psychologueName, "Votre psychologue") + "\n"
                + "Date       : " + fallback(appointmentDate, "-") + "\n"
                + "Horaire    : " + fallback(timeRange, "-") + "\n"
                + "Montant    : " + fallback(amount, "-") + "\n\n"
                + "Lien de paiement : " + fallback(paymentUrl, "-") + "\n\n"
                + "Apres paiement, un formulaire clinique vous sera demande dans l'application.\n\n"
                + "Equipe EmoNado";
    }

    private String buildAppointmentReminderBody(String patientName,
                                                String psychologueName,
                                                String appointmentDate,
                                                String timeRange,
                                                String appointmentType) {
        return "Bonjour " + fallback(patientName, "cher patient") + ",\n\n"
                + "Ceci est un rappel: votre " + fallback(appointmentType, "rendez-vous")
                + " avec " + fallback(psychologueName, "votre psychologue")
                + " est prevu dans 24h.\n\n"
                + "Date    : " + fallback(appointmentDate, "-") + "\n"
                + "Horaire : " + fallback(timeRange, "-") + "\n\n"
                + "Equipe EmoNado";
    }

    private String buildAppointmentReminderHtml(String patientName,
                                                String psychologueName,
                                                String appointmentDate,
                                                String timeRange,
                                                String appointmentType) {
        String safePatientName = escapeHtml(fallback(patientName, "cher patient"));
        String safePsychologueName = escapeHtml(fallback(psychologueName, "Votre psychologue"));
        String safeAppointmentDate = escapeHtml(fallback(appointmentDate, "-"));
        String safeTimeRange = escapeHtml(fallback(timeRange, "-"));
        String safeAppointmentType = escapeHtml(fallback(appointmentType, "Consultation"));
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,sans-serif;color:#16313b;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px;">
                        <tr><td align="center">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 14px 34px rgba(18,55,63,0.12);">
                                <tr><td style="padding:28px 34px;background:linear-gradient(135deg,#1f7a8c 0%%,#42b883 100%%);">
                                    <div style="color:#ffffff;font-size:12px;font-weight:bold;letter-spacing:0.08em;text-transform:uppercase;">EmoNado</div>
                                    <h1 style="margin:12px 0 8px 0;color:#ffffff;font-size:28px;">Rappel de rendez-vous</h1>
                                    <p style="margin:0;color:rgba(255,255,255,0.92);font-size:15px;line-height:1.6;">Bonjour %s, votre rendez-vous est prevu dans 24 heures.</p>
                                </td></tr>
                                <tr><td style="padding:28px 34px;">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f8fbfc;border:1px solid #deecef;border-radius:16px;">
                                        <tr><td style="padding:16px 20px;border-bottom:1px solid #deecef;color:#5b7c84;">Type</td><td style="padding:16px 20px;border-bottom:1px solid #deecef;font-weight:700;color:#18363f;">%s</td></tr>
                                        <tr><td style="padding:16px 20px;border-bottom:1px solid #deecef;color:#5b7c84;">Psychologue</td><td style="padding:16px 20px;border-bottom:1px solid #deecef;font-weight:700;color:#18363f;">%s</td></tr>
                                        <tr><td style="padding:16px 20px;border-bottom:1px solid #deecef;color:#5b7c84;">Date</td><td style="padding:16px 20px;border-bottom:1px solid #deecef;font-weight:700;color:#18363f;">%s</td></tr>
                                        <tr><td style="padding:16px 20px;color:#5b7c84;">Horaire</td><td style="padding:16px 20px;font-weight:700;color:#18363f;">%s</td></tr>
                                    </table>
                                    <p style="margin:22px 0 0 0;color:#45616a;font-size:14px;line-height:1.7;">Merci de verifier votre disponibilite et d'etre ponctuel pour assurer le bon deroulement de la consultation.</p>
                                </td></tr>
                            </table>
                        </td></tr>
                    </table>
                </body>
                </html>
                """.formatted(safePatientName, safeAppointmentType, safePsychologueName, safeAppointmentDate, safeTimeRange);
    }

    private String buildPaymentRequestHtml(String patientName,
                                           String psychologueName,
                                           String appointmentDate,
                                           String timeRange,
                                           String appointmentType,
                                           String amount,
                                           String paymentUrl) {
        String safePatientName = escapeHtml(fallback(patientName, "cher patient"));
        String safePsychologueName = escapeHtml(fallback(psychologueName, "Votre psychologue"));
        String safeAppointmentDate = escapeHtml(fallback(appointmentDate, "-"));
        String safeTimeRange = escapeHtml(fallback(timeRange, "-"));
        String safeAppointmentType = escapeHtml(fallback(appointmentType, "Consultation"));
        String safeAmount = escapeHtml(fallback(amount, "-"));
        String safePaymentUrl = escapeHtml(fallback(paymentUrl, "#"));

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Paiement consultation</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,'Helvetica Neue',sans-serif;color:#11263c;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:28px 14px;background:linear-gradient(180deg,#f7fafc 0%%,#ecf3ff 100%%);">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:660px;background:#ffffff;border-radius:28px;overflow:hidden;box-shadow:0 16px 45px rgba(17,38,60,0.14);">
                                    <tr>
                                        <td style="padding:34px 40px;background:linear-gradient(135deg,#0f766e 0%%,#2563eb 100%%);">
                                            <div style="display:inline-block;background:rgba(255,255,255,0.16);padding:8px 14px;border-radius:999px;color:#ffffff;font-size:12px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;">EmoNado paiement</div>
                                            <h1 style="margin:18px 0 8px 0;color:#ffffff;font-size:30px;line-height:1.2;">Action requise</h1>
                                            <p style="margin:0;color:rgba(255,255,255,0.92);font-size:16px;line-height:1.7;">
                                                Bonjour %s, votre consultation a ete acceptee. Finalisez le paiement pour debloquer le formulaire clinique pre-consultation.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:30px 40px 18px 40px;">
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f8fbff;border:1px solid #dbe8ff;border-radius:18px;">
                                                <tr><td style="padding:22px 24px 10px 24px;color:#5b6d82;font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;">Recapitulatif</td></tr>
                                                <tr><td style="padding:0 24px 24px 24px;">
                                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                                        <tr><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#64748b;">Type</td><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#0f172a;font-weight:700;">%s</td></tr>
                                                        <tr><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#64748b;">Psychologue</td><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#0f172a;font-weight:700;">%s</td></tr>
                                                        <tr><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#64748b;">Date</td><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#0f172a;font-weight:700;">%s</td></tr>
                                                        <tr><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#64748b;">Horaire</td><td style="padding:11px 0;border-bottom:1px solid #e5eefc;color:#0f172a;font-weight:700;">%s</td></tr>
                                                        <tr><td style="padding:11px 0;color:#64748b;">Montant</td><td style="padding:11px 0;color:#0f172a;font-size:18px;font-weight:800;">%s</td></tr>
                                                    </table>
                                                </td></tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding:8px 40px 12px 40px;">
                                            <a href="%s" style="display:inline-block;padding:15px 28px;border-radius:14px;background:linear-gradient(135deg,#2563eb 0%%,#1d4ed8 100%%);color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;">
                                                Payer maintenant
                                            </a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:8px 40px 34px 40px;">
                                            <p style="margin:0 0 10px 0;color:#475569;font-size:14px;line-height:1.7;">
                                                Une fois le paiement valide, revenez dans l'application pour remplir le formulaire clinique qui aidera le psychologue a evaluer votre etat.
                                            </p>
                                            <p style="margin:0;color:#0f172a;font-size:14px;font-weight:700;">Equipe EmoNado</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                safePatientName,
                safeAppointmentType,
                safePsychologueName,
                safeAppointmentDate,
                safeTimeRange,
                safeAmount,
                safePaymentUrl
        );
    }

    private Properties loadFileConfig() {
        Properties props = new Properties();

        tryLoad(props, Paths.get("config", "mail.properties"));

        try {
            URL location = EmailService.class.getProtectionDomain().getCodeSource().getLocation();
            Path classesDir = Paths.get(location.toURI());
            Path projectRoot = classesDir.getParent() == null ? null : classesDir.getParent().getParent();
            if (projectRoot != null) {
                tryLoad(props, projectRoot.resolve(Paths.get("config", "mail.properties")));
            }
        } catch (URISyntaxException | NullPointerException ignored) {
        }

        if (!hasCredentials(props)) {
            try (InputStream is = EmailService.class.getClassLoader().getResourceAsStream("mail.properties")) {
                if (is != null) {
                    props.load(is);
                    System.out.println("[EmailService] Mail config loaded from classpath.");
                }
            } catch (IOException ignored) {
            }
        }

        if (hasCredentials(props)) {
            System.out.println("[EmailService] Mail config loaded.");
        } else {
            System.err.println("[EmailService] Missing mail config. Create config/mail.properties.");
        }
        return props;
    }

    private void tryLoad(Properties props, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
            System.out.println("[EmailService] Mail config path: " + path.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[EmailService] Cannot read " + path + ": " + e.getMessage());
        }
    }

    private boolean hasCredentials(Properties props) {
        String username = normalizeValue(props.getProperty("MAIL_USERNAME", ""));
        String password = normalizePassword(props.getProperty("MAIL_PASSWORD", ""));
        return !username.isEmpty() && !password.isEmpty();
    }

    private void ensureConfigured() {
        if (senderEmail.isBlank() || senderPassword.isBlank()) {
            throw new IllegalStateException(
                    "[EmailService] Configuration manquante. "
                            + "Renseignez MAIL_USERNAME et MAIL_PASSWORD dans config/mail.properties.");
        }
    }

    private void validateRecipient(String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("[EmailService] Adresse email destinataire invalide ou vide.");
        }
        try {
            InternetAddress address = new InternetAddress(recipientEmail.trim(), true);
            address.validate();
        } catch (MessagingException e) {
            throw new IllegalArgumentException("[EmailService] Adresse email destinataire invalide: " + recipientEmail, e);
        }
    }

    private String readConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return normalizeConfigValue(key, value);
        }

        value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return normalizeConfigValue(key, value);
        }

        value = fileConfig.getProperty(key);
        if (value != null && !value.isBlank()) {
            return normalizeConfigValue(key, value);
        }

        return defaultValue;
    }

    private String normalizeConfigValue(String key, String value) {
        if ("MAIL_PASSWORD".equals(key)) {
            return normalizePassword(value);
        }
        return normalizeValue(value);
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePassword(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String fallback(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }

    private static final class SmtpAttempt {
        private final String host;
        private final String port;
        private final boolean ssl;

        private SmtpAttempt(String host, String port, boolean ssl) {
            this.host = host;
            this.port = port;
            this.ssl = ssl;
        }

        private String modeLabel() {
            return ssl ? "SSL" : "STARTTLS";
        }
    }
}
