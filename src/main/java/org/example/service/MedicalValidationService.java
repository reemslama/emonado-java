package org.example.service;

import org.example.entities.AntecedentMedical;
import org.example.entities.Consultation;
import org.example.entities.DossierMedical;
import org.example.entities.User;

import java.time.LocalDate;
import java.util.regex.Pattern;

public final class MedicalValidationService {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ' -]{2,50}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern INVALID_TEXT_PATTERN = Pattern.compile("^[\\p{Punct}\\s]+$");

    private static final int REMINDER_MAX_LENGTH = 500;
    private static final int HISTORY_MAX_LENGTH = 3000;
    private static final int ANTECEDENT_TYPE_MAX_LENGTH = 100;
    private static final int ANTECEDENT_DESCRIPTION_MAX_LENGTH = 1000;
    private static final int CONSULTATION_NOTES_MAX_LENGTH = 2000;
    private static final int PSY_NOTE_MAX_LENGTH = 2000;

    private MedicalValidationService() {
    }

    public static String validatePatientProfile(User user) {
        String nom = normalize(user.getNom());
        String prenom = normalize(user.getPrenom());
        String email = normalize(user.getEmail());
        String telephone = normalize(user.getTelephone()).replaceAll("\\D", "");
        String sexe = normalize(user.getSexe());
        LocalDate birthDate = user.getDateNaissance();

        if (!NAME_PATTERN.matcher(nom).matches()) {
            return "Le nom doit contenir entre 2 et 50 caracteres alphabetiques.";
        }
        if (!NAME_PATTERN.matcher(prenom).matches()) {
            return "Le prenom doit contenir entre 2 et 50 caracteres alphabetiques.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Le format de l'email est invalide.";
        }
        if (!PHONE_PATTERN.matcher(telephone).matches()) {
            return "Le telephone doit contenir entre 8 et 15 chiffres.";
        }
        if (!sexe.isBlank() && !sexe.equalsIgnoreCase("Homme") && !sexe.equalsIgnoreCase("Femme")) {
            return "Le sexe doit etre 'Homme' ou 'Femme'.";
        }
        if (birthDate == null) {
            return "Veuillez saisir une date de naissance.";
        }
        if (birthDate.isAfter(LocalDate.now())) {
            return "La date de naissance ne peut pas etre dans le futur.";
        }
        if (birthDate.isAfter(LocalDate.now().minusYears(5))) {
            return "La date de naissance semble invalide.";
        }
        return null;
    }

    public static String validateDossier(DossierMedical dossierMedical) {
        String reminderText = normalize(dossierMedical.getReminderText());
        String historyText = normalize(dossierMedical.getMedicalHistory());

        if (!reminderText.isBlank()) {
            if (reminderText.length() < 5) {
                return "Le rappel doit contenir au moins 5 caracteres.";
            }
            if (reminderText.length() > REMINDER_MAX_LENGTH) {
                return "Le rappel ne doit pas depasser 500 caracteres.";
            }
            if (INVALID_TEXT_PATTERN.matcher(reminderText).matches()) {
                return "Le rappel doit contenir du texte explicite.";
            }
        }

        if (!historyText.isBlank()) {
            if (historyText.length() < 10) {
                return "L'historique medical doit contenir au moins 10 caracteres.";
            }
            if (historyText.length() > HISTORY_MAX_LENGTH) {
                return "L'historique medical ne doit pas depasser 3000 caracteres.";
            }
            if (INVALID_TEXT_PATTERN.matcher(historyText).matches()) {
                return "L'historique medical doit contenir du texte explicite.";
            }
        }

        if (reminderText.isBlank() && historyText.isBlank()) {
            return "Le dossier medical doit contenir au moins un rappel ou un historique.";
        }
        return null;
    }

    public static String validateAntecedent(AntecedentMedical antecedentMedical) {
        String type = normalize(antecedentMedical.getType());
        String description = normalize(antecedentMedical.getDescription());
        LocalDate dateDiagnostic = antecedentMedical.getDateDiagnostic();

        if (type.isBlank()) {
            return "Veuillez saisir le type de l'antecedent medical.";
        }
        if (type.length() > ANTECEDENT_TYPE_MAX_LENGTH) {
            return "Le type de l'antecedent ne doit pas depasser 100 caracteres.";
        }
        if (INVALID_TEXT_PATTERN.matcher(type).matches()) {
            return "Le type de l'antecedent doit contenir des lettres ou des chiffres.";
        }
        if (description.length() < 5) {
            return "Veuillez saisir une description plus precise pour l'antecedent.";
        }
        if (description.length() > ANTECEDENT_DESCRIPTION_MAX_LENGTH) {
            return "La description de l'antecedent ne doit pas depasser 1000 caracteres.";
        }
        if (INVALID_TEXT_PATTERN.matcher(description).matches()) {
            return "La description de l'antecedent doit contenir du texte explicite.";
        }
        if (dateDiagnostic != null && dateDiagnostic.isAfter(LocalDate.now())) {
            return "La date du diagnostic ne peut pas etre dans le futur.";
        }
        return null;
    }

    public static String validateConsultationPatientData(Consultation consultation) {
        LocalDate consultationDate = consultation.getConsultationDate();
        String notesPatient = normalize(consultation.getNotesPatient());

        if (consultationDate == null) {
            return "Veuillez choisir une date de consultation.";
        }
        if (notesPatient.isBlank()) {
            return "Le compte rendu est obligatoire.";
        }
        if (notesPatient.length() < 10) {
            return "Le compte rendu doit contenir au moins 10 caracteres.";
        }
        if (notesPatient.length() > CONSULTATION_NOTES_MAX_LENGTH) {
            return "Le compte rendu ne doit pas depasser 2000 caracteres.";
        }
        if (INVALID_TEXT_PATTERN.matcher(notesPatient).matches()) {
            return "Le compte rendu doit contenir un contenu explicite.";
        }
        return null;
    }

    public static String validatePsychologueNote(String psychologueNote) {
        String normalizedNote = normalize(psychologueNote);
        if (normalizedNote.isBlank()) {
            return "La note psychologue est obligatoire.";
        }
        if (normalizedNote.length() < 5) {
            return "La note psychologue doit contenir au moins 5 caracteres.";
        }
        if (normalizedNote.length() > PSY_NOTE_MAX_LENGTH) {
            return "La note psychologue ne doit pas depasser 2000 caracteres.";
        }
        if (INVALID_TEXT_PATTERN.matcher(normalizedNote).matches()) {
            return "La note psychologue doit contenir du texte explicite.";
        }
        return null;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
