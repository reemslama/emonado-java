package services;

import entities.Disponibilite;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ServiceDisponibilite {

    private final Connection cnx = DataSource.getInstance().getConnection();

    public ServiceDisponibilite() {
        ensureSchema();
    }

    public boolean ajouter(Disponibilite d) {
        String sql = "INSERT INTO disponibilite (psychologue_id, date, heure_debut, heure_fin, est_libre) VALUES (?, ?, ?, ?, 1)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, d.getPsychologueId());
            ps.setDate(2, Date.valueOf(d.getDate()));
            ps.setTime(3, Time.valueOf(d.getHeureDebut()));
            ps.setTime(4, Time.valueOf(d.getHeureFin()));
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur ajout dispo:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean modifier(Disponibilite d) {
        String sql = "UPDATE disponibilite SET psychologue_id = ?, date = ?, heure_debut = ?, heure_fin = ?, est_libre = ? WHERE id = ?";
        try {
            if (aDesReservations(d.getId())) {
                return false;
            }
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, d.getPsychologueId());
            ps.setDate(2, Date.valueOf(d.getDate()));
            ps.setTime(3, Time.valueOf(d.getHeureDebut()));
            ps.setTime(4, Time.valueOf(d.getHeureFin()));
            ps.setInt(5, d.isLibre() ? 1 : 0);
            ps.setInt(6, d.getId());
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
            if (aDesReservations(id)) {
                return false;
            }
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
        String sql =
                "SELECT d.*, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                "FROM disponibilite d " +
                "LEFT JOIN user u ON u.id = d.psychologue_id " +
                "WHERE d.est_libre = 1 " +
                "AND TIMESTAMP(d.date, d.heure_debut) > NOW() " +
                "ORDER BY d.date, d.heure_debut";
        return executeQuery(sql);
    }

    public List<Disponibilite> getDisposLibresByPsychologue(int psychologueId) {
        return executePreparedQuery(
                "SELECT d.*, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                "FROM disponibilite d " +
                "LEFT JOIN user u ON u.id = d.psychologue_id " +
                "WHERE d.psychologue_id = ? AND d.est_libre = 1 AND TIMESTAMP(d.date, d.heure_debut) > NOW() " +
                "ORDER BY d.date, d.heure_debut",
                psychologueId
        );
    }

    public List<LocalDate> getDatesDisponibles() {
        List<LocalDate> dates = new ArrayList<>();
        String sql =
                "SELECT DISTINCT date FROM disponibilite " +
                "WHERE est_libre = 1 AND TIMESTAMP(date, heure_debut) > NOW() " +
                "ORDER BY date";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                dates.add(rs.getDate("date").toLocalDate());
            }
        } catch (Exception e) {
            System.out.println("Erreur select dates disponibles:");
            e.printStackTrace();
        }
        return dates;
    }

    public List<LocalDate> getDatesDisponiblesByPsychologue(int psychologueId) {
        List<LocalDate> dates = new ArrayList<>();
        String sql =
                "SELECT DISTINCT date FROM disponibilite " +
                "WHERE psychologue_id = ? AND est_libre = 1 AND TIMESTAMP(date, heure_debut) > NOW() " +
                "ORDER BY date";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, psychologueId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                dates.add(rs.getDate("date").toLocalDate());
            }
        } catch (Exception e) {
            System.out.println("Erreur select dates disponibles par psychologue:");
            e.printStackTrace();
        }
        return dates;
    }

    public List<Disponibilite> getDisposLibresByPsychologueAndDate(int psychologueId, LocalDate date) {
        List<Disponibilite> list = new ArrayList<>();
        String sql =
                "SELECT d.*, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                "FROM disponibilite d " +
                "LEFT JOIN user u ON u.id = d.psychologue_id " +
                "WHERE d.psychologue_id = ? AND d.date = ? AND d.est_libre = 1 " +
                "AND TIMESTAMP(d.date, d.heure_debut) > NOW() " +
                "ORDER BY d.heure_debut";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, psychologueId);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapDisponibilite(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur select dispos par psy/date:");
            e.printStackTrace();
        }
        return list;
    }

    public List<Integer> getPsychologueIdsDisponiblesParDate(LocalDate date) {
        List<Integer> ids = new ArrayList<>();
        String sql =
                "SELECT DISTINCT psychologue_id FROM disponibilite " +
                "WHERE date = ? AND est_libre = 1 AND TIMESTAMP(date, heure_debut) > NOW() AND psychologue_id IS NOT NULL " +
                "ORDER BY psychologue_id";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("psychologue_id"));
            }
        } catch (Exception e) {
            System.out.println("Erreur select psychologues disponibles:");
            e.printStackTrace();
        }
        return ids;
    }

    public List<Disponibilite> getDisposLibresIncluding(int dispoId) {
        List<Disponibilite> list = new ArrayList<>();
        String sql =
                "SELECT d.*, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                "FROM disponibilite d " +
                "LEFT JOIN user u ON u.id = d.psychologue_id " +
                "WHERE (d.est_libre = 1 AND TIMESTAMP(d.date, d.heure_debut) > NOW()) OR d.id = ? " +
                "ORDER BY d.date, d.heure_debut";
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
        return executeQuery(
                "SELECT d.*, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                "FROM disponibilite d " +
                "LEFT JOIN user u ON u.id = d.psychologue_id " +
                "ORDER BY d.date DESC, d.heure_debut DESC"
        );
    }

    public List<Disponibilite> afficherToutByPsychologue(int psychologueId) {
        return executePreparedQuery(
                "SELECT d.*, CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                "FROM disponibilite d " +
                "LEFT JOIN user u ON u.id = d.psychologue_id " +
                "WHERE d.psychologue_id = ? ORDER BY d.date DESC, d.heure_debut DESC",
                psychologueId
        );
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
                        "WHERE psychologue_id = ? AND date = ? AND heure_debut < ? AND heure_fin > ?";

        if (excludedId != null) {
            sql += " AND id <> ?";
        }

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, disponibilite.getPsychologueId());
            ps.setDate(2, Date.valueOf(disponibilite.getDate()));
            ps.setTime(3, Time.valueOf(disponibilite.getHeureFin()));
            ps.setTime(4, Time.valueOf(disponibilite.getHeureDebut()));
            if (excludedId != null) {
                ps.setInt(5, excludedId);
            }

            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification disponibilite:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean estDisponiblePourReservation(int dispoId) {
        String sql =
                "SELECT COUNT(*) FROM disponibilite " +
                "WHERE id = ? AND est_libre = 1 AND TIMESTAMP(date, heure_debut) > NOW()";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, dispoId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification reservation dispo:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean appartientAuPsychologue(int dispoId, int psychologueId) {
        String sql = "SELECT COUNT(*) FROM disponibilite WHERE id = ? AND psychologue_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, dispoId);
            ps.setInt(2, psychologueId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification psychologue dispo:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean aDesReservations(int dispoId) {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE dispo_id = ? AND statut IN ('en attente', 'acceptee')";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, dispoId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erreur verification reservations dispo:");
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

    private List<Disponibilite> executePreparedQuery(String sql, int psychologueId) {
        List<Disponibilite> list = new ArrayList<>();
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, psychologueId);
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

    private Disponibilite mapDisponibilite(ResultSet rs) throws Exception {
        Disponibilite d = new Disponibilite();
        d.setId(rs.getInt("id"));
        d.setPsychologueId(rs.getInt("psychologue_id"));
        d.setPsychologueNomComplet(rs.getString("psychologue_nom"));
        d.setDate(rs.getDate("date").toLocalDate());
        d.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        d.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        d.setLibre(rs.getInt("est_libre") == 1);
        return d;
    }

    private void ensureSchema() {
        if (cnx == null) {
            return;
        }

        try {
            addColumnIfMissing("disponibilite", "est_libre", "INT NOT NULL DEFAULT 1");
            syncLegacyLibreColumn();
            addColumnIfMissing("disponibilite", "psychologue_id", "INT NULL");
            fillPsychologueIdIfMissing();
            addForeignKeyIfMissing("disponibilite", "fk_disponibilite_psychologue", "psychologue_id", "user", "id");
        } catch (SQLException e) {
            System.out.println("Erreur ensure schema disponibilite:");
            e.printStackTrace();
        }
    }

    private void fillPsychologueIdIfMissing() throws SQLException {
        String fallbackSql =
                "UPDATE disponibilite SET psychologue_id = (" +
                "SELECT id FROM user WHERE role = 'ROLE_PSYCHOLOGUE' ORDER BY id LIMIT 1" +
                ") WHERE psychologue_id IS NULL";
        try (PreparedStatement ps = cnx.prepareStatement(fallbackSql)) {
            ps.executeUpdate();
        }
    }

    private void syncLegacyLibreColumn() throws SQLException {
        if (!columnExists("disponibilite", "libre")) {
            return;
        }

        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE disponibilite SET est_libre = CASE WHEN libre IS NULL THEN est_libre WHEN libre = b'1' THEN 1 ELSE 0 END")) {
            ps.executeUpdate();
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) throws SQLException {
        if (columnExists(tableName, columnName)) {
            return;
        }
        try (PreparedStatement stmt = cnx.prepareStatement(
                "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition)) {
            stmt.executeUpdate();
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = cnx.getMetaData();
        try (ResultSet rs = metaData.getColumns(cnx.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void addForeignKeyIfMissing(String tableName, String constraintName, String columnName,
                                        String referenceTable, String referenceColumn) throws SQLException {
        if (foreignKeyExists(tableName, constraintName)) {
            return;
        }

        String sql = "ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName +
                " FOREIGN KEY (" + columnName + ") REFERENCES " + referenceTable +
                "(" + referenceColumn + ") ON DELETE SET NULL";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!"HY000".equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    private boolean foreignKeyExists(String tableName, String constraintName) throws SQLException {
        DatabaseMetaData metaData = cnx.getMetaData();
        try (ResultSet rs = metaData.getImportedKeys(cnx.getCatalog(), null, tableName)) {
            while (rs.next()) {
                if (constraintName.equalsIgnoreCase(rs.getString("FK_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
