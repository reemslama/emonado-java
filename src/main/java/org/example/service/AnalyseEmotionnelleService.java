package org.example.service;

import org.example.entities.AnalyseEmotionnelle;
import org.example.entities.JournalAnalyseRow;
import org.example.entities.User;
import org.example.utils.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AnalyseEmotionnelleService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public List<JournalAnalyseRow> findRowsByUser(User user)
            throws SQLException {

        String sql = """
                SELECT
                    j.id AS journal_id,
                    j.humeur,
                    j.contenu,
                    j.date_creation,

                    a.id AS analyse_id,

                    a.emotion_principale AS etat_emotionnel,

                    a.niveau_stress AS niveau,

                    a.resume_ia AS conseil,

                    a.date_analyse

                FROM journal j

                LEFT JOIN analyse_emotionnelle a
                ON a.journal_id = j.id

                WHERE j.user_id = ?

                ORDER BY j.date_creation DESC
                """;

        List<JournalAnalyseRow> rows =
                new ArrayList<>();

        try (
                Connection connection =
                        DataSource.getInstance().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, user.getId());

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    JournalAnalyseRow row =
                            new JournalAnalyseRow();

                    row.setJournalId(
                            rs.getInt("journal_id")
                    );

                    row.setHumeur(
                            rs.getString("humeur")
                    );

                    String contenu =
                            rs.getString("contenu");

                    row.setContenuComplet(contenu);

                    row.setContenuResume(
                            buildSummary(contenu)
                    );

                    Timestamp journalTimestamp =
                            rs.getTimestamp(
                                    "date_creation"
                            );

                    row.setDateJournal(
                            journalTimestamp == null
                                    ? ""
                                    : journalTimestamp
                                      .toLocalDateTime()
                                      .format(DATE_FORMATTER)
                    );

                    int analyseId =
                            rs.getInt("analyse_id");

                    if (!rs.wasNull()) {

                        AnalyseEmotionnelle analyse =
                                new AnalyseEmotionnelle();

                        analyse.setId(analyseId);

                        analyse.setJournalId(
                                rs.getInt("journal_id")
                        );

                        analyse.setEtatEmotionnel(
                                rs.getString(
                                        "etat_emotionnel"
                                )
                        );

                        analyse.setNiveau(
                                mapStressToNiveau(
                                        rs.getString("niveau")
                                )
                        );

                        analyse.setConseil(
                                rs.getString("conseil")
                        );

                        Timestamp analyseTimestamp =
                                rs.getTimestamp(
                                        "date_analyse"
                                );

                        if (analyseTimestamp != null) {

                            analyse.setDateAnalyse(
                                    analyseTimestamp
                                            .toLocalDateTime()
                            );
                        }

                        row.setAnalyseEmotionnelle(
                                analyse
                        );

                        row.setEtatEmotionnel(
                                analyse.getEtatEmotionnel()
                        );

                        row.setNiveau(
                                analyse.getNiveau()
                        );

                        row.setStatut("Analysee");

                    } else {

                        row.setEtatEmotionnel(
                                "Aucune"
                        );

                        row.setNiveau("-");

                        row.setStatut(
                                "En attente"
                        );
                    }

                    rows.add(row);
                }
            }
        }

        return rows;
    }

    public int countTodayJournals(User user)
            throws SQLException {

        String sql =
                "SELECT COUNT(id) FROM journal WHERE user_id = ? AND DATE(date_creation) = ?";

        try (
                Connection connection =
                        DataSource.getInstance().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, user.getId());

            statement.setDate(
                    2,
                    Date.valueOf(LocalDate.now())
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next()
                        ? rs.getInt(1)
                        : 0;
            }
        }
    }

    public int countAnalysed(User user)
            throws SQLException {

        String sql = """
                SELECT COUNT(a.id)

                FROM analyse_emotionnelle a

                INNER JOIN journal j
                ON j.id = a.journal_id

                WHERE j.user_id = ?
                """;

        try (
                Connection connection =
                        DataSource.getInstance().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, user.getId());

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next()
                        ? rs.getInt(1)
                        : 0;
            }
        }
    }

    public void create(AnalyseEmotionnelle analyse)
            throws SQLException {

        String sql = """
                INSERT INTO analyse_emotionnelle
                (
                    journal_id,
                    emotion_principale,
                    niveau_stress,
                    score_bien_etre,
                    resume_ia,
                    date_analyse
                )

                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        DataSource.getInstance().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setInt(
                    1,
                    analyse.getJournalId()
            );

            statement.setString(
                    2,
                    analyse.getEtatEmotionnel()
            );

            statement.setInt(
                    3,
                    mapNiveauToStress(
                            analyse.getNiveau()
                    )
            );

            statement.setInt(4, 0);

            statement.setString(
                    5,
                    analyse.getConseil()
            );

            statement.setTimestamp(
                    6,
                    Timestamp.valueOf(
                            analyse.getDateAnalyse()
                    )
            );

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {

                    analyse.setId(
                            keys.getInt(1)
                    );
                }
            }
        }
    }

    public void update(AnalyseEmotionnelle analyse)
            throws SQLException {

        String sql = """
                UPDATE analyse_emotionnelle

                SET
                    emotion_principale = ?,
                    niveau_stress = ?,
                    resume_ia = ?,
                    date_analyse = ?

                WHERE id = ?
                """;

        try (
                Connection connection =
                        DataSource.getInstance().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    analyse.getEtatEmotionnel()
            );

            statement.setInt(
                    2,
                    mapNiveauToStress(
                            analyse.getNiveau()
                    )
            );

            statement.setString(
                    3,
                    analyse.getConseil()
            );

            statement.setTimestamp(
                    4,
                    Timestamp.valueOf(
                            analyse.getDateAnalyse()
                    )
            );

            statement.setInt(
                    5,
                    analyse.getId()
            );

            statement.executeUpdate();
        }
    }

    public void delete(int analyseId, int journalId)
            throws SQLException {

        String sql =
                "DELETE FROM analyse_emotionnelle WHERE id = ?";

        try (
                Connection connection =
                        DataSource.getInstance().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, analyseId);

            statement.executeUpdate();
        }
    }

    public AnalyseEmotionnelle build(
            int journalId,
            String etatEmotionnel,
            String niveau,
            String declencheur,
            String conseil
    ) {

        AnalyseEmotionnelle analyse =
                new AnalyseEmotionnelle();

        analyse.setJournalId(journalId);

        analyse.setEtatEmotionnel(etatEmotionnel);

        analyse.setNiveau(niveau);

        analyse.setDeclencheur(declencheur);

        analyse.setConseil(conseil);

        analyse.setDateAnalyse(LocalDateTime.now());

        return analyse;
    }

    private String buildSummary(String contenu) {

        if (contenu == null || contenu.isBlank()) {
            return "";
        }

        String compact =
                contenu.replaceAll("\\s+", " ").trim();

        return compact.length() <= 90
                ? compact
                : compact.substring(0, 87) + "...";
    }

    private String mapStressToNiveau(String stress) {

        if (stress == null) {
            return "Faible";
        }

        try {

            int value = Integer.parseInt(stress);

            if (value <= 25) {
                return "Faible";
            }

            if (value <= 50) {
                return "Modere";
            }

            if (value <= 75) {
                return "Eleve";
            }

            return "Critique";

        } catch (Exception e) {

            return "Faible";
        }
    }

    private int mapNiveauToStress(String niveau) {

        if (niveau == null) {
            return 25;
        }

        return switch (niveau.toLowerCase()) {

            case "faible" -> 25;

            case "modere" -> 50;

            case "eleve" -> 75;

            case "critique" -> 100;

            default -> 25;
        };
    }
}