package services;

import entities.RendezVous;
import entities.RendezVousPsy;
import org.example.service.MedicalDataService;
import org.example.service.MedicalValidationService;
import org.example.utils.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {
    private static final String STATUT_EN_ATTENTE = "en attente";
    private static final String STATUT_ACCEPTEE = "acceptee";
    private static final String STATUT_REJETEE = "rejetee";

    private final Connection cnx = DataSource.getInstance().getConnection();
    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();
    private final MedicalDataService medicalDataService = new MedicalDataService();
    private String lastValidationError = "";

    public ServiceRendezVous() {
        ensureSchema();
    }

    public boolean ajouter(RendezVous r) {
        if (r == null) {
            lastValidationError = "Donnees du rendez-vous invalides";
            return false;
        }
        if (!typeAutorise(r.getTypeId())) {
            lastValidationError = "Type de rendez-vous invalide";
            return false;
        }
        String patientNoteError = MedicalValidationService.validatePatientRendezVousNote(r.getNotesPatient());
        if (patientNoteError != null) {
            lastValidationError = patientNoteError;
            return false;
        }

        String sql = "INSERT INTO rendez_vous(age, adresse, type_id, dispo_id, user_id, statut, notes_patient, notes_psychologue) VALUES (?,?,?,?,?,?,?,?)";
        boolean initialAutoCommit = true;
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            ValidationReservation validation = validerReservation(r.getUserId(), r.getDispoId(), null, null);
            if (!validation.allowed) {
                cnx.rollback();
                lastValidationError = validation.message;
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, r.getAge());
                ps.setString(2, r.getAdresse());
                ps.setInt(3, r.getTypeId());
                ps.setInt(4, r.getDispoId());
                ps.setInt(5, r.getUserId());
                ps.setString(6, STATUT_EN_ATTENTE);
                ps.setString(7, MedicalValidationService.normalize(r.getNotesPatient()));
                ps.setString(8, MedicalValidationService.normalize(r.getNotesPsychologue()));
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Reservation impossible";
                    return false;
                }
            }

            updateDisponibiliteState(r.getDispoId(), false);
            cnx.commit();
            lastValidationError = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("ERREUR INSERT:");
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la reservation";
            return false;
        } finally {
            restoreAutoCommit(initialAutoCommit);
        }
    }

    public boolean modifier(RendezVous r) {
        if (r == null) {
            lastValidationError = "Donnees du rendez-vous invalides";
            return false;
        }
        if (!typeAutorise(r.getTypeId())) {
            lastValidationError = "Type de rendez-vous invalide";
            return false;
        }
        String patientNoteError = MedicalValidationService.validatePatientRendezVousNote(r.getNotesPatient());
        if (patientNoteError != null) {
            lastValidationError = patientNoteError;
            return false;
        }

        String sql = "UPDATE rendez_vous SET age = ?, adresse = ?, type_id = ?, dispo_id = ?, user_id = ?, statut = ?, notes_patient = ?, notes_psychologue = ? WHERE id = ?";
        boolean initialAutoCommit = true;
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            RendezVous existing = getRendezVousByIdForUpdate(r.getId());
            if (existing == null) {
                cnx.rollback();
                lastValidationError = "Rendez-vous introuvable";
                return false;
            }

            ValidationReservation validation = validerReservation(r.getUserId(), r.getDispoId(), r.getId(), existing.getDispoId());
            if (!validation.allowed) {
                cnx.rollback();
                lastValidationError = validation.message;
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, r.getAge());
                ps.setString(2, r.getAdresse());
                ps.setInt(3, r.getTypeId());
                ps.setInt(4, r.getDispoId());
                ps.setInt(5, r.getUserId());
                ps.setString(6, STATUT_EN_ATTENTE);
                ps.setString(7, MedicalValidationService.normalize(r.getNotesPatient()));
                ps.setString(8, MedicalValidationService.normalize(existing.getNotesPsychologue()));
                ps.setInt(9, r.getId());
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Modification impossible";
                    return false;
                }
            }

            if (existing.getDispoId() != r.getDispoId()) {
                updateDisponibiliteState(existing.getDispoId(), true);
                updateDisponibiliteState(r.getDispoId(), false);
            }

            cnx.commit();
            lastValidationError = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("ERREUR UPDATE:");
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la modification";
            return false;
        } finally {
            restoreAutoCommit(initialAutoCommit);
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM rendez_vous WHERE id = ?";
        boolean initialAutoCommit = true;
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            RendezVous existing = getRendezVousByIdForUpdate(id);
            if (existing == null) {
                cnx.rollback();
                lastValidationError = "Rendez-vous introuvable";
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, id);
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Suppression impossible";
                    return false;
                }
            }

            if (!hasActiveReservationOnDispo(existing.getDispoId(), null)) {
                updateDisponibiliteState(existing.getDispoId(), true);
            }

            cnx.commit();
            lastValidationError = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("ERREUR DELETE:");
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la suppression";
            return false;
        } finally {
            restoreAutoCommit(initialAutoCommit);
        }
    }

    public List<RendezVous> getRendezVousByUser(int userId) {
        List<RendezVous> list = new ArrayList<>();
        String sql =
                "SELECT r.id, r.age, r.adresse, r.type_id, r.dispo_id, r.user_id, r.statut, r.notes_patient, r.notes_psychologue, d.psychologue_id, " +
                        "t.libelle AS type_rdv, d.date, d.heure_debut, d.heure_fin " +
                        ", CONCAT(u.prenom, ' ', u.nom) AS psychologue_nom " +
                        "FROM rendez_vous r " +
                        "JOIN type_rendez_vous t ON r.type_id = t.id " +
                        "JOIN disponibilite d ON r.dispo_id = d.id " +
                        "LEFT JOIN user u ON u.id = d.psychologue_id " +
                        "WHERE r.user_id = ? ORDER BY d.date DESC, d.heure_debut DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRendezVous(rs));
            }
        } catch (SQLException e) {
            System.out.println("ERREUR SELECT USER:");
            e.printStackTrace();
        }
        return list;
    }

    public List<RendezVousPsy> getRendezVousForPsy(int psychologueId) {
        List<RendezVousPsy> list = new ArrayList<>();
        String sql =
                "SELECT r.id, r.age, r.adresse, r.type_id, r.dispo_id, r.user_id, r.statut, r.notes_patient, r.notes_psychologue, " +
                        "u.nom, u.prenom, t.libelle AS type_rdv, d.date, d.heure_debut, d.heure_fin " +
                        "FROM rendez_vous r " +
                        "JOIN user u ON r.user_id = u.id " +
                        "JOIN type_rendez_vous t ON r.type_id = t.id " +
                        "JOIN disponibilite d ON r.dispo_id = d.id " +
                        "WHERE d.psychologue_id = ? " +
                        "ORDER BY d.date DESC, d.heure_debut DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, psychologueId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RendezVousPsy r = new RendezVousPsy();
                r.setId(rs.getInt("id"));
                r.setAge(rs.getInt("age"));
                r.setAdresse(rs.getString("adresse"));
                r.setTypeId(rs.getInt("type_id"));
                r.setDispoId(rs.getInt("dispo_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setNom(rs.getString("nom"));
                r.setPrenom(rs.getString("prenom"));
                r.setTypeRdv(rs.getString("type_rdv"));
                r.setDate(String.valueOf(rs.getDate("date")));
                r.setHeureDebut(String.valueOf(rs.getTime("heure_debut")));
                r.setHeureFin(String.valueOf(rs.getTime("heure_fin")));
                r.setStatut(rs.getString("statut"));
                r.setNotesPatient(rs.getString("notes_patient"));
                r.setNotesPsychologue(rs.getString("notes_psychologue"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("ERREUR SELECT PSY:");
            e.printStackTrace();
        }
        return list;
    }

    public boolean validerRendezVous(int rendezVousId, int psychologueId) {
        String sql =
                "UPDATE rendez_vous r " +
                "JOIN disponibilite d ON d.id = r.dispo_id " +
                "SET r.statut = ? " +
                "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, STATUT_ACCEPTEE);
            ps.setInt(2, rendezVousId);
            ps.setInt(3, psychologueId);
            ps.setString(4, STATUT_EN_ATTENTE);
            boolean updated = ps.executeUpdate() > 0;
            if (!updated) {
                lastValidationError = "Validation impossible";
                return false;
            }
            medicalDataService.ensureSchema();
            medicalDataService.createConsultationFromAcceptedRendezVous(rendezVousId);
            lastValidationError = "";
            return true;
        } catch (SQLException e) {
            System.out.println("ERREUR VALIDATION RDV:");
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de la validation";
            return false;
        }
    }

    public boolean rejeterRendezVous(int rendezVousId, int psychologueId) {
        String sql =
                "UPDATE rendez_vous r " +
                "JOIN disponibilite d ON d.id = r.dispo_id " +
                "SET r.statut = ? " +
                "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ?";
        boolean initialAutoCommit = true;
        try {
            initialAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            Integer dispoId = getDispoIdForPendingRendezVous(rendezVousId, psychologueId);
            if (dispoId == null) {
                cnx.rollback();
                lastValidationError = "Rejet impossible";
                return false;
            }

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, STATUT_REJETEE);
                ps.setInt(2, rendezVousId);
                ps.setInt(3, psychologueId);
                ps.setString(4, STATUT_EN_ATTENTE);
                if (ps.executeUpdate() <= 0) {
                    cnx.rollback();
                    lastValidationError = "Rejet impossible";
                    return false;
                }
            }

            if (!hasActiveReservationOnDispo(dispoId, rendezVousId)) {
                updateDisponibiliteState(dispoId, true);
            }

            cnx.commit();
            lastValidationError = "";
            return true;
        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("ERREUR REJET RDV:");
            e.printStackTrace();
            lastValidationError = "Erreur technique lors du rejet";
            return false;
        } finally {
            restoreAutoCommit(initialAutoCommit);
        }
    }

    public boolean updatePsychologueNote(int rendezVousId, int psychologueId, String note) {
        String validationError = MedicalValidationService.validatePsychologueNote(note);
        if (validationError != null) {
            lastValidationError = validationError;
            return false;
        }

        String sql = "UPDATE rendez_vous r " +
                "JOIN disponibilite d ON d.id = r.dispo_id " +
                "SET r.notes_psychologue = ? " +
                "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, MedicalValidationService.normalize(note));
            ps.setInt(2, rendezVousId);
            ps.setInt(3, psychologueId);
            ps.setString(4, STATUT_ACCEPTEE);
            boolean updated = ps.executeUpdate() > 0;
            lastValidationError = updated ? "" : "La note psychologue ne peut etre ajoutee que sur un rendez-vous accepte";
            return updated;
        } catch (SQLException e) {
            System.out.println("ERREUR NOTE PSY RDV:");
            e.printStackTrace();
            lastValidationError = "Erreur technique lors de l'enregistrement de la note psychologue";
            return false;
        }
    }

    public boolean typeAutorise(int typeId) {
        String sql = "SELECT libelle FROM type_rendez_vous WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, typeId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return false;
            }
            String libelle = rs.getString("libelle");
            if (libelle == null) {
                return false;
            }
            String normalized = libelle.trim().toLowerCase();
            return normalized.equals("suivi") || normalized.equals("consultation");
        } catch (SQLException e) {
            System.out.println("ERREUR TYPE RDV:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean patientHasRendezVousAtSameTime(int userId, int dispoId, Integer excludedRendezVousId) {
        String sql =
                "SELECT COUNT(*) " +
                "FROM rendez_vous r " +
                "JOIN disponibilite cible ON cible.id = ? " +
                "JOIN disponibilite d ON d.id = r.dispo_id " +
                "WHERE r.user_id = ? " +
                "AND r.statut IN (?, ?) " +
                "AND d.date = cible.date " +
                "AND d.heure_debut < cible.heure_fin " +
                "AND d.heure_fin > cible.heure_debut";

        if (excludedRendezVousId != null) {
            sql += " AND r.id <> ?";
        }

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, dispoId);
            ps.setInt(2, userId);
            ps.setString(3, STATUT_EN_ATTENTE);
            ps.setString(4, STATUT_ACCEPTEE);
            if (excludedRendezVousId != null) {
                ps.setInt(5, excludedRendezVousId);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("ERREUR VERIF RDV PATIENT:");
            e.printStackTrace();
            return true;
        }
    }

    public boolean peutReserver(int userId, int dispoId, Integer excludedRendezVousId) {
        return serviceDisponibilite.estDisponiblePourReservation(dispoId)
                && !patientHasRendezVousAtSameTime(userId, dispoId, excludedRendezVousId);
    }

    public String getLastValidationError() {
        return lastValidationError;
    }

    private RendezVous mapRendezVous(ResultSet rs) throws SQLException {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setId(rs.getInt("id"));
        rendezVous.setAge(rs.getInt("age"));
        rendezVous.setAdresse(rs.getString("adresse"));
        rendezVous.setTypeId(rs.getInt("type_id"));
        rendezVous.setDispoId(rs.getInt("dispo_id"));
        rendezVous.setUserId(rs.getInt("user_id"));
        rendezVous.setTypeLibelle(rs.getString("type_rdv"));
        rendezVous.setDateDisponibilite(String.valueOf(rs.getDate("date")));
        rendezVous.setHeureDebut(String.valueOf(rs.getTime("heure_debut")));
        rendezVous.setHeureFin(String.valueOf(rs.getTime("heure_fin")));
        rendezVous.setStatut(rs.getString("statut"));
        rendezVous.setNotesPatient(rs.getString("notes_patient"));
        rendezVous.setNotesPsychologue(rs.getString("notes_psychologue"));
        rendezVous.setPsychologueId(rs.getInt("psychologue_id"));
        rendezVous.setPsychologueNomComplet(rs.getString("psychologue_nom"));
        return rendezVous;
    }

    private void ensureSchema() {
        if (cnx == null) {
            return;
        }

        try {
            addColumnIfMissing("rendez_vous", "statut", "VARCHAR(30) NOT NULL DEFAULT 'en attente'");
            addColumnIfMissing("rendez_vous", "notes_patient", "TEXT NULL");
            addColumnIfMissing("rendez_vous", "notes_psychologue", "TEXT NULL");
            backfillStatut();
        } catch (SQLException e) {
            System.out.println("Erreur ensure schema rendez_vous:");
            e.printStackTrace();
        }
    }

    private void backfillStatut() throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE rendez_vous SET statut = ? WHERE statut IS NULL OR TRIM(statut) = ''")) {
            ps.setString(1, STATUT_EN_ATTENTE);
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

    private ValidationReservation validerReservation(int userId, int dispoId, Integer excludedRendezVousId, Integer currentDispoId)
            throws SQLException {
        DispoReservationState dispoState = getDispoReservationStateForUpdate(dispoId);
        if (dispoState == null) {
            return ValidationReservation.denied("Le creneau selectionne n'existe pas");
        }
        if (!dispoState.future) {
            return ValidationReservation.denied("Impossible de reserver un creneau dans le passe");
        }
        if (!dispoState.free && (currentDispoId == null || dispoId != currentDispoId)) {
            return ValidationReservation.denied("Ce creneau est deja reserve");
        }
        if (patientHasRendezVousAtSameTime(userId, dispoId, excludedRendezVousId)) {
            return ValidationReservation.denied("Le patient a deja un rendez-vous sur ce creneau");
        }
        return ValidationReservation.allowed();
    }

    private DispoReservationState getDispoReservationStateForUpdate(int dispoId) throws SQLException {
        String sql = "SELECT est_libre, TIMESTAMP(date, heure_debut) > NOW() AS est_future FROM disponibilite WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, dispoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new DispoReservationState(rs.getInt("est_libre") == 1, rs.getInt("est_future") == 1);
            }
        }
    }

    private RendezVous getRendezVousByIdForUpdate(int id) throws SQLException {
        String sql = "SELECT id, age, adresse, type_id, dispo_id, user_id, statut, notes_patient, notes_psychologue FROM rendez_vous WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                RendezVous rendezVous = new RendezVous();
                rendezVous.setId(rs.getInt("id"));
                rendezVous.setAge(rs.getInt("age"));
                rendezVous.setAdresse(rs.getString("adresse"));
                rendezVous.setTypeId(rs.getInt("type_id"));
                rendezVous.setDispoId(rs.getInt("dispo_id"));
                rendezVous.setUserId(rs.getInt("user_id"));
                rendezVous.setStatut(rs.getString("statut"));
                rendezVous.setNotesPatient(rs.getString("notes_patient"));
                rendezVous.setNotesPsychologue(rs.getString("notes_psychologue"));
                return rendezVous;
            }
        }
    }

    private Integer getDispoIdForPendingRendezVous(int rendezVousId, int psychologueId) throws SQLException {
        String sql = "SELECT r.dispo_id FROM rendez_vous r " +
                "JOIN disponibilite d ON d.id = r.dispo_id " +
                "WHERE r.id = ? AND d.psychologue_id = ? AND r.statut = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            ps.setInt(2, psychologueId);
            ps.setString(3, STATUT_EN_ATTENTE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("dispo_id") : null;
            }
        }
    }

    private boolean hasActiveReservationOnDispo(int dispoId, Integer excludedRendezVousId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE dispo_id = ? AND statut IN (?, ?)";
        if (excludedRendezVousId != null) {
            sql += " AND id <> ?";
        }
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, dispoId);
            ps.setString(2, STATUT_EN_ATTENTE);
            ps.setString(3, STATUT_ACCEPTEE);
            if (excludedRendezVousId != null) {
                ps.setInt(4, excludedRendezVousId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void updateDisponibiliteState(int dispoId, boolean libre) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("UPDATE disponibilite SET est_libre = ? WHERE id = ?")) {
            ps.setInt(1, libre ? 1 : 0);
            ps.setInt(2, dispoId);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly() {
        try {
            if (cnx != null && !cnx.getAutoCommit()) {
                cnx.rollback();
            }
        } catch (SQLException rollbackException) {
            System.out.println("ERREUR ROLLBACK:");
            rollbackException.printStackTrace();
        }
    }

    private void restoreAutoCommit(boolean initialAutoCommit) {
        try {
            if (cnx != null) {
                cnx.setAutoCommit(initialAutoCommit);
            }
        } catch (SQLException e) {
            System.out.println("ERREUR AUTO COMMIT:");
            e.printStackTrace();
        }
    }

    private static final class ValidationReservation {
        private final boolean allowed;
        private final String message;

        private ValidationReservation(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        private static ValidationReservation allowed() {
            return new ValidationReservation(true, "");
        }

        private static ValidationReservation denied(String message) {
            return new ValidationReservation(false, message);
        }
    }

    private static final class DispoReservationState {
        private final boolean free;
        private final boolean future;

        private DispoReservationState(boolean free, boolean future) {
            this.free = free;
            this.future = future;
        }
    }
}
