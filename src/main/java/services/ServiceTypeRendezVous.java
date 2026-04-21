package services;

import entities.TypeRendezVous;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceTypeRendezVous {

    private final Connection cnx = DataSource.getInstance().getConnection();

    public ServiceTypeRendezVous() {
        ensureDefaultTypes();
    }

    public boolean ajouter(TypeRendezVous t) {
        String sql = "INSERT INTO type_rendez_vous(libelle) VALUES (?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, t.getLibelle());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur ajout type: " + e.getMessage());
            return false;
        }
    }

    public boolean modifier(TypeRendezVous t) {
        String sql = "UPDATE type_rendez_vous SET libelle = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, t.getLibelle());
            ps.setInt(2, t.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur modif type: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM type_rendez_vous WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur suppression type: " + e.getMessage());
            return false;
        }
    }

    public List<TypeRendezVous> afficherTout() {
        List<TypeRendezVous> list = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM type_rendez_vous ORDER BY libelle");
            while (rs.next()) {
                list.add(new TypeRendezVous(
                        rs.getInt("id"),
                        rs.getString("libelle")
                ));
            }
        } catch (Exception e) {
            System.out.println("Erreur type: " + e.getMessage());
        }
        return list;
    }

    public boolean existeLibelle(String libelle, Integer excludedId) {
        String sql = "SELECT COUNT(*) FROM type_rendez_vous WHERE LOWER(libelle) = LOWER(?)";
        if (excludedId != null) {
            sql += " AND id <> ?";
        }

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, libelle);
            if (excludedId != null) {
                ps.setInt(2, excludedId);
            }

            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification type: " + e.getMessage());
            return false;
        }
    }

    private void ensureDefaultTypes() {
        ensureTypeExists("consultation");
        ensureTypeExists("suivi");
    }

    private void ensureTypeExists(String libelle) {
        if (existeLibelle(libelle, null)) {
            return;
        }

        try (PreparedStatement ps = cnx.prepareStatement("INSERT INTO type_rendez_vous(libelle) VALUES (?)")) {
            ps.setString(1, libelle);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erreur initialisation type " + libelle + ": " + e.getMessage());
        }
    }
}
