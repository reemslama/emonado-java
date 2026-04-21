package org.example.service;

import org.example.entities.ImageCarte;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceImageCarte {
    private final Connection cnx = DataSource.getInstance().getConnection();

    public boolean ajouter(ImageCarte img) {
        String sql = "INSERT INTO image_carte(jeu_id, image_path, interpretation_psy) VALUES (?,?,?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, img.getJeuId());
            ps.setString(2, img.getImagePath());
            ps.setString(3, img.getInterpretationPsy());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur ajout image_carte: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM image_carte WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur suppression image_carte: " + e.getMessage());
            return false;
        }
    }

    public List<ImageCarte> findByJeuId(int jeuId) {
        List<ImageCarte> images = new ArrayList<>();
        String sql = "SELECT id, jeu_id, image_path, interpretation_psy FROM image_carte WHERE jeu_id = ? ORDER BY id";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, jeuId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                images.add(mapImageCarte(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur lecture images du jeu: " + e.getMessage());
        }
        return images;
    }

    public ImageCarte findById(int id) {
        String sql = "SELECT id, jeu_id, image_path, interpretation_psy FROM image_carte WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapImageCarte(rs);
            }
        } catch (Exception e) {
            System.out.println("Erreur recherche image_carte: " + e.getMessage());
        }
        return null;
    }

    public void migrerAnciennesImages() {
        java.util.Map<String, String> mapping = java.util.Map.of(
                "https://placehold.co/320x220/png?text=Lion",           "/images/animaux/lion.png",
                "https://placehold.co/320x220/png?text=Chat",           "/images/animaux/chat.png",
                "https://placehold.co/320x220/png?text=Chien",          "/images/animaux/chien.png",
                "https://placehold.co/320x220/png?text=Soleil",         "/images/nature/soleil.png",
                "https://placehold.co/320x220/png?text=Ciel",           "/images/nature/ciel.png",
                "https://placehold.co/320x220/png?text=Arbre",          "/images/nature/arbre.png",
                "https://placehold.co/320x220/png?text=Enfant+seul",    "/images/situation/enfant_seul.png",
                "https://placehold.co/320x220/png?text=Enfant+qui+dessine", "/images/situation/enfant_dessin.png",
                "https://placehold.co/320x220/png?text=Enfants+en+groupe",  "/images/situation/enfants_groupe.png"
        );
        String sql = "UPDATE image_carte SET image_path = ? WHERE image_path = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            for (java.util.Map.Entry<String, String> entry : mapping.entrySet()) {
                ps.setString(1, entry.getValue());
                ps.setString(2, entry.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            System.out.println("Erreur migration images: " + e.getMessage());
        }
    }


    private ImageCarte mapImageCarte(ResultSet rs) throws Exception {
        ImageCarte img = new ImageCarte();
        img.setId(rs.getInt("id"));
        img.setJeuId(rs.getInt("jeu_id"));
        img.setImagePath(rs.getString("image_path"));
        img.setInterpretationPsy(rs.getString("interpretation_psy"));
        return img;
    }
}
