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

public class MoodPdfExportService {
    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void exportMoodReport(Path outputFile, User user, List<Journal> journals) throws IOException {
        if (outputFile == null) {
            throw new IOException("Fichier cible introuvable.");
        }
        if (journals == null || journals.isEmpty()) {
            throw new IOException("Aucun journal a exporter.");
        }

        MoodStats stats = buildStats(journals);
        String content = buildContent(user, stats);
        writePdf(outputFile, content);
    }

    private MoodStats buildStats(List<Journal> journals) {
        MoodStats stats = new MoodStats();
        stats.total = journals.size();
        stats.generatedAt = LocalDateTime.now();

        for (String mood : List.of("heureux", "calme", "SOS", "en colere")) {
            stats.counts.put(mood, 0);
        }

        LocalDateTime latest = null;
        for (Journal journal : journals) {
            String mood = journal.getHumeur();
            if (mood != null && stats.counts.containsKey(mood)) {
                stats.counts.put(mood, stats.counts.get(mood) + 1);
            }
            if (journal.getDateCreation() != null && (latest == null || journal.getDateCreation().isAfter(latest))) {
                latest = journal.getDateCreation();
            }
        }

        stats.latestDate = latest != null ? latest : stats.generatedAt;
        stats.dominantMood = findDominantMood(stats.counts);
        return stats;
    }

    private String findDominantMood(Map<String, Integer> counts) {
        String dominant = "Aucune";
        int max = -1;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                dominant = formatMood(entry.getKey());
            }
        }
        return dominant;
    }

    private String buildContent(User user, MoodStats stats) {
        List<String> ops = new ArrayList<>();

        float pageWidth = 595f;
        float pageHeight = 842f;
        float margin = 24f;

        addFilledRect(ops, 0.93f, 0.96f, 0.99f, margin, pageHeight - 785f, pageWidth - (margin * 2), 760f);
        addStrokeRect(ops, 0.82f, 0.88f, 0.95f, margin, pageHeight - 785f, pageWidth - (margin * 2), 760f);

        addFilledRect(ops, 0.95f, 0.97f, 1f, 32f, pageHeight - 130f, 531f, 100f);
        addStrokeRect(ops, 0.82f, 0.88f, 0.95f, 32f, pageHeight - 130f, 531f, 100f);

        addText(ops, "F1", 18, 52, pageHeight - 72, "EmoNado");
        addText(ops, "F2", 10, 52, pageHeight - 88, "Rapport Professionnel des Humeurs");
        addText(ops, "F3", 9, 52, pageHeight - 108,
                "Patient: " + buildPatientName(user) + " | Genere le " + stats.generatedAt.format(PDF_DATE_FORMAT));

        float cardY = pageHeight - 220f;
        addStatCard(ops, 40f, cardY, 160f, 62f, "Total journaux", String.valueOf(stats.total));
        addStatCard(ops, 218f, cardY, 160f, 62f, "Humeur dominante", stats.dominantMood);
        addStatCard(ops, 396f, cardY, 160f, 62f, "Derniere mise a jour", stats.latestDate.format(SHORT_DATE_FORMAT));

        addFilledRect(ops, 1f, 1f, 1f, 40f, pageHeight - 470f, 515f, 210f);
        addStrokeRect(ops, 0.82f, 0.88f, 0.95f, 40f, pageHeight - 470f, 515f, 210f);
        addText(ops, "F2", 14, 52, pageHeight - 290f, "Distribution visuelle");

        float startBarY = pageHeight - 325f;
        int index = 0;
        for (Map.Entry<String, Integer> entry : stats.counts.entrySet()) {
            String label = formatMood(entry.getKey());
            int count = entry.getValue();
            double percentage = stats.total == 0 ? 0d : (count * 100d / stats.total);
            float y = startBarY - (index * 34f);

            addText(ops, "F3", 10, 52, y + 12, label + " - " + count + " (" + formatPercent(percentage) + ")");
            addFilledRect(ops, 0.90f, 0.92f, 0.95f, 52f, y - 2, 460f, 8f);

            float[] color = moodColor(entry.getKey());
            float width = (float) (460f * Math.max(0d, Math.min(1d, percentage / 100d)));
            addFilledRect(ops, color[0], color[1], color[2], 52f, y - 2, width, 8f);
            index++;
        }

        addFilledRect(ops, 1f, 1f, 1f, 40f, pageHeight - 690f, 515f, 170f);
        addStrokeRect(ops, 0.82f, 0.88f, 0.95f, 40f, pageHeight - 690f, 515f, 170f);
        addText(ops, "F2", 14, 52, pageHeight - 510f, "Tableau recapitulatif");
        drawTable(ops, stats, 52f, pageHeight - 545f);

        // Footer intentionally minimal to avoid a "noisy" render.
        addText(ops, "F3", 8, 40f, 44f, "EmoNado - Rapport humeurs");
        return String.join("\n", ops) + "\n";
    }

    private void addStatCard(List<String> ops, float x, float y, float width, float height, String title, String value) {
        addFilledRect(ops, 1f, 1f, 1f, x, y, width, height);
        addStrokeRect(ops, 0.82f, 0.88f, 0.95f, x, y, width, height);
        addText(ops, "F3", 9, x + 12, y + height - 18, title);
        addText(ops, "F1", 16, x + 12, y + 18, value);
    }

    private void drawTable(List<String> ops, MoodStats stats, float x, float topY) {
        float[] widths = {150f, 150f, 180f};
        float rowHeight = 24f;
        String[][] rows = new String[stats.counts.size() + 2][3];
        rows[0] = new String[]{"Humeur", "Nombre", "Pourcentage"};

        int rowIndex = 1;
        for (Map.Entry<String, Integer> entry : stats.counts.entrySet()) {
            double percentage = stats.total == 0 ? 0d : (entry.getValue() * 100d / stats.total);
            rows[rowIndex++] = new String[]{
                    formatMood(entry.getKey()),
                    String.valueOf(entry.getValue()),
                    formatPercent(percentage)
            };
        }
        rows[rowIndex] = new String[]{"Total", String.valueOf(stats.total), "100%"};

        for (int i = 0; i < rows.length; i++) {
            float rowY = topY - (i * rowHeight);
            if (i == 0) {
                addFilledRect(ops, 0.91f, 0.95f, 0.97f, x, rowY - rowHeight, widths[0] + widths[1] + widths[2], rowHeight);
            }
            float currentX = x;
            for (int j = 0; j < widths.length; j++) {
                addStrokeRect(ops, 0.82f, 0.88f, 0.95f, currentX, rowY - rowHeight, widths[j], rowHeight);
                addText(ops, i == 0 || i == rows.length - 1 ? "F2" : "F3", 10, currentX + 8, rowY - 16, rows[i][j]);
                currentX += widths[j];
            }
        }
    }

    private void writePdf(Path outputFile, String content) throws IOException {
        Files.createDirectories(outputFile.toAbsolutePath().getParent());

        List<Integer> offsets = new ArrayList<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeAscii(out, "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");

        offsets.add(out.size());
        writeAscii(out, "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");

        offsets.add(out.size());
        writeAscii(out, "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >> endobj\n");

        offsets.add(out.size());
        writeAscii(out, "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >> endobj\n");

        offsets.add(out.size());
        writeAscii(out, "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-BoldOblique >> endobj\n");

        offsets.add(out.size());
        writeAscii(out, "6 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");

        byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
        offsets.add(out.size());
        writeAscii(out, "7 0 obj << /Length " + contentBytes.length + " >> stream\n");
        out.write(contentBytes);
        writeAscii(out, "endstream endobj\n");

        int xrefOffset = out.size();
        writeAscii(out, "xref\n0 8\n");
        writeAscii(out, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            writeAscii(out, String.format(Locale.US, "%010d 00000 n \n", offset));
        }
        writeAscii(out, "trailer << /Size 8 /Root 1 0 R >>\n");
        writeAscii(out, "startxref\n" + xrefOffset + "\n%%EOF");

        Files.write(outputFile, out.toByteArray());
    }

    private void addText(List<String> ops, String font, int size, float x, float y, String text) {
        ops.add("BT");
        ops.add("/" + font + " " + size + " Tf");
        ops.add(String.format(Locale.US, "%.2f %.2f Td", x, y));
        ops.add("(" + escape(text) + ") Tj");
        ops.add("ET");
    }

    private void addFilledRect(List<String> ops, float r, float g, float b, float x, float y, float width, float height) {
        ops.add(String.format(Locale.US, "%.3f %.3f %.3f rg", r, g, b));
        ops.add(String.format(Locale.US, "%.2f %.2f %.2f %.2f re f", x, y, width, height));
    }

    private void addStrokeRect(List<String> ops, float r, float g, float b, float x, float y, float width, float height) {
        ops.add(String.format(Locale.US, "%.3f %.3f %.3f RG", r, g, b));
        ops.add(String.format(Locale.US, "%.2f %.2f %.2f %.2f re S", x, y, width, height));
    }

    private String buildPatientName(User user) {
        if (user == null) {
            return "Patient inconnu";
        }
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? "Patient inconnu" : fullName;
    }

    private String formatMood(String mood) {
        if (mood == null || mood.isBlank()) {
            return "Aucune";
        }
        return switch (mood) {
            case "SOS" -> "SOS";
            case "en colere" -> "En colere";
            default -> mood.substring(0, 1).toUpperCase() + mood.substring(1);
        };
    }

    private String formatPercent(double percentage) {
        return String.format(Locale.US, "%.1f%%", percentage);
    }

    private float[] moodColor(String mood) {
        return switch (mood) {
            case "heureux" -> new float[]{0.18f, 0.73f, 0.44f};
            case "calme" -> new float[]{0.26f, 0.55f, 0.95f};
            case "SOS" -> new float[]{0.98f, 0.61f, 0.28f};
            case "en colere" -> new float[]{0.93f, 0.28f, 0.36f};
            default -> new float[]{0.55f, 0.60f, 0.68f};
        };
    }

    private String escape(String text) {
        return (text == null ? "" : text)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private void writeAscii(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static class MoodStats {
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private int total;
        private String dominantMood;
        private LocalDateTime latestDate;
        private LocalDateTime generatedAt;
    }
}
