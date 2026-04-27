package org.example.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.entities.Participation;
import org.example.entities.User;
import org.example.service.ServiceParticipation;
import org.example.utils.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class RapportParentController {

    @FXML private TableView<Participation>           tableRapports;
    @FXML private TableColumn<Participation, String> colDate;
    @FXML private TableColumn<Participation, String> colScenario;
    @FXML private TableColumn<Participation, String> colImage;
    @FXML private TableColumn<Participation, String> colEtat;
    @FXML private TableColumn<Participation, String> colReco;
    @FXML private TextArea txtRapportDetail;
    @FXML private Label    labelParentNom;
    @FXML private Label    labelAucunRapport;

    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        User parent = UserSession.getInstance();
        if (labelParentNom != null)
            labelParentNom.setText("Parent : " +
                    (parent == null ? "Inconnu" : parent.getNom() + " " + parent.getPrenom()));

        colDate.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateParticipation() != null
                        ? d.getValue().getDateParticipation().format(DATE_FMT) : "—"));
        colScenario.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getJeuTitre()));
        colImage.setCellValueFactory(d -> new ReadOnlyStringWrapper(simplifyImageLabel(d.getValue().getImagePath())));
        colEtat.setCellValueFactory(d -> new ReadOnlyStringWrapper(extraireEtat(d.getValue().getResultatPsy())));
        colReco.setCellValueFactory(d -> new ReadOnlyStringWrapper(extraireRecommandation(d.getValue().getResultatPsy())));

        tableRapports.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> afficherRapportDetail(sel));

        chargerRapports(parent);
    }

    private void chargerRapports(User parent) {
        List<Participation> toutes = serviceParticipation.afficherTout();
        List<Participation> miennes = (parent != null && parent.getId() > 0)
                ? toutes.stream().filter(p -> p.getUserId() == parent.getId()).toList()
                : toutes;

        if (miennes.isEmpty() && labelAucunRapport != null)
            labelAucunRapport.setText("Aucun rapport disponible. Votre enfant n'a pas encore passé de test.");
        else if (labelAucunRapport != null)
            labelAucunRapport.setText("");

        tableRapports.setItems(FXCollections.observableArrayList(miennes));
    }

    private void afficherRapportDetail(Participation p) {
        if (p == null) { txtRapportDetail.clear(); return; }
        String rapport = p.getResultatPsy();
        txtRapportDetail.setText(rapport == null || rapport.isBlank()
                ? "Le rapport n'est pas encore disponible."
                : rapport);
    }

    private String extraireEtat(String rapport) {
        if (rapport == null || rapport.isBlank()) return "—";
        int idx = rapport.indexOf("ÉTAT ÉMOTIONNEL");
        if (idx < 0) idx = rapport.indexOf("1.");
        if (idx < 0) return "Voir rapport";
        for (String l : rapport.substring(idx).split("\\n")) {
            l = l.trim();
            if (!l.isBlank() && l.length() > 5 && !l.contains("ÉTAT"))
                return l.length() > 50 ? l.substring(0, 47) + "…" : l;
        }
        return "Voir rapport";
    }

    private String extraireRecommandation(String rapport) {
        if (rapport == null || rapport.isBlank()) return "—";
        String lower = rapport.toLowerCase();
        if (lower.contains("consultation recommandée") || lower.contains("consulter un psychologue"))
            return "👨‍⚕️ Consultation conseillée";
        if (lower.contains("non nécessaire") || lower.contains("pas nécessaire"))
            return "✅ Aucune visite requise";
        if (lower.contains("à surveiller") || lower.contains("à considérer"))
            return "⚠️ À surveiller";
        return "Voir rapport";
    }

    private String simplifyImageLabel(String path) {
        if (path == null || path.isBlank()) return "(image)";
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String f = idx >= 0 ? path.substring(idx + 1) : path;
        int dot = f.lastIndexOf('.');
        if (dot > 0) f = f.substring(0, dot);
        f = f.replace('_', ' ').replace('-', ' ');
        return f.isEmpty() ? "(image)" : Character.toUpperCase(f.charAt(0)) + f.substring(1);
    }
}