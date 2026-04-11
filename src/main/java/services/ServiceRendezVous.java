package services;

import entities.RendezVous;
import entities.RendezVousPsy;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {

    Connection cnx = MyDatabase.getInstance().getConnection();

    // =====================================================
    // AJOUT RENDEZ-VOUS
    // =====================================================
    public boolean ajouter(RendezVous r) {

        String sql = "INSERT INTO rendez_vous(nom, prenom, age, adresse, id_type, id_dispo) VALUES (?,?,?,?,?,?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            // DEBUG IMPORTANT
            System.out.println("TYPE ID = " + r.getTypeId());
            System.out.println("DISPO ID = " + r.getDispoId());

            ps.setString(1, r.getNom());
            ps.setString(2, r.getPrenom());
            ps.setInt(3, r.getAge());
            ps.setString(4, r.getAdresse());
            ps.setInt(5, r.getTypeId());
            ps.setInt(6, r.getDispoId());

            int rows = ps.executeUpdate();

            System.out.println("✔ INSERT OK -> rows = " + rows);

            return rows > 0;

        } catch (SQLException e) {

            System.out.println("❌ ERREUR INSERT:");
            e.printStackTrace();

            return false;
        }
    }

    // =====================================================
    // AFFICHAGE POUR PSY
    // =====================================================
    public List<RendezVousPsy> getRendezVousForPsy() {

        List<RendezVousPsy> list = new ArrayList<>();

        String sql =
                "SELECT r.nom, r.prenom, " +
                        "t.libelle AS type_rdv, " +
                        "d.date, d.heure_debut, d.heure_fin " +
                        "FROM rendez_vous r " +
                        "JOIN type_rendez_vous t ON r.id_type = t.id " +
                        "JOIN disponibilite d ON r.id_dispo = d.id";

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

            System.out.println("✔ RDV PSY = " + list.size());

        } catch (SQLException e) {
            System.out.println("❌ ERREUR SELECT PSY:");
            e.printStackTrace();
        }

        return list;
    }
}