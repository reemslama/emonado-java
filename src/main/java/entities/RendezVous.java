package entities;

public class RendezVous {

    private int age;
    private String adresse;

    private int typeId;
    private int dispoId;
    private int userId; // 🔥 liaison avec user

    public RendezVous() {}

    // ✔️ constructeur corrigé
    public RendezVous(int age, String adresse, int typeId, int dispoId, int userId) {
        this.age = age;
        this.adresse = adresse;
        this.typeId = typeId;
        this.dispoId = dispoId;
        this.userId = userId;
    }

    // getters & setters

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
}