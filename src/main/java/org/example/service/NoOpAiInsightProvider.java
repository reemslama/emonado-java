package org.example.service;

import org.example.entities.ProfilPsychologique;
import org.example.entities.RapportSessionJeu;

import java.util.Optional;

public class NoOpAiInsightProvider implements AiInsightProvider {
    @Override
    public Optional<String> enrichAnalysis(ProfilPsychologique profilPsychologique, RapportSessionJeu rapportSessionJeu) {
        return Optional.empty();
    }
}
