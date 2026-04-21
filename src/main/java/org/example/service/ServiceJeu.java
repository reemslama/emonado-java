package org.example.service;

import org.example.entities.Jeu;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceJeu {
    private final Connection cnx = DataSource.getInstance().getConnection();

    public ServiceJeu() {
        ensureSchema();
    }

    public boolean ajouter(Jeu jeu) {
        String sql = "INSERT INTO jeu(titre, description, max_participants, actif) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, jeu.getTitre());
            ps.setString(2, jeu.getDescription());
            ps.setInt(3, jeu.getMaxParticipants());
            ps.setInt(4, jeu.isActif() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur ajout jeu: " + e.getMessage());
            return false;
        }
    }

    public boolean modifier(Jeu jeu) {
        String sql = "UPDATE jeu SET titre = ?, description = ?, max_participants = ?, actif = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, jeu.getTitre());
            ps.setString(2, jeu.getDescription());
            ps.setInt(3, jeu.getMaxParticipants());
            ps.setInt(4, jeu.isActif() ? 1 : 0);
            ps.setInt(5, jeu.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur modification jeu: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM jeu WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur suppression jeu: " + e.getMessage());
            return false;
        }
    }

    public List<Jeu> afficherTout() {
        return executeJeuQuery(baseQuery() + " ORDER BY j.id DESC");
    }

    public List<Jeu> afficherDisponibles() {
        return executeJeuQuery(baseQuery() + " HAVING j.actif = 1 AND COUNT(p.id) < j.max_participants ORDER BY j.id DESC");
    }

    public Jeu findById(int id) {
        String sql = baseQuery() + " HAVING j.id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapJeu(rs);
            }
        } catch (Exception e) {
            System.out.println("Erreur recherche jeu: " + e.getMessage());
        }
        return null;
    }

    public boolean existeTitre(String titre, Integer excludedId) {
        String sql = "SELECT COUNT(*) FROM jeu WHERE LOWER(titre) = LOWER(?)";
        if (excludedId != null) {
            sql += " AND id <> ?";
        }
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, titre);
            if (excludedId != null) {
                ps.setInt(2, excludedId);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification titre jeu: " + e.getMessage());
            return false;
        }
    }

    public boolean aPlacesDisponibles(int jeuId, Integer excludedParticipationId) {
        String sql =
                "SELECT j.max_participants, j.actif, COUNT(p.id) AS nb " +
                        "FROM jeu j " +
                        "LEFT JOIN participation p ON p.jeu_id = j.id";

        if (excludedParticipationId != null) {
            sql += " AND p.id <> ?";
        }

        sql += " WHERE j.id = ? GROUP BY j.id, j.max_participants, j.actif";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            int index = 1;
            if (excludedParticipationId != null) {
                ps.setInt(index++, excludedParticipationId);
            }
            ps.setInt(index, jeuId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("actif") == 1 && rs.getInt("nb") < rs.getInt("max_participants");
            }
        } catch (Exception e) {
            System.out.println("Erreur verification capacite jeu: " + e.getMessage());
        }
        return false;
    }

    private void ensureSchema() {
        try (Statement st = cnx.createStatement()) {

            // 1. Table jeu (schéma final sans image/interpretation_base)
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS jeu (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "titre VARCHAR(80) NOT NULL UNIQUE, " +
                            "description VARCHAR(400) NOT NULL, " +
                            "max_participants INT NOT NULL DEFAULT 3, " +
                            "actif TINYINT(1) NOT NULL DEFAULT 1" +
                            ")"
            );

            // 2. Table image_carte (FK → jeu)
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS image_carte (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "jeu_id INT NOT NULL, " +
                            "image_path VARCHAR(255) NOT NULL, " +
                            "interpretation_psy VARCHAR(500) NOT NULL, " +
                            "CONSTRAINT fk_ic_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id) ON DELETE CASCADE" +
                            ")"
            );

            // 3. Migration: colonne 'image' dans jeu → image_carte
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM jeu LIKE 'image'");
                if (rs.next()) {
                    st.executeUpdate(
                            "INSERT INTO image_carte (jeu_id, image_path, interpretation_psy) " +
                                    "SELECT id, image, COALESCE(interpretation_base, 'Interpretation non definie') " +
                                    "FROM jeu WHERE image IS NOT NULL AND TRIM(image) != '' " +
                                    "AND id NOT IN (SELECT DISTINCT jeu_id FROM image_carte)"
                    );
                    st.executeUpdate("ALTER TABLE jeu DROP COLUMN image");
                }
            } catch (Exception e) {
                System.out.println("Migration jeu.image: " + e.getMessage());
            }

            // 4. Migration: supprimer interpretation_base de jeu
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM jeu LIKE 'interpretation_base'");
                if (rs.next()) {
                    st.executeUpdate("ALTER TABLE jeu DROP COLUMN interpretation_base");
                }
            } catch (Exception e) {
                System.out.println("Migration jeu.interpretation_base: " + e.getMessage());
            }

            // 5. Migration: ancienne colonne 'images' → 'image'
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM jeu LIKE 'images'");
                if (rs.next()) {
                    st.executeUpdate("ALTER TABLE jeu CHANGE COLUMN images image VARCHAR(255) NOT NULL");
                }
            } catch (Exception ignored) {}

            // 6. Table participation (schéma final)
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS participation (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "user_id INT NOT NULL DEFAULT 0, " +
                            "jeu_id INT NOT NULL, " +
                            "image_choisie_id INT NOT NULL, " +
                            "resultat_psy VARCHAR(500) NOT NULL DEFAULT '', " +
                            "date_participation DATETIME NOT NULL, " +
                            "CONSTRAINT fk_participation_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id) ON DELETE CASCADE" +
                            ")"
            );

            // 7. Migration: supprimer choix_image de participation
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'choix_image'");
                if (rs.next()) {
                    st.executeUpdate("ALTER TABLE participation DROP COLUMN choix_image");
                }
            } catch (Exception e) {
                System.out.println("Migration participation.choix_image: " + e.getMessage());
            }

            // 8. Migration: ajouter user_id si absent (anciens schémas)
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'user_id'");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE participation ADD COLUMN user_id INT NOT NULL DEFAULT 0");
                }
            } catch (Exception e) {
                System.out.println("Migration add user_id: " + e.getMessage());
            }

            // 9. Migration: image_carte_id -> image_choisie_id
            try {
                ResultSet hasOld = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'image_carte_id'");
                if (hasOld.next()) {
                    ResultSet hasNew = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'image_choisie_id'");
                    if (!hasNew.next()) {
                        st.executeUpdate("ALTER TABLE participation CHANGE COLUMN image_carte_id image_choisie_id INT NOT NULL");
                    }
                }
            } catch (Exception e) {
                System.out.println("Migration image_carte_id -> image_choisie_id: " + e.getMessage());
            }

            // 10. Migration: interpretation -> resultat_psy
            try {
                ResultSet hasOld = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'interpretation'");
                if (hasOld.next()) {
                    ResultSet hasNew = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'resultat_psy'");
                    if (!hasNew.next()) {
                        st.executeUpdate("ALTER TABLE participation CHANGE COLUMN interpretation resultat_psy VARCHAR(500) NOT NULL DEFAULT ''");
                    }
                }
            } catch (Exception e) {
                System.out.println("Migration interpretation -> resultat_psy: " + e.getMessage());
            }

            // 11. Migration: supprimer nom_enfant de participation (ancien schema)
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'nom_enfant'");
                if (rs.next()) {
                    st.executeUpdate("ALTER TABLE participation DROP COLUMN nom_enfant");
                    System.out.println("Migration: colonne nom_enfant supprimee de participation");
                }
            } catch (Exception e) {
                System.out.println("Migration participation.nom_enfant: " + e.getMessage());
            }

            // 12. Migration: supprimer age_enfant de participation (ancien schema)
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'age_enfant'");
                if (rs.next()) {
                    st.executeUpdate("ALTER TABLE participation DROP COLUMN age_enfant");
                    System.out.println("Migration: colonne age_enfant supprimee de participation");
                }
            } catch (Exception e) {
                System.out.println("Migration participation.age_enfant: " + e.getMessage());
            }

            // 13. Migration: ajouter image_choisie_id si absent
            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'image_choisie_id'");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE participation ADD COLUMN image_choisie_id INT NOT NULL DEFAULT 0");
                }
            } catch (Exception e) {
                System.out.println("Migration add image_choisie_id: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Erreur creation schema jeu/image_carte/participation: " + e.getMessage());
        }
    }

    private String baseQuery() {
        return "SELECT j.id, j.titre, j.description, j.max_participants, j.actif, COUNT(p.id) AS nombre_participations " +
                "FROM jeu j " +
                "LEFT JOIN participation p ON p.jeu_id = j.id " +
                "GROUP BY j.id, j.titre, j.description, j.max_participants, j.actif";
    }

    private List<Jeu> executeJeuQuery(String sql) {
        List<Jeu> jeux = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                jeux.add(mapJeu(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur lecture jeux: " + e.getMessage());
        }
        return jeux;
    }

    private Jeu mapJeu(ResultSet rs) throws Exception {
        Jeu jeu = new Jeu();
        jeu.setId(rs.getInt("id"));
        jeu.setTitre(rs.getString("titre"));
        jeu.setDescription(rs.getString("description"));
        jeu.setMaxParticipants(rs.getInt("max_participants"));
        jeu.setActif(rs.getInt("actif") == 1);
        jeu.setNombreParticipations(rs.getInt("nombre_participations"));
        return jeu;
    }
}
