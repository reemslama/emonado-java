package org.example.service;

import org.example.entities.Journal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CLI utility placed in org.example.service to avoid creating new package directory.
 * Builds sample journals, computes professional stats and prints a friendly report.
 */
public class PrintMoodStats {
    public static void main(String[] args) {
        List<Journal> journals = buildSampleJournals();
        MoodStatsService.ProfessionalMoodStats s = MoodStatsService.computeProfessionalStats(journals);

        // Affichage JSON-like compact
        System.out.println("\n=== EMONADO - STATISTIQUES D'ANALYSE EMOTIONNELLE ===\n");
        System.out.println("Total journaux : " + s.total);
        System.out.println("Généré : " + s.generatedAt);
        System.out.println("Dominante : " + s.dominantMood + "  |  Stabilité : " + s.stability);
        System.out.println("\n-- Counts --");
        s.counts.forEach((k,v)-> System.out.printf("  %s : %d\n", k, v));

        System.out.println("\n-- Percentages --");
        s.percentages.forEach((k,v)-> System.out.printf("  %s : %.1f%%\n", k, v * 100));

        System.out.println("\n-- Pulse Intensity (pour animations) --");
        s.pulseIntensity.forEach((k,v)-> System.out.printf("  %s : %.2f\n", k, v));

        System.out.println("\n-- Time Series (par jour) --");
        for (Map.Entry<java.time.LocalDate, Map<String,Integer>> e : s.timeSeries.entrySet()){
            System.out.print("  " + e.getKey() + " : ");
            e.getValue().forEach((k,v)-> System.out.print(k+"="+v+" "));
            System.out.println();
        }

        System.out.println("\n-- Moving Averages (dernieres valeurs) --");
        s.movingAverages.forEach((m,map)-> {
            java.time.LocalDate last = map.keySet().stream().reduce((first, second) -> second).orElse(null);
            Double lastVal = last == null ? 0.0 : map.get(last);
            System.out.printf("  %s : %.2f (dernier)\n", m, lastVal);
        });

        System.out.println("\n-- Anomalies détectées --");
        s.anomalies.forEach((m,dates)-> System.out.println("  " + m + " : " + dates));

        // Belle fin
        System.out.println("\n=============================================");
        System.out.println("\u2728 Rapport prêt — export possible en PDF (bouton EXPORT) \u2728");
        System.out.println("Merci d'utiliser EmoNado. Prenez soin de vous. ❤️\n");
        printNiceFooter();
    }

    private static List<Journal> buildSampleJournals(){
        List<Journal> list = new ArrayList<>();
        String[] moods = new String[]{"heureux","calme","en colere","sos","heureux","calme","heureux","en colere","calme","heureux"};
        for (int i = 0; i < moods.length; i++){
            Journal j = new Journal();
            j.setId(i+1);
            j.setContenu("Entrée d'exemple numéro " + (i+1));
            j.setHumeur(moods[i]);
            j.setDateCreation(LocalDateTime.now().minusDays(moods.length - i));
            j.setUserId(1);
            j.setEtatAnalyse("OK");
            list.add(j);
        }
        return list;
    }

    private static void printNiceFooter(){
        System.out.println("  ************************************************");
        System.out.println("  *        EmoNado — Rapport professionnel       *");
        System.out.println("  *   Cercles animés, tendances & export PDF    *");
        System.out.println("  *               © EmoNado 2026                *");
        System.out.println("  ************************************************\n");
    }
}
