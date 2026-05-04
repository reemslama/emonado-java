package org.example.service;

import org.example.entities.User;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordResetService {
    private static final PasswordResetService INSTANCE = new PasswordResetService();

    private final EmailService emailService = new EmailService();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> codeByEmail = new ConcurrentHashMap<>();

    public static PasswordResetService getInstance() {
        return INSTANCE;
    }

    public User prepareReset(String email) {
        User user = AuthService.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Aucun compte n'est associe a cet email.");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        emailService.sendPasswordResetCode(email, code);
        codeByEmail.put(email.toLowerCase(), code);
        return user;
    }

    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        String expected = codeByEmail.get(email.toLowerCase());
        return expected != null && expected.equals(code.trim());
    }

    public boolean resetPassword(String email, String newPassword) {
        boolean updated = AuthService.updatePasswordByEmail(email, newPassword);
        if (updated) {
            codeByEmail.remove(email.toLowerCase());
        }
        return updated;
    }
}
