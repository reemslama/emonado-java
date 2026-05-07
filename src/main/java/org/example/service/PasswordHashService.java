package org.example.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordHashService {
    private static final BCryptPasswordEncoder ENCODER =
            new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2Y, 12);

    private PasswordHashService() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est vide.");
        }
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    public static boolean isHashed(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}$");
    }

    public static String ensureHashed(String password) {
        return isHashed(password) ? password : hash(password);
    }
}
