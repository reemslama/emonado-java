package org.example.service;

import org.example.entities.User;
import org.example.utils.DataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FaceProfileService {
    private static final Path FACE_ID_DIRECTORY = Path.of("user_data", "faceid");
    private static final DateTimeFormatter FILE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static volatile boolean schemaChecked;

    private FaceProfileService() {
    }

    public static synchronized void ensureSchema() throws SQLException {
        if (schemaChecked) {
            return;
        }

        try (Connection conn = DataSource.getInstance().getConnection()) {
            if (!columnExists(conn, "avatar")) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "ALTER TABLE user ADD COLUMN avatar VARCHAR(40) NULL")) {
                    pstmt.executeUpdate();
                }
            }

            if (!columnExists(conn, "face_id_image_path")) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "ALTER TABLE user ADD COLUMN face_id_image_path VARCHAR(255) NULL")) {
                    pstmt.executeUpdate();
                }
            }
        }

        schemaChecked = true;
    }

    public static void savePreferences(User user, String avatar, Path selectedFaceImage) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Utilisateur invalide pour la configuration Face ID.");
        }

        try {
            ensureSchema();
            String storedImagePath = copyFaceImage(user.getId(), selectedFaceImage);

            try (Connection conn = DataSource.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE user SET avatar = ?, face_id_image_path = ? WHERE id = ?")) {
                pstmt.setString(1, normalizeAvatar(avatar));
                pstmt.setString(2, storedImagePath);
                pstmt.setInt(3, user.getId());
                pstmt.executeUpdate();
            }

            user.setAvatar(normalizeAvatar(avatar));
            user.setFaceIdImagePath(storedImagePath);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'enregistrer l'avatar ou la photo Face ID : " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de copier la photo Face ID : " + e.getMessage(), e);
        }
    }

    public static String normalizeAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        return avatar.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean columnExists(Connection conn, String columnName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = ?")) {
            pstmt.setString(1, columnName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static String copyFaceImage(int userId, Path selectedFaceImage) throws IOException {
        if (selectedFaceImage == null) {
            return null;
        }

        Files.createDirectories(FACE_ID_DIRECTORY);
        String extension = getExtension(selectedFaceImage.getFileName().toString());
        String fileName = "user_" + userId + "_" + FILE_SUFFIX.format(LocalDateTime.now()) + extension;
        Path destination = FACE_ID_DIRECTORY.resolve(fileName);
        Files.copy(selectedFaceImage, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toAbsolutePath().toString();
    }

    private static String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return ".png";
        }
        return fileName.substring(index);
    }
}
