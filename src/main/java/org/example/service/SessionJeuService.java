package org.example.service;

import org.example.entities.Participation;
import org.example.entities.ProfilPsychologique;
import org.example.entities.RapportSessionJeu;
import org.example.entities.User;

import java.time.LocalDateTime;
import java.util.List;

public class SessionJeuService {
    private final ServiceParticipation serviceParticipation;
    private final AnalyseComportementaleService analyseComportementaleService;
    private final EmailService emailService;
    private final AiInsightProvider aiInsightProvider;

    public SessionJeuService() {
        this(new ServiceParticipation(), new AnalyseComportementaleService(), new EmailService(), createAiProvider());
    }

    public SessionJeuService(ServiceParticipation serviceParticipation,
                             AnalyseComportementaleService analyseComportementaleService,
                             EmailService emailService,
                             AiInsightProvider aiInsightProvider) {
        this.serviceParticipation = serviceParticipation;
        this.analyseComportementaleService = analyseComportementaleService;
        this.emailService = emailService;
        this.aiInsightProvider = aiInsightProvider;
    }

    public RapportSessionJeu genererRapportSession(int userId) {
        List<Participation> participations = serviceParticipation.findByUserId(userId);
        ProfilPsychologique profil = analyseComportementaleService.analyser(participations);
        RapportSessionJeu rapport = new RapportSessionJeu(
                profil,
                buildAnalyse(profil, participations),
                buildConseils(profil),
                participations,
                LocalDateTime.now(),
                buildResumeSession(profil, participations)
        );
        String enrichissement = aiInsightProvider.enrichAnalysis(profil, rapport).orElse("");
        if (!enrichissement.isBlank()) {
            return new RapportSessionJeu(
                    profil,
                    rapport.getAnalyseDetaillee() + "\n\nAnalyse IA complementaire:\n" + enrichissement,
                    rapport.getConseilsParent(),
                    participations,
                    rapport.getDateGeneration(),
                    rapport.getResumeSession()
            );
        }
        return rapport;
    }

    public void envoyerRapportParent(User parent, RapportSessionJeu rapport) {
        if (parent == null || parent.getEmail() == null || parent.getEmail().isBlank()) {
            return;
        }
        emailService.sendSessionReport(parent.getEmail(), parent.getPrenom(), rapport);
    }

    private String buildAnalyse(ProfilPsychologique profil, List<Participation> participations) {
        return "Profil detecte: " + profil.getProfil() + ". "
                + "Score emotionnel: " + profil.getScoreEmotionnel() + "/100. "
                + "Tendance principale: " + profil.getTendance() + ". "
                + "Anomalie: " + profil.getAnomalie() + ". "
                + "Synthese: " + profil.getSyntheseClinique() + " "
                + "Participations analysees: " + participations.size() + ".";
    }

    private String buildConseils(ProfilPsychologique profil) {
        if (profil.getStress() >= 7) {
            return "Privilegier des situations rassurantes, ritualiser les transitions et observer si les choix de peur persistent.";
        }
        if (profil.getTimidite() > profil.getSociabilite()) {
            return "Favoriser des jeux progressifs a deux, avec encouragements brefs et modelisation calme.";
        }
        if (profil.getSociabilite() >= profil.getTimidite()) {
            return "Proposer des activites collaboratives courtes pour renforcer les interactions positives.";
        }
        return "Poursuivre les observations sur plusieurs sessions pour confirmer la stabilite du profil.";
    }

    private String buildResumeSession(ProfilPsychologique profil, List<Participation> participations) {
        long decisionsRapides = participations.stream().filter(Participation::isDecisionRapide).count();
        long decisionsLentes = participations.stream().filter(Participation::isDecisionLente).count();
        return "Choix visuels: " + participations.size()
                + ", decisions rapides: " + decisionsRapides
                + ", decisions lentes: " + decisionsLentes
                + ", score emotionnel: " + profil.getScoreEmotionnel() + "/100.";
    }

    private static AiInsightProvider createAiProvider() {
        String apiKey = System.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new NoOpAiInsightProvider();
        }
        return new OpenAiInsightProvider(apiKey);
    }
}
