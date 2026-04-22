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
        new ServiceJeu(); // ensures schema is created
    }

    public boolean ajouter(Participation participation) {
        lastError = "";
        String sql = "INSERT INTO participation(jeu_id, nom_enfant, age_enfant, resultat_psy, date_participation, user_id) VALUES (?,?,?,?,?,?)";
        try {
            String nomEnfant = "Invite";
            int ageEnfant = 8;
            org.example.entities.User currentUser = org.example.utils.UserSession.getInstance();
            if (currentUser != null) {
                nomEnfant = currentUser.getNom() + " " + currentUser.getPrenom();
            }
            int userId = resolveFallbackUserId(participation.getUserId());
            return tryInsert(sql,
                    participation.getJeuId(),
                    nomEnfant,
                    ageEnfant,
                    participation.getResultatPsy(),
                    Timestamp.valueOf(participation.getDateParticipation()),
                    userId
            );
        } catch (Exception e) {
            lastError = e.getMessage();
            System.out.println("Erreur ajout participation: " + e.getMessage());
            return false;
        }
    }

    public boolean modifier(Participation participation) {
        String sql = "UPDATE participation SET user_id = ?, jeu_id = ?, image_choisie_id = ?, resultat_psy = ?, date_participation = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, participation.getUserId());
            ps.setInt(2, participation.getJeuId());
            ps.setInt(3, participation.getImageChoisieId());
            ps.setString(4, participation.getResultatPsy());
            ps.setTimestamp(5, Timestamp.valueOf(participation.getDateParticipation()));
            ps.setInt(6, participation.getId());
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
                        "p.resultat_psy, p.date_participation " +
                        "FROM participation p " +
                        "JOIN jeu j ON j.id = p.jeu_id " +
                        "LEFT JOIN image_carte ic ON ic.id = p.image_choisie_id " +
                        "ORDER BY p.date_participation DESC";

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

    private Participation mapParticipation(ResultSet rs) throws Exception {
        Participation participation = new Participation();
        participation.setId(rs.getInt("id"));
        participation.setUserId(rs.getInt("user_id"));
        participation.setJeuId(rs.getInt("jeu_id"));
        participation.setJeuTitre(rs.getString("jeu_titre"));
        participation.setImageChoisieId(rs.getInt("image_choisie_id"));
        participation.setImagePath(rs.getString("image_path"));
        participation.setResultatPsy(rs.getString("resultat_psy"));
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
}
