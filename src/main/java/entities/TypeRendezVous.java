package entities;

public class TypeRendezVous {

    private int id;
    private String libelle;

    public TypeRendezVous() {}

    public TypeRendezVous(int id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public int getId() { return id; }
    public String getLibelle() { return libelle; }

    public void setId(int id) { this.id = id; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public String toString() {
        return libelle;
    }
}