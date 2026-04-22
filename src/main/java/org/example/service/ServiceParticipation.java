package org.example.service;

import org.example.entities.Participation;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ServiceParticipation {
    private final Connection cnx = DataSource.getInstance().getConnection();
    private String lastError = "";

    public ServiceParticipation() {
        new ServiceJeu();
    }

    public boolean ajouter(Participation participation) {
        lastError = "";
        String sql = "INSERT INTO participation(user_id, jeu_id, image_choisie_id, resultat_psy, comportement_tag, temps_reponse_ms, date_participation) VALUES (?,?,?,?,?,?,?)";
        try {
            int userId = resolveFallbackUserId(participation.getUserId());
            return tryInsert(sql,
                    userId,
                    participation.getJeuId(),
                    participation.getImageChoisieId(),
                    participation.getResultatPsy(),
                    normalizeTag(participation.getComportementTag()),
                    participation.getTempsReponseMs(),
                    Timestamp.valueOf(participation.getDateParticipation())
            );
        } catch (Exception e) {
            lastError = e.getMessage();
            System.out.println("Erreur ajout participation: " + e.getMessage());
            return false;
        }
    }

    public boolean modifier(Participation participation) {
        String sql = "UPDATE participation SET user_id = ?, jeu_id = ?, image_choisie_id = ?, resultat_psy = ?, comportement_tag = ?, temps_reponse_ms = ?, date_participation = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, participation.getUserId());
            ps.setInt(2, participation.getJeuId());
            ps.setInt(3, participation.getImageChoisieId());
            ps.setString(4, participation.getResultatPsy());
            ps.setString(5, normalizeTag(participation.getComportementTag()));
            ps.setLong(6, participation.getTempsReponseMs());
            ps.setTimestamp(7, Timestamp.valueOf(participation.getDateParticipation()));
            ps.setInt(8, participation.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur modification participation: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM participation WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur suppression participation: " + e.getMessage());
            return false;
        }
    }

    public List<Participation> afficherTout() {
        String sql =
                "SELECT p.id, p.user_id, p.jeu_id, j.titre AS jeu_titre, " +
                        "p.image_choisie_id, COALESCE(ic.image_path, '') AS image_path, " +
                        "p.resultat_psy, p.comportement_tag, p.temps_reponse_ms, p.date_participation " +
                        "FROM participation p " +
                        "JOIN jeu j ON j.id = p.jeu_id " +
                        "LEFT JOIN image_carte ic ON ic.id = p.image_choisie_id " +
                        "ORDER BY p.date_participation DESC";
        return executeParticipationQuery(sql);
    }

    public List<Participation> findByUserId(int userId) {
        String sql =
                "SELECT p.id, p.user_id, p.jeu_id, j.titre AS jeu_titre, " +
                        "p.image_choisie_id, COALESCE(ic.image_path, '') AS image_path, " +
                        "p.resultat_psy, p.comportement_tag, p.temps_reponse_ms, p.date_participation " +
                        "FROM participation p " +
                        "JOIN jeu j ON j.id = p.jeu_id " +
                        "LEFT JOIN image_carte ic ON ic.id = p.image_choisie_id " +
                        "WHERE p.user_id = ? " +
                        "ORDER BY p.date_participation DESC";
        List<Participation> participations = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                participations.add(mapParticipation(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur lecture participations user: " + e.getMessage());
        }
        return participations;
    }

    private List<Participation> executeParticipationQuery(String sql) {
        List<Participation> participations = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                participations.add(mapParticipation(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur lecture participations: " + e.getMessage());
        }
        return participations;
    }

    private boolean tryInsert(String sql, Object... values) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    private int resolveFallbackUserId(int requestedUserId) {
        if (requestedUserId > 0) {
            return requestedUserId;
        }
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM user ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private String normalizeTag(String tag) {
        return tag == null || tag.isBlank() ? "neutre" : tag.trim().toLowerCase();
    }

    private Participation mapParticipation(ResultSet rs) throws Exception {
        Participation participation = new Participation();
        participation.setId(rs.getInt("id"));
        participation.setUserId(rs.getInt("user_id"));
        participation.setJeuId(rs.getInt("jeu_id"));
        participation.setJeuTitre(rs.getString("jeu_titre"));
        participation.setImageChoisieId(rs.getInt("image_choisie_id"));
        participation.setImagePath(rs.getString("image_path"));
        participation.setResultatPsy(rs.getString("resultat_psy"));
        participation.setComportementTag(normalizeTag(rs.getString("comportement_tag")));
        participation.setTempsReponseMs(rs.getLong("temps_reponse_ms"));
        participation.setDateParticipation(rs.getTimestamp("date_participation").toLocalDateTime());
        return participation;
    }

    public boolean dejaParticipe(int jeuId, int userId) {
        String sql = "SELECT COUNT(*) FROM participation WHERE jeu_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, jeuId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification doublon: " + e.getMessage());
            return false;
        }
    }

    public String getLastError() {
        return lastError == null ? "" : lastError;
    }

    public List<Object[]> getHeatmapData() {
        String sql =
                "SELECT ic.image_path, j.titre AS jeu_titre, COUNT(*) as choix_count, " +
                        "p.resultat_psy " +
                        "FROM participation p " +
                        "JOIN jeu j ON j.id = p.jeu_id " +
                        "JOIN image_carte ic ON ic.id = p.image_choisie_id " +
                        "GROUP BY ic.image_path, j.titre, p.resultat_psy " +
                        "ORDER BY j.titre, choix_count DESC";

        List<Object[]> heatmapData = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Object[] row = new Object[] {
                        rs.getString("image_path"),
                        rs.getString("jeu_titre"),
                        rs.getInt("choix_count"),
                        rs.getString("resultat_psy")
                };
                heatmapData.add(row);
            }
        } catch (Exception e) {
            System.out.println("Erreur lecture heatmap data: " + e.getMessage());
        }
        return heatmapData;
    }
}
