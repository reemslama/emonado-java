package services;

import entities.Disponibilite;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ServiceDisponibilite {

    private final Connection cnx = DataSource.getInstance().getConnection();

    public boolean ajouter(Disponibilite d) {
        String sql = "INSERT INTO disponibilite (date, heure_debut, heure_fin, est_libre) VALUES (?, ?, ?, 1)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur ajout dispo:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean modifier(Disponibilite d) {
        String sql = "UPDATE disponibilite SET date = ?, heure_debut = ?, heure_fin = ?, est_libre = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));
            ps.setInt(4, d.isLibre() ? 1 : 0);
            ps.setInt(5, d.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur modif dispo:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM disponibilite WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur suppression dispo:");
            e.printStackTrace();
            return false;
        }
    }

    public List<Disponibilite> getDisposLibres() {
        return executeQuery("SELECT * FROM disponibilite WHERE est_libre = 1 ORDER BY date, heure_debut");
    }

    public List<Disponibilite> getDisposLibresIncluding(int dispoId) {
        List<Disponibilite> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilite WHERE est_libre = 1 OR id = ? ORDER BY date, heure_debut";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, dispoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapDisponibilite(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur select dispo:");
            e.printStackTrace();
        }
        return list;
    }

    public List<Disponibilite> afficherTout() {
        return executeQuery("SELECT * FROM disponibilite ORDER BY date DESC, heure_debut DESC");
    }

    public void rendreIndisponible(int id) {
        updateDisponibiliteState(id, 0);
    }

    public void rendreDisponible(int id) {
        updateDisponibiliteState(id, 1);
    }

    public boolean existeChevauchement(Disponibilite disponibilite, Integer excludedId) {
        String sql =
                "SELECT COUNT(*) FROM disponibilite " +
                        "WHERE date = ? AND heure_debut < ? AND heure_fin > ?";

        if (excludedId != null) {
            sql += " AND id <> ?";
        }

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(disponibilite.getDate()));
            ps.setTime(2, Time.valueOf(disponibilite.getHeureFin()));
            ps.setTime(3, Time.valueOf(disponibilite.getHeureDebut()));
            if (excludedId != null) {
                ps.setInt(4, excludedId);
            }

            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification disponibilite:");
            e.printStackTrace();
            return false;
        }
    }

    private void updateDisponibiliteState(int id, int libre) {
        String sql = "UPDATE disponibilite SET est_libre = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, libre);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erreur update dispo:");
            e.printStackTrace();
        }
    }

    private List<Disponibilite> executeQuery(String sql) {
        List<Disponibilite> list = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(mapDisponibilite(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur select dispo:");
            e.printStackTrace();
        }
        return list;
    }

    private Disponibilite mapDisponibilite(ResultSet rs) throws Exception {
        Disponibilite d = new Disponibilite();
        d.setId(rs.getInt("id"));
        d.setDate(rs.getDate("date").toLocalDate());
        d.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        d.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        d.setLibre(rs.getInt("est_libre") == 1);
        return d;
    }
}
