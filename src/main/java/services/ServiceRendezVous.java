package services;

import entities.RendezVous;
import entities.RendezVousPsy;
import org.example.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {

    Connection cnx = DataSource.getInstance().getConnection();

    // =====================================================
    // AJOUT RENDEZ-VOUS
    // =====================================================
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
            System.out.println("❌ ERREUR INSERT:");
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // LISTE POUR PSY
    // =====================================================
    public List<RendezVousPsy> getRendezVousForPsy() {

        List<RendezVousPsy> list = new ArrayList<>();

        String sql =
                "SELECT r.age, r.adresse, " +
                        "u.nom, u.prenom, " +
                        "t.libelle AS type_rdv, " +
                        "d.date, d.heure_debut, d.heure_fin " +
                        "FROM rendez_vous r " +
                        "JOIN user u ON r.user_id = u.id " +
                        "JOIN type_rendez_vous t ON r.type_id = t.id " +
                        "JOIN disponibilite d ON r.dispo_id = d.id";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                RendezVousPsy r = new RendezVousPsy();

                r.setNom(rs.getString("nom"));
                r.setPrenom(rs.getString("prenom"));
                r.setTypeRdv(rs.getString("type_rdv"));
                r.setDate(rs.getString("date"));
                r.setHeureDebut(rs.getString("heure_debut"));
                r.setHeureFin(rs.getString("heure_fin"));

                list.add(r);
            }

        } catch (SQLException e) {
            System.out.println("❌ ERREUR SELECT PSY:");
            e.printStackTrace();
        }

        return list;
    }
}