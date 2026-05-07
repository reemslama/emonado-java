package org.example.service;

import org.example.entities.ProfilPsychologique;
import org.example.entities.RapportSessionJeu;

import java.util.Optional;

public interface AiInsightProvider {
    Optional<String> enrichAnalysis(ProfilPsychologique profilPsychologique, RapportSessionJeu rapportSessionJeu);
}
