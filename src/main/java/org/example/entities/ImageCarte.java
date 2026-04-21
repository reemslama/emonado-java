package org.example.entities;

public class ImageCarte {
    private int id;
    private int jeuId;
    private String imagePath;
    private String interpretationPsy;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJeuId() {
        return jeuId;
    }

    public void setJeuId(int jeuId) {
        this.jeuId = jeuId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getInterpretationPsy() {
        return interpretationPsy;
    }

    public void setInterpretationPsy(String interpretationPsy) {
        this.interpretationPsy = interpretationPsy;
    }

    @Override
    public String toString() {
        return imagePath;
    }
}
