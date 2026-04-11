package entities;

public class RendezVous {

    private String nom;
    private String prenom;
    private int age;
    private String adresse;

    private int typeId;
    private int dispoId;

    public RendezVous() {}

    // ✔ constructeur utilisé dans ton controller
    public RendezVous(String nom, String prenom, int age, String adresse, int typeId, int dispoId) {
        this.nom = nom;
        this.prenom = prenom;
        this.age = age;
        this.adresse = adresse;
        this.typeId = typeId;
        this.dispoId = dispoId;
    }

    // getters & setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public int getDispoId() { return dispoId; }
    public void setDispoId(int dispoId) { this.dispoId = dispoId; }
}