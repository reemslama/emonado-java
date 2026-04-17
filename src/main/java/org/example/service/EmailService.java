package org.example.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private static final String DEFAULT_SMTP_HOST = "smtp.gmail.com";
    private static final String DEFAULT_SMTP_PORT = "587";
    private static final String DEFAULT_SENDER_EMAIL = "reemslama21@gmail.com";
    private static final String DEFAULT_SENDER_PASSWORD = "pqvq efff ggkl qxhd";

    private final String smtpHost;
    private final String smtpPort;
    private final String senderEmail;
    private final String senderPassword;

    public EmailService() {
        this.smtpHost = readConfig("MAIL_SMTP_HOST", DEFAULT_SMTP_HOST);
        this.smtpPort = readConfig("MAIL_SMTP_PORT", DEFAULT_SMTP_PORT);
        this.senderEmail = readConfig("MAIL_USERNAME", DEFAULT_SENDER_EMAIL);
        this.senderPassword = readConfig("MAIL_PASSWORD", DEFAULT_SENDER_PASSWORD);
    }

    public void sendPasswordResetCode(String recipientEmail, String resetCode) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Reinitialisation de votre mot de passe Emonado");
            message.setText(buildResetBody(resetCode));
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Impossible d'envoyer l'email de reinitialisation : " + e.getMessage(), e);
        }
    }

    private String buildResetBody(String resetCode) {
        return "Bonjour,\n\n"
                + "Voici votre code de verification pour reinitialiser votre mot de passe Emonado : "
                + resetCode + "\n\n"
                + "Ce code est personnel. Si vous n'avez pas demande cette operation, ignorez cet email.\n\n"
                + "Equipe Emonado";
    }

    private static String readConfig(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return defaultValue;
    }
}
