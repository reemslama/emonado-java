package entities;

public class TypeRendezVous {
    private int id;
    private String libelle;

    // Constructeur vide
    public TypeRendezVous() {}

    // Constructeur avec paramètres
    public TypeRendezVous(int id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    // Getters et Setters (indispensables pour JavaFX)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    // Pour afficher le nom dans les listes déroulantes plus tard
    @Override
    public String toString() {
        return libelle;
    }
}