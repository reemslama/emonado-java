package org.example.service;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHashService {
    private static final int BCRYPT_COST = 13;

    private PasswordHashService() {
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }

        String bcryptHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
        return toSymfonyPrefix(bcryptHash);
    }

    public static boolean matches(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcryptHash(storedPassword)) {
            return BCrypt.checkpw(plainPassword, toJavaPrefix(storedPassword));
        }

        return plainPassword.equals(storedPassword);
    }

    public static boolean needsRehash(String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return true;
        }

        if (!looksLikeBcryptHash(storedPassword)) {
            return true;
        }

        String normalized = toJavaPrefix(storedPassword);
        return !normalized.startsWith("$2a$" + String.format("%02d", BCRYPT_COST) + "$");
    }

    private static boolean looksLikeBcryptHash(String value) {
        return value.startsWith("$2y$") || value.startsWith("$2a$") || value.startsWith("$2b$");
    }

    private static String toSymfonyPrefix(String hash) {
        return hash.startsWith("$2a$") ? "$2y$" + hash.substring(4) : hash;
    }

    private static String toJavaPrefix(String hash) {
        return hash.startsWith("$2y$") ? "$2a$" + hash.substring(4) : hash;
    }
}
