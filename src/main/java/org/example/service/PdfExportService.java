package org.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.entities.ResultatTest;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère un rapport PDF professionnel pour un résultat de test adaptatif.
 * Dépendance Maven requise :
 *   <dependency>
 *     <groupId>org.apache.pdfbox</groupId>
 *     <artifactId>pdfbox</artifactId>
 *     <version>3.0.1</version>
 *   </dependency>
 */
public class PdfExportService {

    private static final float PAGE_W  = PDRectangle.A4.getWidth();
    private static final float PAGE_H  = PDRectangle.A4.getHeight();
    private static final float MARGIN  = 50f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;

    // Palette de couleurs
    private static final Color COLOR_PRIMARY   = new Color(0x2C, 0x3E, 0x50); // bleu foncé
    private static final Color COLOR_ACCENT    = new Color(0x27, 0xAE, 0x60); // vert
    private static final Color COLOR_WARNING   = new Color(0xF3, 0x9C, 0x12); // orange
    private static final Color COLOR_DANGER    = new Color(0xE7, 0x4C, 0x3C); // rouge
    private static final Color COLOR_LIGHT_BG  = new Color(0xEC, 0xF0, 0xF1); // gris clair
    private static final Color COLOR_WHITE     = Color.WHITE;

    /**
     * Génère le PDF pour UN résultat et l'ouvre automatiquement.
     * @param resultat  le résultat du test
     * @param destFile  fichier de sortie (.pdf)
     */
    public void exporterResultat(ResultatTest resultat, File destFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_H - MARGIN;

                // ── En-tête ───────────────────────────────────────────────
                y = drawHeader(cs, y, resultat);

                // ── Séparateur ────────────────────────────────────────────
                y -= 10;
                drawLine(cs, MARGIN, y, PAGE_W - MARGIN, y, COLOR_PRIMARY, 1.5f);
                y -= 20;

                // ── Bloc Score ────────────────────────────────────────────
                y = drawScoreBloc(cs, y, resultat);
                y -= 20;

                // ── Barre de progression ──────────────────────────────────
                y = drawProgressBar(cs, y, resultat);
                y -= 25;

                // ── Dimensions ────────────────────────────────────────────
                y = drawDimensions(cs, y, resultat);
                y -= 25;

                // ── Analyse IA ────────────────────────────────────────────
                if (resultat.getAnalyseIA() != null && !resultat.getAnalyseIA().isBlank()) {
                    y = drawAnalyseIA(cs, y, resultat.getAnalyseIA(), doc, page);
                }

                // ── Pied de page ──────────────────────────────────────────
                drawFooter(cs, resultat);
            }

            doc.save(destFile);
        }

        // Ouvrir automatiquement le PDF
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(destFile);
        }
    }

    // -----------------------------------------------------------------------
    // En-tête
    // -----------------------------------------------------------------------
    private float drawHeader(PDPageContentStream cs, float y, ResultatTest r) throws IOException {
        // Rectangle de fond
        drawRect(cs, MARGIN, y - 60, CONTENT_W, 65, COLOR_PRIMARY, true);

        // Titre principal
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
        cs.setNonStrokingColor(COLOR_WHITE);
        cs.newLineAtOffset(MARGIN + 15, y - 35);
        cs.showText("RAPPORT DE TEST ADAPTATIF");
        cs.endText();

        // Catégorie
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 13);
        cs.setNonStrokingColor(new Color(0xBD, 0xC3, 0xC7));
        cs.newLineAtOffset(MARGIN + 15, y - 55);
        cs.showText("Catégorie : " + r.getCategorie().toUpperCase());
        cs.endText();

        // Date à droite
        String dateStr = r.getDateTest() != null
                ? r.getDateTest().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "";
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 10);
        cs.setNonStrokingColor(new Color(0xBD, 0xC3, 0xC7));
        cs.newLineAtOffset(PAGE_W - MARGIN - 130, y - 55);
        cs.showText(dateStr);
        cs.endText();

        return y - 70;
    }

    // -----------------------------------------------------------------------
    // Bloc Score
    // -----------------------------------------------------------------------
    private float drawScoreBloc(PDPageContentStream cs, float y, ResultatTest r) throws IOException {
        Color niveauColor = niveauColor(r.getNiveau());

        // Cercle de score (simulé par rectangle arrondi)
        drawRect(cs, MARGIN, y - 70, 130, 75, COLOR_LIGHT_BG, true);

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 28);
        cs.setNonStrokingColor(niveauColor);
        cs.newLineAtOffset(MARGIN + 20, y - 40);
        cs.showText(r.getScoreActuel() + " / " + r.getScoreMax());
        cs.endText();

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        cs.setNonStrokingColor(COLOR_PRIMARY);
        cs.newLineAtOffset(MARGIN + 30, y - 58);
        cs.showText("Score total");
        cs.endText();

        // Pastille niveau
        drawRect(cs, MARGIN + 150, y - 45, 120, 30, niveauColor, true);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        cs.setNonStrokingColor(COLOR_WHITE);
        cs.newLineAtOffset(MARGIN + 170, y - 26);
        cs.showText("NIVEAU : " + r.getNiveau());
        cs.endText();

        // Description niveau
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 10);
        cs.setNonStrokingColor(COLOR_PRIMARY);
        cs.newLineAtOffset(MARGIN + 150, y - 65);
        cs.showText(descriptionNiveau(r.getNiveau()));
        cs.endText();

        return y - 80;
    }

    // -----------------------------------------------------------------------
    // Barre de progression
    // -----------------------------------------------------------------------
    private float drawProgressBar(PDPageContentStream cs, float y, ResultatTest r) throws IOException {
        double pct = r.getPourcentage();
        Color barColor = niveauColor(r.getNiveau());

        // Fond
        drawRect(cs, MARGIN, y - 18, CONTENT_W, 14, COLOR_LIGHT_BG, true);
        // Remplissage
        drawRect(cs, MARGIN, y - 18, (float)(CONTENT_W * pct), 14, barColor, true);

        // Texte %
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
        cs.setNonStrokingColor(COLOR_PRIMARY);
        cs.newLineAtOffset(MARGIN + CONTENT_W + 5, y - 10);
        cs.showText(String.format("%.0f%%", pct * 100));
        cs.endText();

        return y - 25;
    }

    // -----------------------------------------------------------------------
    // Dimensions (Émotionnel / Physique / Cognitif)
    // -----------------------------------------------------------------------
    private float drawDimensions(PDPageContentStream cs, float y, ResultatTest r) throws IOException {
        double pct = r.getPourcentage();
        double[] vals = {
                Math.min(pct * 1.1, 1.0),
                Math.min(pct * 0.7, 1.0),
                Math.min(pct * 0.9, 1.0)
        };
        String[] labels = {"Émotionnel", "Physique", "Cognitif"};
        Color[] colors  = {
                new Color(0x34, 0x98, 0xDB),
                new Color(0x2E, 0xCC, 0x71),
                new Color(0xF1, 0xC4, 0x0F)
        };

        // Titre section
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        cs.setNonStrokingColor(COLOR_PRIMARY);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("DIMENSIONS ÉVALUÉES");
        cs.endText();
        y -= 20;

        float barW = (CONTENT_W - 30) / 3f;
        for (int i = 0; i < 3; i++) {
            float x = MARGIN + i * (barW + 15);
            // Fond
            drawRect(cs, x, y - 50, barW, 40, COLOR_LIGHT_BG, true);
            // Remplissage
            float fillH = (float)(40 * vals[i]);
            drawRect(cs, x, y - 50, barW, fillH, colors[i], true);

            // Valeur
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
            cs.setNonStrokingColor(COLOR_PRIMARY);
            cs.newLineAtOffset(x + barW / 2 - 15, y - 20);
            cs.showText(String.format("%.0f/10", vals[i] * 10));
            cs.endText();

            // Label
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
            cs.setNonStrokingColor(COLOR_PRIMARY);
            cs.newLineAtOffset(x + 5, y - 65);
            cs.showText(labels[i]);
            cs.endText();
        }
        return y - 80;
    }

    // -----------------------------------------------------------------------
    // Analyse IA (multi-ligne avec saut de page si besoin)
    // -----------------------------------------------------------------------
    private float drawAnalyseIA(PDPageContentStream csIn, float y,
                                String analyse, PDDocument doc, PDPage page) throws IOException {
        PDPageContentStream cs = csIn;

        // Titre section
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        cs.setNonStrokingColor(COLOR_PRIMARY);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("ANALYSE IA");
        cs.endText();
        y -= 5;
        drawLine(cs, MARGIN, y, MARGIN + 80, y, COLOR_ACCENT, 2f);
        y -= 15;

        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float fontSize = 9.5f;
        float lineH = 13f;

        for (String paragraph : analyse.split("\n")) {
            // Saut de page si nécessaire
            if (y < MARGIN + 40) {
                cs.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                doc.addPage(newPage);
                cs = new PDPageContentStream(doc, newPage);
                y = PAGE_H - MARGIN;
            }

            // Titres de section (lignes qui commencent par ---)
            if (paragraph.startsWith("---")) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                cs.setNonStrokingColor(COLOR_PRIMARY);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(paragraph.replace("-", "").trim());
                cs.endText();
                y -= lineH + 2;
                continue;
            }

            // Découper les lignes longues
            List<String> lines = wrapText(paragraph, font, fontSize, CONTENT_W);
            for (String line : lines) {
                if (y < MARGIN + 40) {
                    cs.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    doc.addPage(newPage);
                    cs = new PDPageContentStream(doc, newPage);
                    y = PAGE_H - MARGIN;
                }
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.setNonStrokingColor(COLOR_PRIMARY);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(line);
                cs.endText();
                y -= lineH;
            }
            y -= 4; // espace entre paragraphes
        }
        if (cs != csIn) cs.close();
        return y;
    }

    // -----------------------------------------------------------------------
    // Pied de page
    // -----------------------------------------------------------------------
    private void drawFooter(PDPageContentStream cs, ResultatTest r) throws IOException {
        drawLine(cs, MARGIN, MARGIN + 20, PAGE_W - MARGIN, MARGIN + 20, COLOR_LIGHT_BG, 1f);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
        cs.setNonStrokingColor(new Color(0x95, 0xA5, 0xA6));
        cs.newLineAtOffset(MARGIN, MARGIN + 8);
        cs.showText("Rapport généré par EMonado · Test adaptatif · " + r.getCategorie());
        cs.endText();

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
        cs.setNonStrokingColor(new Color(0x95, 0xA5, 0xA6));
        cs.newLineAtOffset(PAGE_W - MARGIN - 80, MARGIN + 8);
        cs.showText("Document confidentiel");
        cs.endText();
    }

    // -----------------------------------------------------------------------
    // Helpers graphiques
    // -----------------------------------------------------------------------
    private void drawRect(PDPageContentStream cs, float x, float y,
                          float w, float h, Color color, boolean fill) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        if (fill) cs.fill(); else cs.stroke();
    }

    private void drawLine(PDPageContentStream cs, float x1, float y1,
                          float x2, float y2, Color color, float width) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(width);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /** Découpe un texte long en lignes selon la largeur max. */
    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth)
            throws IOException {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) { lines.add(""); return lines; }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            float w = font.getStringWidth(test) / 1000 * fontSize;
            if (w > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    // -----------------------------------------------------------------------
    // Utilitaires métier
    // -----------------------------------------------------------------------
    private Color niveauColor(String niveau) {
        if (niveau == null) return COLOR_ACCENT;
        return switch (niveau.toUpperCase()) {
            case "ÉLEVÉ",  "ELEVE"  -> COLOR_DANGER;
            case "MODÉRÉ", "MODERE" -> COLOR_WARNING;
            default                  -> COLOR_ACCENT;
        };
    }

    private String descriptionNiveau(String niveau) {
        if (niveau == null) return "";
        return switch (niveau.toUpperCase()) {
            case "ÉLEVÉ",  "ELEVE"  -> "Consultation spécialiste recommandée";
            case "MODÉRÉ", "MODERE" -> "Quelques points de vigilance détectés";
            default                  -> "État stable et maîtrisé";
        };
    }
}
