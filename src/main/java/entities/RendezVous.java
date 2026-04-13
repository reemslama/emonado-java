package entities;

public class RendezVous {

    private int id;
    private int age;
    private String adresse;
    private int typeId;
    private int dispoId;
    private int userId;
    private String typeLibelle;
    private String dateDisponibilite;
    private String heureDebut;
    private String heureFin;

    public RendezVous() {
    }

    public RendezVous(int age, String adresse, int typeId, int dispoId, int userId) {
        this.age = age;
        this.adresse = adresse;
        this.typeId = typeId;
        this.dispoId = dispoId;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getDispoId() {
        return dispoId;
    }

    public void setDispoId(int dispoId) {
        this.dispoId = dispoId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public void setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
    }

    public String getDateDisponibilite() {
        return dateDisponibilite;
    }

    public void setDateDisponibilite(String dateDisponibilite) {
        this.dateDisponibilite = dateDisponibilite;
    }

    public String getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(String heureDebut) {
        this.heureDebut = heureDebut;
    }

    public String getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(String heureFin) {
        this.heureFin = heureFin;
    }
}
