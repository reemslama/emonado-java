package org.example.service;

import org.example.entities.ResultatTest;
import org.example.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service de persistance des résultats de tests adaptatifs.
 * Crée automatiquement la table si elle n'existe pas.
 */
public class ResultatTestService {

    private static final Logger log = Logger.getLogger(ResultatTestService.class.getName());

    // -----------------------------------------------------------------------
    // DDL – création automatique de la table
    // -----------------------------------------------------------------------
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS resultat_test (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                categorie    VARCHAR(100)  NOT NULL,
                score_actuel INT           NOT NULL,
                score_max    INT           NOT NULL,
                niveau       VARCHAR(20)   NOT NULL,
                analyse_ia   TEXT,
                date_test    DATETIME      NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

    public ResultatTestService() {
        initialiserTable();
    }

    private void initialiserTable() {
        try (Statement st = DataSource.getInstance().getConnection().createStatement()) {
            st.execute(CREATE_TABLE_SQL);
            log.info("Table resultat_test prête.");
        } catch (SQLException e) {
            log.severe("Impossible de créer la table resultat_test : " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // SAVE
    // -----------------------------------------------------------------------
    public void sauvegarder(ResultatTest r) {
        String sql = """
                INSERT INTO resultat_test
                    (categorie, score_actuel, score_max, niveau, analyse_ia, date_test)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DataSource.getInstance().getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, r.getCategorie());
            ps.setInt(2, r.getScoreActuel());
            ps.setInt(3, r.getScoreMax());
            ps.setString(4, r.getNiveau());
            ps.setString(5, r.getAnalyseIA());
            ps.setTimestamp(6, Timestamp.valueOf(
                    r.getDateTest() != null ? r.getDateTest() : LocalDateTime.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
            log.info("Résultat sauvegardé, id=" + r.getId());

        } catch (SQLException e) {
            log.severe("Erreur sauvegarde résultat : " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // HISTORIQUE – 5 derniers tests (tous ou par catégorie)
    // -----------------------------------------------------------------------
    public List<ResultatTest> getDerniersResultats(int limit) {
        String sql = """
                SELECT id, categorie, score_actuel, score_max, niveau, analyse_ia, date_test
                FROM resultat_test
                ORDER BY date_test DESC
                LIMIT ?
                """;
        return executeQuery(sql, limit);
    }

    public List<ResultatTest> getDerniersResultatsParCategorie(String categorie, int limit) {
        String sql = """
                SELECT id, categorie, score_actuel, score_max, niveau, analyse_ia, date_test
                FROM resultat_test
                WHERE categorie = ?
                ORDER BY date_test DESC
                LIMIT ?
                """;
        List<ResultatTest> list = new ArrayList<>();
        try (PreparedStatement ps = DataSource.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, categorie);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) {
            log.severe("Erreur lecture historique : " + e.getMessage());
        }
        return list;
    }

    // -----------------------------------------------------------------------
    // Utilitaires
    // -----------------------------------------------------------------------
    private List<ResultatTest> executeQuery(String sql, int limit) {
        List<ResultatTest> list = new ArrayList<>();
        try (PreparedStatement ps = DataSource.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) {
            log.severe("Erreur lecture résultats : " + e.getMessage());
        }
        return list;
    }

    private ResultatTest mapper(ResultSet rs) throws SQLException {
        ResultatTest r = new ResultatTest();
        r.setId(rs.getInt("id"));
        r.setCategorie(rs.getString("categorie"));
        r.setScoreActuel(rs.getInt("score_actuel"));
        r.setScoreMax(rs.getInt("score_max"));
        r.setNiveau(rs.getString("niveau"));
        r.setAnalyseIA(rs.getString("analyse_ia"));
        Timestamp ts = rs.getTimestamp("date_test");
        if (ts != null) r.setDateTest(ts.toLocalDateTime());
        return r;
    }
}