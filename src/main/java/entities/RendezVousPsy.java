package entities;

public class RendezVousPsy {

    private String nom;
    private String prenom;
    private String typeRdv;
    private String date;
    private String heureDebut;
    private String heureFin;

    // getters & setters

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTypeRdv() { return typeRdv; }
    public void setTypeRdv(String typeRdv) { this.typeRdv = typeRdv; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }

    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
}