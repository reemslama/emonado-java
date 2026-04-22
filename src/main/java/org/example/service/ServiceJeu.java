package org.example.service;

import org.example.entities.Jeu;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceJeu {
    private final Connection cnx = DataSource.getInstance().getConnection();

    public ServiceJeu() {
        ensureSchema();
    }

    public boolean ajouter(Jeu jeu) {
        String sql = "INSERT INTO jeu(titre, description, scene_image_path, max_participants, actif) VALUES (?,?,?,?,?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, jeu.getTitre());
            ps.setString(2, jeu.getDescription());
            ps.setString(3, jeu.getSceneImagePath());
            ps.setInt(4, jeu.getMaxParticipants());
            ps.setInt(5, jeu.isActif() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur ajout jeu: " + e.getMessage());
            return false;
        }
    }

    public boolean modifier(Jeu jeu) {
        String sql = "UPDATE jeu SET titre = ?, description = ?, scene_image_path = ?, max_participants = ?, actif = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, jeu.getTitre());
            ps.setString(2, jeu.getDescription());
            ps.setString(3, jeu.getSceneImagePath());
            ps.setInt(4, jeu.getMaxParticipants());
            ps.setInt(5, jeu.isActif() ? 1 : 0);
            ps.setInt(6, jeu.getId());
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
        return executeJeuQuery(baseQuery() + " ORDER BY j.id ASC");
    }

    public List<Jeu> afficherDisponibles() {
        return executeJeuQuery(baseQuery() + " HAVING j.actif = 1 AND COUNT(p.id) < j.max_participants ORDER BY j.id ASC");
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
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS jeu (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "titre VARCHAR(80) NOT NULL UNIQUE, " +
                            "description VARCHAR(400) NOT NULL, " +
                            "scene_image_path VARCHAR(255) NOT NULL DEFAULT '', " +
                            "max_participants INT NOT NULL DEFAULT 3, " +
                            "actif TINYINT(1) NOT NULL DEFAULT 1" +
                            ")"
            );

            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM jeu LIKE 'scene_image_path'");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE jeu ADD COLUMN scene_image_path VARCHAR(255) NOT NULL DEFAULT '' AFTER description");
                }
            } catch (Exception e) {
                System.out.println("Migration jeu.scene_image_path: " + e.getMessage());
            }

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS image_carte (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "jeu_id INT NOT NULL, " +
                            "image_path VARCHAR(255) NOT NULL, " +
                            "interpretation_psy VARCHAR(500) NOT NULL, " +
                            "comportement_tag VARCHAR(50) NOT NULL DEFAULT 'neutre', " +
                            "CONSTRAINT fk_ic_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id) ON DELETE CASCADE" +
                            ")"
            );

            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM image_carte LIKE 'comportement_tag'");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE image_carte ADD COLUMN comportement_tag VARCHAR(50) NOT NULL DEFAULT 'neutre'");
                }
            } catch (Exception e) {
                System.out.println("Migration image_carte.comportement_tag: " + e.getMessage());
            }

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS participation (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "user_id INT NOT NULL DEFAULT 0, " +
                            "jeu_id INT NOT NULL, " +
                            "image_choisie_id INT NOT NULL, " +
                            "resultat_psy VARCHAR(500) NOT NULL DEFAULT '', " +
                            "comportement_tag VARCHAR(50) NOT NULL DEFAULT 'neutre', " +
                            "temps_reponse_ms BIGINT NOT NULL DEFAULT 0, " +
                            "date_participation DATETIME NOT NULL, " +
                            "CONSTRAINT fk_participation_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id) ON DELETE CASCADE" +
                            ")"
            );

            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'comportement_tag'");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE participation ADD COLUMN comportement_tag VARCHAR(50) NOT NULL DEFAULT 'neutre'");
                }
            } catch (Exception e) {
                System.out.println("Migration participation.comportement_tag: " + e.getMessage());
            }

            try {
                ResultSet rs = st.executeQuery("SHOW COLUMNS FROM participation LIKE 'temps_reponse_ms'");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE participation ADD COLUMN temps_reponse_ms BIGINT NOT NULL DEFAULT 0");
                }
            } catch (Exception e) {
                System.out.println("Migration participation.temps_reponse_ms: " + e.getMessage());
            }

            seedDefaultGamesIfNeeded();
        } catch (Exception e) {
            System.out.println("Erreur creation schema jeu/image_carte/participation: " + e.getMessage());
        }
    }

    private void seedDefaultGamesIfNeeded() {
        List<SeedGame> seedGames = List.of(
                new SeedGame("TEST 01 - Son chien", "SCENARIO:AUDIO_CHIEN", "/images/background.png",
                        option("/images/animaux/chien.png", "Associe le son au chien", "curiosite"),
                        option("/images/animaux/chat.png", "Hesite sur un animal plus doux", "timidite"),
                        option("/images/animaux/lion.png", "Est attire par une image impressionnante", "peur")),
                new SeedGame("TEST 02 - Son chat", "SCENARIO:AUDIO_CHAT", "/images/background.png",
                        option("/images/animaux/chat.png", "Associe le son au chat", "calme"),
                        option("/images/animaux/chien.png", "Cherche un animal plus actif", "jouer"),
                        option("/images/animaux/lion.png", "Est distrait par une image forte", "peur")),
                new SeedGame("TEST 03 - Son lion", "SCENARIO:AUDIO_LION", "/images/background.png",
                        option("/images/animaux/lion.png", "Associe le son au lion", "peur"),
                        option("/images/animaux/chat.png", "Cherche une image rassurante", "calme"),
                        option("/images/animaux/chien.png", "Se tourne vers un animal familier", "curiosite")),
                new SeedGame("TEST 04 - Dessin ou reel", "SCENARIO:DRAWING_MATCH", "/images/situation/enfant_dessin.png",
                        option("/images/situation/enfant_dessin.png", "Reconnaissance d'une image dessinee", "curiosite"),
                        option("/images/animaux/chat.png", "Prefere une image concrete", "calme"),
                        option("/images/situation/enfant_seul.png", "Se retire de la tache", "timidite")),
                new SeedGame("TEST 05 - Probleme solitude", "SCENARIO:PROBLEME_SOLITUDE", "/images/situation/enfant_seul.png",
                        option("/images/situation/enfants_groupe.png", "Choisit la compagnie pour aller mieux", "jouer"),
                        option("/images/situation/enfant_dessin.png", "Choisit une activite calme", "calme"),
                        option("/images/nature/soleil.png", "Choisit une image reconfortante", "curiosite")),
                new SeedGame("TEST 06 - Probleme groupe", "SCENARIO:PROBLEME_GROUPE", "/images/situation/enfants_groupe.png",
                        option("/images/situation/enfants_groupe.png", "Va spontanement vers les autres", "partage"),
                        option("/images/situation/enfant_dessin.png", "Prefere une solution individuelle", "timidite"),
                        option("/images/nature/ciel.png", "Observe avant d'agir", "calme")),
                new SeedGame("TEST 07 - Probleme peur", "SCENARIO:PROBLEME_PEUR", "/images/animaux/lion.png",
                        option("/images/nature/soleil.png", "Cherche une image apaisante", "calme"),
                        option("/images/situation/enfants_groupe.png", "Cherche du soutien", "jouer"),
                        option("/images/animaux/lion.png", "Reste fixe sur la source de peur", "peur")),
                new SeedGame("TEST 08 - Probleme repas", "SCENARIO:PROBLEME_REPAS", "/images/situation/enfant_seul.png",
                        option("/images/nature/soleil.png", "Cherche du reconfort", "calme"),
                        option("/images/situation/enfants_groupe.png", "Cherche de l'aide", "jouer"),
                        option("/images/situation/enfant_dessin.png", "Se replie sur une activite solitaire", "rester_seul")),
                new SeedGame("TEST 09 - Observation animale", "SCENARIO:OBSERVATION_ANIMALE", "/images/animaux/chat.png",
                        option("/images/animaux/chat.png", "Observe calmement", "curiosite"),
                        option("/images/animaux/chien.png", "Cherche l'interaction", "jouer"),
                        option("/images/animaux/lion.png", "Se focalise sur l'intensite", "peur")),
                new SeedGame("TEST 10 - Resolution finale", "SCENARIO:RESOLUTION_FINALE", "/images/situation/enfant_seul.png",
                        option("/images/situation/enfants_groupe.png", "Choisit une resolution sociale", "jouer"),
                        option("/images/nature/soleil.png", "Choisit une resolution apaisante", "calme"),
                        option("/images/situation/enfant_dessin.png", "Choisit une resolution personnelle", "rester_seul"))
        );

        String insertJeuSql = "INSERT INTO jeu(titre, description, scene_image_path, max_participants, actif) VALUES (?,?,?,?,?)";
        String insertImageSql = "INSERT INTO image_carte(jeu_id, image_path, interpretation_psy, comportement_tag) VALUES (?,?,?,?)";
        String existsSql = "SELECT id FROM jeu WHERE titre = ?";
        String updateJeuSql = "UPDATE jeu SET description = ?, scene_image_path = ?, max_participants = ?, actif = ? WHERE id = ?";
        String deleteImagesSql = "DELETE FROM image_carte WHERE jeu_id = ?";

        try (PreparedStatement existsPs = cnx.prepareStatement(existsSql);
             PreparedStatement updatePs = cnx.prepareStatement(updateJeuSql);
             PreparedStatement deleteImagesPs = cnx.prepareStatement(deleteImagesSql);
             PreparedStatement jeuPs = cnx.prepareStatement(insertJeuSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement imagePs = cnx.prepareStatement(insertImageSql)) {
            for (SeedGame seedGame : seedGames) {
                Integer jeuId = null;
                existsPs.setString(1, seedGame.titre());
                try (ResultSet existing = existsPs.executeQuery()) {
                    if (existing.next()) {
                        jeuId = existing.getInt("id");
                    }
                }

                if (jeuId == null) {
                    jeuPs.setString(1, seedGame.titre());
                    jeuPs.setString(2, seedGame.description());
                    jeuPs.setString(3, seedGame.sceneImagePath());
                    jeuPs.setInt(4, 3);
                    jeuPs.setInt(5, 1);
                    jeuPs.executeUpdate();

                    try (ResultSet keys = jeuPs.getGeneratedKeys()) {
                        if (!keys.next()) {
                            continue;
                        }
                        jeuId = keys.getInt(1);
                    }
                } else {
                    updatePs.setString(1, seedGame.description());
                    updatePs.setString(2, seedGame.sceneImagePath());
                    updatePs.setInt(3, 3);
                    updatePs.setInt(4, 1);
                    updatePs.setInt(5, jeuId);
                    updatePs.executeUpdate();

                    deleteImagesPs.setInt(1, jeuId);
                    deleteImagesPs.executeUpdate();
                }

                for (SeedOption option : seedGame.options()) {
                    imagePs.setInt(1, jeuId);
                    imagePs.setString(2, option.imagePath());
                    imagePs.setString(3, option.interpretation());
                    imagePs.setString(4, option.tag());
                    imagePs.addBatch();
                }
                imagePs.executeBatch();
                imagePs.clearBatch();
            }
        } catch (Exception e) {
            System.out.println("Erreur seed jeux par defaut: " + e.getMessage());
        }
    }

    private String baseQuery() {
        return "SELECT j.id, j.titre, j.description, j.scene_image_path, j.max_participants, j.actif, COUNT(p.id) AS nombre_participations " +
                "FROM jeu j " +
                "LEFT JOIN participation p ON p.jeu_id = j.id " +
                "GROUP BY j.id, j.titre, j.description, j.scene_image_path, j.max_participants, j.actif";
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
        jeu.setSceneImagePath(rs.getString("scene_image_path"));
        jeu.setMaxParticipants(rs.getInt("max_participants"));
        jeu.setActif(rs.getInt("actif") == 1);
        jeu.setNombreParticipations(rs.getInt("nombre_participations"));
        return jeu;
    }

    private static SeedOption option(String imagePath, String interpretation, String tag) {
        return new SeedOption(imagePath, interpretation, tag);
    }

    private record SeedGame(String titre, String description, String sceneImagePath, List<SeedOption> options) {
        private SeedGame(String titre, String description, String sceneImagePath, SeedOption... options) {
            this(titre, description, sceneImagePath, Arrays.asList(options));
        }
    }

    private record SeedOption(String imagePath, String interpretation, String tag) {
    }
}
