package org.example.service;

import org.example.entities.Journal;
import org.example.entities.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service d'exportation PDF "Expert Dashboard" pour EmoNado.
 * Architecture Multi-pages avec graphiques circulaires (Donut Charts) géométriques.
 */
public class MoodPdfExportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Palette EmoNado officielle
    private static final float[] COLOR_PRIMARY = {0.773f, 0.875f, 0.263f}; // Vert #c5df43
    private static final float[] COLOR_DARK = {0.122f, 0.227f, 0.302f};    // Bleu foncé #1f3a4d
    private static final float[] COLOR_BG = {0.97f, 0.98f, 0.99f};         // Fond Zen
    private static final float[] COLOR_WHITE = {1.0f, 1.0f, 1.0f};
    private static final float[] COLOR_TEXT_MUTED = {0.58f, 0.63f, 0.67f};

    public void exportMoodReport(Path outputFile, User user, List<Journal> journals) throws IOException {
        MoodStats stats = buildStats(journals);
        writeMultiPagePdf(outputFile, user, stats, journals);
    }

    private void writeMultiPagePdf(Path outPath, User user, MoodStats stats, List<Journal> journals) throws IOException {
        Files.createDirectories(outPath.getParent());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        
        // --- STRUCTURE DES OBJETS PDF ---
        offsets.add(out.size()); writeAscii(out, "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        offsets.add(out.size()); writeAscii(out, "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
        
        // Page 1 : Dashboard (single page)
        offsets.add(out.size()); writeAscii(out, "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >> endobj\n");

        
        // Ressources Polices
        offsets.add(out.size()); writeAscii(out, "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >> endobj\n");
        offsets.add(out.size()); writeAscii(out, "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-BoldOblique >> endobj\n");
        offsets.add(out.size()); writeAscii(out, "6 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");

        // --- CONTENU PAGE 1 ---
        byte[] p1 = buildPage1Content(user, stats, journals).getBytes(StandardCharsets.ISO_8859_1);
        offsets.add(out.size()); writeAscii(out, "7 0 obj << /Length " + p1.length + " >> stream\n");
        out.write(p1);
        writeAscii(out, "endstream endobj\n");

        // --- TABLE DE RÉFÉRENCES (XREF) ---
        int xrefOffset = out.size();
        int objCount = offsets.size() + 1; // +1 for the free object 0
        writeAscii(out, "xref\n0 " + objCount + "\n0000000000 65535 f \n");
        for (Integer o : offsets) writeAscii(out, String.format(Locale.US, "%010d 00000 n \n", o));
        writeAscii(out, "trailer << /Size " + objCount + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF");

        Files.write(outPath, out.toByteArray());
    }

    private String buildPage1Content(User user, MoodStats stats, List<Journal> journals) {
        List<String> ops = new ArrayList<>();
        float pW = 595; float pH = 842; float margin = 40;

        addFilledRect(ops, COLOR_WHITE, 0, 0, pW, pH);

        // 1. EN-TETE (HEADER)
        float headerY = pH - 60;
        addTextCentered(ops, "F1", 28, pW/2, headerY, "EMONADO", COLOR_DARK);
        addTextCentered(ops, "F3", 12, pW/2, headerY - 20, "Rapport d'analyse emotionnelle", COLOR_TEXT_MUTED);
        addFilledRect(ops, COLOR_PRIMARY, margin, headerY - 35, pW - (margin * 2), 1.5f);

        // 2. INFORMATIONS PATIENT (Top Left)
        float infoY = headerY - 80;
        addText(ops, "F3", 11, margin, infoY, "Patient referent :", COLOR_TEXT_MUTED);
        addText(ops, "F1", 14, margin, infoY - 20, buildPatientName(user).toUpperCase(), COLOR_DARK);

        // 3. DISTRIBUTION DES ETATS EMOTIONNELS (Top Right / Centered)
        float distY = infoY;
        float distX = pW - margin - 200;
        addText(ops, "F1", 11, distX, distY, "Distribution des etats emotionnels :", COLOR_DARK);
        drawDistributionList(ops, stats.percentages, distX, distY - 25);

        // 4. DERNIERES ACTIVITES (Tableau structure)
        float tableY = infoY - 140;
        addTextCentered(ops, "F1", 13, pW/2, tableY, "DERNIERES ACTIVITES", COLOR_DARK);
        drawActivitiesTable(ops, journals, margin, tableY - 30, pW - (margin * 2));

        // 5. STATISTIQUES (Bottom blocks)
        float statsY = 120;
        float kW = (pW - (margin*2) - 40) / 3;
        drawStatBlock(ops, margin, statsY, kW, "Total journaux", stats.total + " entrees");
        drawStatBlock(ops, margin + kW + 20, statsY, kW, "Humeur dominante", formatMood(stats.dominantMood));
        drawStatBlock(ops, margin + (kW + 20) * 2, statsY, kW, "Stabilite", stats.stability);

        // 6. PIED DE PAGE (FOOTER)
        addTextCentered(ops, "F3", 9, pW/2, 30, "EMONADO Analytics System - Confidentiel", new float[]{0.7f, 0.7f, 0.7f});

        return String.join("\n", ops);
    }

    private void addTextCentered(List<String> ops, String font, int size, float cx, float y, String text, float[] rgb) {
        // Approximation simple du centrage : on recule de ~0.3 * size par caractere
        float estW = text.length() * (size * 0.5f); 
        addText(ops, font, size, cx - (estW / 2), y, text, rgb);
    }

    private void drawDistributionList(List<String> ops, Map<String, Double> percentages, float x, float y) {
        float rowH = 18f;
        int i = 0;
        for (Map.Entry<String, Double> e : percentages.entrySet()) {
            float rowY = y - (i * rowH);
            String label = formatMood(e.getKey());
            String pct = String.format(Locale.US, "%.0f%%", e.getValue() * 100);
            
            addText(ops, "F3", 10, x, rowY, label, COLOR_DARK);
            // Alignement des pourcentages a droite
            addText(ops, "F1", 10, x + 150, rowY, pct, moodColor(e.getKey()));
            i++;
        }
    }

    private void drawActivitiesTable(List<String> ops, List<Journal> journals, float x, float y, float w) {
        float h = 22f;
        // Header
        addFilledRect(ops, new float[]{0.95f, 0.95f, 0.95f}, x, y, w, h);
        addText(ops, "F1", 9, x + 5, y + 7, "Date", COLOR_DARK);
        addText(ops, "F1", 9, x + 80, y + 7, "Heure", COLOR_DARK);
        addText(ops, "F1", 9, x + 140, y + 7, "Humeur", COLOR_DARK);
        addText(ops, "F1", 9, x + 230, y + 7, "Note clinique", COLOR_DARK);

        int max = Math.min(8, journals == null ? 0 : journals.size());
        for (int i = 0; i < max; i++) {
            Journal j = journals.get(i);
            float rY = y - (h * (i + 1));
            addStrokeRect(ops, new float[]{0.9f, 0.9f, 0.9f}, x, rY, w, h);
            
            LocalDateTime dt = j.getDateCreation();
            addText(ops, "F3", 9, x + 5, rY + 7, dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), COLOR_DARK);
            addText(ops, "F3", 9, x + 80, rY + 7, dt.format(DateTimeFormatter.ofPattern("HH:mm")), COLOR_DARK);
            addText(ops, "F1", 9, x + 140, rY + 7, formatMood(j.getHumeur()), moodColor(j.getHumeur()));
            
            String note = j.getContenu() == null ? "" : j.getContenu();
            if (note.length() > 65) note = note.substring(0, 62) + "...";
            addText(ops, "F3", 8, x + 230, rY + 7, note, COLOR_TEXT_MUTED);
        }
    }

    private void drawStatBlock(List<String> ops, float x, float y, float w, String title, String value) {
        float h = 50;
        addStrokeRect(ops, new float[]{0.9f, 0.9f, 0.9f}, x, y, w, h);
        addTextCentered(ops, "F3", 9, x + w/2, y + 32, title, COLOR_TEXT_MUTED);
        addTextCentered(ops, "F1", 12, x + w/2, y + 12, value, COLOR_DARK);
    }

    private float[] moodColor(String m) {
        if (m == null) return new float[]{0.5f, 0.5f, 0.5f};
        return switch (m.toLowerCase()) {
            case "heureux", "HEUREUX" -> new float[]{0.95f, 0.85f, 0.0f}; // Jaune
            case "calme", "CALME" -> new float[]{0.0f, 0.7f, 0.3f};       // Vert
            case "sos", "SOS" -> new float[]{1.0f, 0.5f, 0.0f};           // Orange
            case "en colere", "colere", "COLERE" -> new float[]{0.9f, 0.1f, 0.1f}; // Rouge
            default -> new float[]{0.6f, 0.65f, 0.7f};
        };
    }

    private void addText(List<String> ops, String font, int size, float x, float y, String text, float[] rgb) {
        ops.add(String.format(Locale.US, "%.3f %.3f %.3f rg", rgb[0], rgb[1], rgb[2]));
        ops.add("BT /" + font + " " + size + " Tf " + String.format(Locale.US, "%.2f %.2f Td", x, y) + " (" + escape(text) + ") Tj ET");
    }

    private void addFilledRect(List<String> ops, float[] rgb, float x, float y, float w, float h) {
        ops.add(String.format(Locale.US, "%.3f %.3f %.3f rg %.2f %.2f %.2f %.2f re f", rgb[0], rgb[1], rgb[2], x, y, w, h));
    }

    private void addStrokeRect(List<String> ops, float[] rgb, float x, float y, float w, float h) {
        ops.add(String.format(Locale.US, "%.3f %.3f %.3f RG %.2f %.2f %.2f %.2f re S", rgb[0], rgb[1], rgb[2], x, y, w, h));
    }

    private String escape(String t) {
        if (t == null) return "";
        return t.replace("é","e").replace("è","e").replace("ê","e").replace("à","a").replace("î","i")
                .replace("(", "\\(").replace(")", "\\)");
    }

    private void writeAscii(ByteArrayOutputStream out, String v) throws IOException { out.write(v.getBytes(StandardCharsets.ISO_8859_1)); }

    private String buildPatientName(User u) { return u == null ? "PATIENT ANONYME" : (u.getPrenom() + " " + u.getNom()).trim(); }

    private String formatMood(String m) {
        if (m == null) return "-";
        return switch (m.toLowerCase()) {
            case "heureux" -> "Heureux"; case "calme" -> "Calme";
            case "sos" -> "Urgent/SOS"; case "en colere", "colere" -> "Colere";
            default -> m;
        };
    }

    private MoodStats buildStats(List<Journal> journals) {
        MoodStats s = new MoodStats(); s.total = journals.size(); s.generatedAt = LocalDateTime.now();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String m : List.of("heureux", "calme", "sos", "en colere")) counts.put(m, 0);
        
        int max = 0; String domKey = "";
        if (journals != null) {
            for (Journal j : journals) {
                String m = j.getHumeur() != null ? j.getHumeur().toLowerCase() : "";
                if (counts.containsKey(m)) {
                    int newVal = counts.get(m) + 1;
                    counts.put(m, newVal);
                    if (newVal > max) { max = newVal; domKey = m; }
                } else if (!m.isEmpty()) {
                    counts.put(m, counts.getOrDefault(m, 0) + 1);
                    if (counts.get(m) > max) { max = counts.get(m); domKey = m; }
                }
            }
        }
        
        // percentages
        Map<String, Double> percentages = new LinkedHashMap<>();
        for (Map.Entry<String,Integer> e : counts.entrySet()) {
            percentages.put(e.getKey(), s.total == 0 ? 0.0 : (double) e.getValue() / s.total);
        }

        // pulse intensity normalization (0.4..1.0)
        Map<String, Double> pulse = new LinkedHashMap<>();
        double maxPct = percentages.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        for (Map.Entry<String, Double> e : percentages.entrySet()) {
            double norm = maxPct == 0 ? 0.4 : 0.4 + 0.6 * (e.getValue() / maxPct);
            pulse.put(e.getKey(), norm);
        }

        s.counts = counts;
        s.percentages = percentages;
        s.pulseIntensity = pulse;
        s.dominantMood = max > 0 ? domKey.toUpperCase() : "AUCUNE";
        
        if (s.total == 0) s.stability = "INCONNUE";
        else {
            double ratio = (double) max / s.total;
            if (domKey.equals("sos") || domKey.equals("en colere")) s.stability = "A SURVEILLER";
            else if (ratio > 0.6) s.stability = "POSITIVE";
            else s.stability = "MODEREE";
        }
        return s;
    }

    private static class MoodStats {
        int total; String dominantMood; String stability; LocalDateTime generatedAt;
        Map<String,Integer> counts; Map<String,Double> percentages; Map<String,Double> pulseIntensity;
    }
}
