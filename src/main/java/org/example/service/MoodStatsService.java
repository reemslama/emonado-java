package org.example.service;

import org.example.entities.Journal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de calcul de statistiques "professionnelles" pour les humeurs.
 * Fournit : counts, percentages, séries temporelles par jour, moyennes mobiles,
 * détection d'anomalies simples et valeurs normalisées pour animations frontend.
 */
public class MoodStatsService {

    public static ProfessionalMoodStats computeProfessionalStats(List<Journal> journals) {
        ProfessionalMoodStats s = new ProfessionalMoodStats();
        s.generatedAt = LocalDateTime.now();
        s.total = journals == null ? 0 : journals.size();

        // canonical moods (can be extended)
        List<String> moods = List.of("heureux", "calme", "sos", "en colere");
        // counts initialisation
        s.counts = new LinkedHashMap<>();
        for (String m : moods) s.counts.put(m, 0);

        if (journals == null || journals.isEmpty()) {
            s.percentages = new LinkedHashMap<>();
            for (String m : moods) s.percentages.put(m, 0.0);
            s.timeSeries = Collections.emptyMap();
            s.movingAverages = Collections.emptyMap();
            s.anomalies = Collections.emptyMap();
            s.pulseIntensity = Collections.emptyMap();
            s.dominantMood = "AUCUNE";
            s.stability = "INCONNUE";
            return s;
        }

        // count occurrences and build per-day aggregation
        Map<LocalDate, Map<String, Integer>> series = new LinkedHashMap<>();
        for (Journal j : journals) {
            String raw = j.getHumeur();
            String m = raw == null ? "" : raw.toLowerCase();
            if (!s.counts.containsKey(m)) {
                // treat unknown as new key
                s.counts.putIfAbsent(m, 0);
            }
            s.counts.put(m, s.counts.getOrDefault(m, 0) + 1);

            LocalDate d = j.getDateCreation() == null ? LocalDate.now() : j.getDateCreation().toLocalDate();
            series.putIfAbsent(d, new LinkedHashMap<>());
            Map<String, Integer> day = series.get(d);
            day.put(m, day.getOrDefault(m, 0) + 1);
        }

        // normalize order for percentages based on counts map
        s.percentages = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : s.counts.entrySet()) {
            s.percentages.put(e.getKey(), s.total == 0 ? 0.0 : (double) e.getValue() / s.total);
        }

        // build continuous time series: fill missing days between min and max
        LocalDate min = series.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate max = series.keySet().stream().max(LocalDate::compareTo).orElse(LocalDate.now());
        long days = ChronoUnit.DAYS.between(min, max);
        Map<LocalDate, Map<String, Integer>> fullSeries = new LinkedHashMap<>();
        for (long i = 0; i <= days; i++) {
            LocalDate d = min.plusDays(i);
            Map<String, Integer> day = new LinkedHashMap<>();
            for (String mood : s.counts.keySet()) day.put(mood, 0);
            if (series.containsKey(d)) {
                Map<String, Integer> src = series.get(d);
                for (Map.Entry<String, Integer> e : src.entrySet()) day.put(e.getKey(), e.getValue());
            }
            fullSeries.put(d, day);
        }
        s.timeSeries = fullSeries;

        // moving averages per mood (window 7 days)
        s.movingAverages = new LinkedHashMap<>();
        int window = 7;
        for (String mood : s.counts.keySet()) {
            List<Double> ma = new ArrayList<>();
            List<LocalDate> dates = new ArrayList<>(fullSeries.keySet());
            List<Integer> values = dates.stream().map(d -> fullSeries.get(d).getOrDefault(mood, 0)).collect(Collectors.toList());
            for (int i = 0; i < values.size(); i++) {
                int start = Math.max(0, i - window + 1);
                List<Integer> windowVals = values.subList(start, i + 1);
                double avg = windowVals.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                ma.add(avg);
            }
            // store as map date->value for easier frontend mapping
            Map<LocalDate, Double> maMap = new LinkedHashMap<>();
            List<LocalDate> datesOrdered = new ArrayList<>(fullSeries.keySet());
            for (int i = 0; i < datesOrdered.size(); i++) maMap.put(datesOrdered.get(i), ma.get(i));
            s.movingAverages.put(mood, maMap);
        }

        // anomaly detection (simple: today's value > mean + 2*stddev over window)
        s.anomalies = new LinkedHashMap<>();
        for (String mood : s.counts.keySet()) {
            List<LocalDate> dates = new ArrayList<>(fullSeries.keySet());
            List<Integer> vals = dates.stream().map(d -> fullSeries.get(d).getOrDefault(mood, 0)).collect(Collectors.toList());
            List<LocalDate> anomalyDates = new ArrayList<>();
            for (int i = 0; i < vals.size(); i++) {
                int start = Math.max(0, i - window); // look-back
                List<Integer> windowVals = vals.subList(start, i + 1);
                double mean = windowVals.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                double variance = windowVals.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
                double std = Math.sqrt(variance);
                if (i > 0 && vals.get(i) > mean + 2 * std && vals.get(i) > 1) anomalyDates.add(dates.get(i));
            }
            s.anomalies.put(mood, anomalyDates);
        }

        // pulse intensity normalization for frontend animation (0.2..1.0)
        s.pulseIntensity = new LinkedHashMap<>();
        double maxPct = s.percentages.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        for (Map.Entry<String, Double> e : s.percentages.entrySet()) {
            double norm = maxPct == 0 ? 0.2 : 0.2 + 0.8 * (e.getValue() / maxPct);
            s.pulseIntensity.put(e.getKey(), norm);
        }

        // dominant & stability (reuse simple logic)
        int maxCount = 0; String domKey = "";
        for (Map.Entry<String, Integer> e : s.counts.entrySet()) {
            if (e.getValue() > maxCount) { maxCount = e.getValue(); domKey = e.getKey(); }
        }
        s.dominantMood = maxCount > 0 ? domKey.toUpperCase() : "AUCUNE";
        double ratio = (double) maxCount / s.total;
        if (domKey.equals("sos") || domKey.equals("en colere")) s.stability = "A SURVEILLER";
        else if (ratio > 0.6) s.stability = "POSITIVE";
        else s.stability = "MODEREE";

        return s;
    }

    public static class ProfessionalMoodStats {
        public int total;
        public String dominantMood;
        public String stability;
        public LocalDateTime generatedAt;

        // counts per mood (ordered map)
        public Map<String, Integer> counts;
        // percentages per mood (0..1)
        public Map<String, Double> percentages;
        // time series per day -> mood -> count
        public Map<LocalDate, Map<String, Integer>> timeSeries;
        // moving averages per mood -> date -> value
        public Map<String, Map<LocalDate, Double>> movingAverages;
        // anomalies per mood -> list of dates
        public Map<String, List<LocalDate>> anomalies;
        // normalized pulse intensity per mood (0.0..1.0 scaled for animations)
        public Map<String, Double> pulseIntensity;
    }
}
