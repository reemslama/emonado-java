package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.example.entities.ImageCarte;
import org.example.service.ServiceImageCarte;
import org.example.service.ServiceParticipation;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class HeatmapController {

    @FXML
    private GridPane heatmapGrid;

    @FXML
    private Label labelNoData;

    @FXML
    private ScrollPane scrollPane;

    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private final ServiceImageCarte serviceImageCarte = new ServiceImageCarte();

    @FXML
    public void initialize() {
        loadHeatmapData();
    }

    private void loadHeatmapData() {
        heatmapGrid.getChildren().clear();

        // Get heatmap data from database
        List<Object[]> heatmapData = serviceParticipation.getHeatmapData();

        if (heatmapData.isEmpty()) {
            labelNoData.setVisible(true);
            scrollPane.setVisible(false);
            return;
        }

        labelNoData.setVisible(false);
        scrollPane.setVisible(true);

        // Group data by game
        Map<String, List<Object[]>> gameMap = new LinkedHashMap<>();
        for (Object[] row : heatmapData) {
            String jeuTitre = (String) row[1];
            gameMap.computeIfAbsent(jeuTitre, k -> new ArrayList<>()).add(row);
        }

        // Find max count for color scaling
        int maxCount = 0;
        for (Object[] row : heatmapData) {
            int count = (Integer) row[2];
            if (count > maxCount) {
                maxCount = count;
            }
        }

        int row = 0;

        // Render each game section
        for (Map.Entry<String, List<Object[]>> entry : gameMap.entrySet()) {
            String jeuTitre = entry.getKey();
            List<Object[]> images = entry.getValue();

            // Game title header
            Label gameTitle = new Label(jeuTitre);
            gameTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f6f5f; -fx-padding: 10 0 5 0;");
            heatmapGrid.add(gameTitle, 0, row, 5, 1);
            row++;

            // Column headers
            Label colImage = new Label("Image");
            colImage.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            heatmapGrid.add(colImage, 0, row);

            Label colCount = new Label("Choix");
            colCount.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            heatmapGrid.add(colCount, 1, row);

            Label colPercentage = new Label("%");
            colPercentage.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            heatmapGrid.add(colPercentage, 2, row);

            Label colResult = new Label("Etat psychologique");
            colResult.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            heatmapGrid.add(colResult, 3, row);
            row++;

            // Render each image in this game
            for (Object[] rowData : images) {
                String imagePath = (String) rowData[0];
                int count = (Integer) rowData[2];
                String resultatPsy = (String) rowData[3];

                // Calculate percentage
                int totalForGame = images.stream().mapToInt(r -> (Integer) r[2]).sum();
                double percentage = (double) count / totalForGame * 100;

                // Create image view
                VBox imageBox = createImageBox(imagePath, count, maxCount);
                heatmapGrid.add(imageBox, 0, row);

                // Count label
                Label countLabel = new Label(String.valueOf(count));
                countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                countLabel.setPadding(new Insets(10, 20, 10, 20));
                heatmapGrid.add(countLabel, 1, row);

                // Percentage label
                Label percentLabel = new Label(String.format("%.1f%%", percentage));
                percentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                percentLabel.setPadding(new Insets(10, 20, 10, 20));
                heatmapGrid.add(percentLabel, 2, row);

                // Psychological result
                Label resultLabel = new Label(resultatPsy != null && !resultatPsy.isBlank() ? resultatPsy : "Non d\u00e9fini");
                resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                resultLabel.setWrapText(true);
                resultLabel.setMaxWidth(400);
                resultLabel.setPadding(new Insets(10, 20, 10, 0));
                heatmapGrid.add(resultLabel, 3, row);

                row++;
            }

            // Add spacing between games
            row++;
        }
    }

    private VBox createImageBox(String imagePath, int count, int maxCount) {
        VBox box = new VBox();
        box.setSpacing(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));

        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(90);
        imageView.setPreserveRatio(true);

        Image image = loadImage(imagePath);
        if (image != null && !image.isError()) {
            imageView.setImage(image);
        }

        // Calculate color intensity based on count (heatmap effect)
        double intensity = maxCount > 0 ? (double) count / maxCount : 0;
        String borderColor = getHeatColor(intensity);
        String borderStyle = String.format("-fx-border-color: %s; -fx-border-width: 3; -fx-border-radius: 8;", borderColor);

        box.getChildren().add(imageView);

        // Heat indicator (text-based)
        StringBuilder heatIndicator = new StringBuilder();
        for (int i = 0; i < Math.min(count, 5); i++) {
            heatIndicator.append("*");
        }
        Label heatLabel = new Label(heatIndicator.toString());
        heatLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + getHeatColor(intensity) + "; -fx-font-weight: bold;");
        box.getChildren().add(heatLabel);

        box.setStyle(borderStyle + " -fx-background-color: white; -fx-background-radius: 8;");

        return box;
    }

    private String getHeatColor(double intensity) {
        // Color gradient from light green to dark red
        if (intensity < 0.25) {
            return "#90EE90"; // Light green
        } else if (intensity < 0.5) {
            return "#FFD700"; // Gold
        } else if (intensity < 0.75) {
            return "#FF8C00"; // Dark orange
        } else {
            return "#FF4500"; // Orange red (hottest)
        }
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        String path = imagePath.trim().replace("\\", "/");

        // Try classpath
        try {
            if (path.startsWith("/")) {
                Image img = new Image(path);
                if (!img.isError()) {
                    return img;
                }
            }
            Image img = new Image("/" + path);
            if (!img.isError()) {
                return img;
            }
        } catch (Exception e) {
            // Continue with other methods
        }

        // Try via getResourceAsStream
        String[] candidates = new String[] {
                path,
                path.startsWith("/") ? path : "/" + path,
        };

        for (String candidate : candidates) {
            var stream = getClass().getResourceAsStream(candidate);
            if (stream == null && candidate.startsWith("/")) {
                stream = getClass().getResourceAsStream(candidate.substring(1));
            }
            if (stream != null) {
                try {
                    Image img = new Image(stream);
                    if (!img.isError()) {
                        return img;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Try filesystem
        try {
            String[] fsPaths = new String[] {
                    "src/main/resources" + (path.startsWith("/") ? path : "/" + path),
                    "target/classes" + (path.startsWith("/") ? path : "/" + path),
                    path.startsWith("/") ? "." + path : "./" + path,
            };
            for (String fsPath : fsPaths) {
                Path p = Paths.get(fsPath).normalize();
                if (Files.exists(p)) {
                    try (FileInputStream fis = new FileInputStream(p.toFile())) {
                        Image img = new Image(fis);
                        if (!img.isError()) {
                            return img;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
