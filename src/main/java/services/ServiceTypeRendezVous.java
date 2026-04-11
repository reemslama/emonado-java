package services;

import entities.TypeRendezVous;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTypeRendezVous {

    Connection cnx = MyDatabase.getInstance().getConnection();

    public boolean ajouter(TypeRendezVous t) {

        String sql = "INSERT INTO type_rendez_vous(libelle) VALUES (?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, t.getLibelle());
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.out.println("Erreur ajout type: " + e.getMessage());
            return false;
        }
    }

    public List<TypeRendezVous> afficherTout() {

        List<TypeRendezVous> list = new ArrayList<>();

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM type_rendez_vous");

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
}