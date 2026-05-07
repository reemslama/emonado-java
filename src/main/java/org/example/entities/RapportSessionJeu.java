package org.example.entities;

import java.time.LocalDateTime;
import java.util.List;

public class RapportSessionJeu {
    private final ProfilPsychologique profilPsychologique;
    private final String analyseDetaillee;
    private final String conseilsParent;
    private final List<Participation> participations;
    private final LocalDateTime dateGeneration;
    private final String resumeSession;

    public RapportSessionJeu(ProfilPsychologique profilPsychologique,
                             String analyseDetaillee,
                             String conseilsParent,
                             List<Participation> participations,
                             LocalDateTime dateGeneration,
                             String resumeSession) {
        this.profilPsychologique = profilPsychologique;
        this.analyseDetaillee = analyseDetaillee;
        this.conseilsParent = conseilsParent;
        this.participations = participations;
        this.dateGeneration = dateGeneration;
        this.resumeSession = resumeSession;
    }

    public ProfilPsychologique getProfilPsychologique() {
        return profilPsychologique;
    }

    public String getAnalyseDetaillee() {
        return analyseDetaillee;
    }

    public String getConseilsParent() {
        return conseilsParent;
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public LocalDateTime getDateGeneration() {
        return dateGeneration;
    }

    public String getResumeSession() {
        return resumeSession;
    }
}
