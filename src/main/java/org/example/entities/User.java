package org.example.entities;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDate;

public class User {

    private int id;

    private String nom;
    private String prenom;
    private String email;
    private String roles;
    private String password;
    private String role;

    private String telephone;
    private String sexe;
    private String specialite;

    private String avatar;
    private String faceIdImagePath;

    public LocalDate date_naissance;

    private boolean hasChild;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoles() {
        if (roles != null && !roles.isBlank()) {
            return roles;
        }
        if (role == null || role.isBlank()) {
            return null;
        }
        return new JSONArray().put(role).toString();
    }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() {
        if (role != null && !role.isBlank()) {
            return role;
        }
        return extractPrimaryRole(roles);
    }
    public void setRole(String role) { this.role = role; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getFaceIdImagePath() { return faceIdImagePath; }
    public void setFaceIdImagePath(String faceIdImagePath) { this.faceIdImagePath = faceIdImagePath; }

    public LocalDate getdate_naissance() { return date_naissance; }
    public void setdate_naissance(LocalDate date_naissance) { this.date_naissance = date_naissance; }

    public boolean isHasChild() { return hasChild; }
    public void setHasChild(boolean hasChild) { this.hasChild = hasChild; }

    private static String extractPrimaryRole(String rolesValue) {
        if (rolesValue == null || rolesValue.isBlank()) {
            return null;
        }

        try {
            JSONArray rolesArray = new JSONArray(rolesValue);
            for (int i = 0; i < rolesArray.length(); i++) {
                String value = rolesArray.optString(i, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (JSONException ignored) {
        }

        String sanitized = rolesValue.replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();
        if (sanitized.isEmpty()) {
            return null;
        }

        int commaIndex = sanitized.indexOf(',');
        return commaIndex >= 0 ? sanitized.substring(0, commaIndex).trim() : sanitized;
    }
}
