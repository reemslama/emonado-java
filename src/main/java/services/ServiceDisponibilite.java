package services;

import entities.Disponibilite;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDisponibilite {

    Connection cnx = MyDatabase.getInstance().getConnection();

    // =====================================================
    // AJOUT DISPONIBILITE
    // =====================================================
    public boolean ajouter(Disponibilite d) {

        String sql = "INSERT INTO disponibilite (date, heure_debut, heure_fin, est_libre) VALUES (?, ?, ?, 1)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));

            ps.executeUpdate();

            System.out.println("✔ Disponibilité ajoutée");

            return true;

        } catch (Exception e) {
            System.out.println("❌ Erreur ajout dispo:");
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // DISPONIBILITÉS LIBRES
    // =====================================================
    public List<Disponibilite> getDisposLibres() {

        List<Disponibilite> list = new ArrayList<>();

        String sql = "SELECT * FROM disponibilite WHERE est_libre = 1";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                Disponibilite d = new Disponibilite();

                d.setId(rs.getInt("id"));
                d.setDate(rs.getDate("date").toLocalDate());
                d.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
                d.setHeureFin(rs.getTime("heure_fin").toLocalTime());

                list.add(d);
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur select dispo:");
            e.printStackTrace();
        }

        return list;
    }

    // =====================================================
    // MARQUER COMME RÉSERVÉ (AU LIEU DE DELETE)
    // =====================================================
    public void rendreIndisponible(int id) {

        String sql = "UPDATE disponibilite SET est_libre = 0 WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✔ Créneau marqué indisponible");

        } catch (Exception e) {
            System.out.println("❌ Erreur update dispo:");
            e.printStackTrace();
        }
    }
}