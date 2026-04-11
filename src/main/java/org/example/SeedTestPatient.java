package org.example;

import org.example.entities.User;
import org.example.service.AuthService;

import java.time.LocalDate;

/**
 * Utilitaire ponctuel : crée un compte patient de test (lancer une fois depuis l'IDE ou la ligne de commande).
 */
public final class SeedTestPatient {
    public static void main(String[] args) {
        User u = new User();
        u.setNom("Test");
        u.setPrenom("Patient");
        u.setEmail("patient.test@emonado.local");
        u.setPassword("Test123!");
        u.setTelephone("12345678");
        u.setSexe("Homme");
        u.setDateNaissance(LocalDate.of(1999, 2, 2));

        String err = AuthService.addPatient(u);
        if (err != null) {
            System.err.println("ECHEC: " + err);
            System.exit(1);
        }
        System.out.println("OK — compte créé.");
        System.out.println("Email: patient.test@emonado.local");
        System.out.println("Mot de passe: Test123!");
    }
}
