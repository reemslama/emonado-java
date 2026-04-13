package services;

import entities.RendezVous;
import entities.RendezVousPsy;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {

    private final Connection cnx = DataSource.getInstance().getConnection();

    public boolean ajouter(RendezVous r) {
        String sql = "INSERT INTO rendez_vous(age, adresse, type_id, dispo_id, user_id) VALUES (?,?,?,?,?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, r.getAge());
            ps.setString(2, r.getAdresse());
            ps.setInt(3, r.getTypeId());
            ps.setInt(4, r.getDispoId());
            ps.setInt(5, r.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("ERREUR INSERT:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean modifier(RendezVous r) {
        String sql = "UPDATE rendez_vous SET age = ?, adresse = ?, type_id = ?, dispo_id = ?, user_id = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, r.getAge());
            ps.setString(2, r.getAdresse());
            ps.setInt(3, r.getTypeId());
            ps.setInt(4, r.getDispoId());
            ps.setInt(5, r.getUserId());
            ps.setInt(6, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("ERREUR UPDATE:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM rendez_vous WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("ERREUR DELETE:");
            e.printStackTrace();
            return false;
        }
    }

    public List<RendezVous> getRendezVousByUser(int userId) {
        List<RendezVous> list = new ArrayList<>();
        String sql =
                "SELECT r.id, r.age, r.adresse, r.type_id, r.dispo_id, r.user_id, " +
                        "t.libelle AS type_rdv, d.date, d.heure_debut, d.heure_fin " +
                        "FROM rendez_vous r " +
                        "JOIN type_rendez_vous t ON r.type_id = t.id " +
                        "JOIN disponibilite d ON r.dispo_id = d.id " +
                        "WHERE r.user_id = ? ORDER BY d.date DESC, d.heure_debut DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRendezVous(rs));
            }
        } catch (SQLException e) {
            System.out.println("ERREUR SELECT USER:");
            e.printStackTrace();
        }
        return list;
    }

    public List<RendezVousPsy> getRendezVousForPsy() {
        List<RendezVousPsy> list = new ArrayList<>();
        String sql =
                "SELECT r.id, r.age, r.adresse, r.type_id, r.dispo_id, r.user_id, " +
                        "u.nom, u.prenom, t.libelle AS type_rdv, d.date, d.heure_debut, d.heure_fin " +
                        "FROM rendez_vous r " +
                        "JOIN user u ON r.user_id = u.id " +
                        "JOIN type_rendez_vous t ON r.type_id = t.id " +
                        "JOIN disponibilite d ON r.dispo_id = d.id " +
                        "ORDER BY d.date DESC, d.heure_debut DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RendezVousPsy r = new RendezVousPsy();
                r.setId(rs.getInt("id"));
                r.setAge(rs.getInt("age"));
                r.setAdresse(rs.getString("adresse"));
                r.setTypeId(rs.getInt("type_id"));
                r.setDispoId(rs.getInt("dispo_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setNom(rs.getString("nom"));
                r.setPrenom(rs.getString("prenom"));
                r.setTypeRdv(rs.getString("type_rdv"));
                r.setDate(String.valueOf(rs.getDate("date")));
                r.setHeureDebut(String.valueOf(rs.getTime("heure_debut")));
                r.setHeureFin(String.valueOf(rs.getTime("heure_fin")));
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("ERREUR SELECT PSY:");
            e.printStackTrace();
        }
        return list;
    }

    private RendezVous mapRendezVous(ResultSet rs) throws SQLException {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setId(rs.getInt("id"));
        rendezVous.setAge(rs.getInt("age"));
        rendezVous.setAdresse(rs.getString("adresse"));
        rendezVous.setTypeId(rs.getInt("type_id"));
        rendezVous.setDispoId(rs.getInt("dispo_id"));
        rendezVous.setUserId(rs.getInt("user_id"));
        rendezVous.setTypeLibelle(rs.getString("type_rdv"));
        rendezVous.setDateDisponibilite(String.valueOf(rs.getDate("date")));
        rendezVous.setHeureDebut(String.valueOf(rs.getTime("heure_debut")));
        rendezVous.setHeureFin(String.valueOf(rs.getTime("heure_fin")));
        return rendezVous;
    }
}
