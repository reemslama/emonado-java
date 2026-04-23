package org.example.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.example.controller.AdminDashboardController;
import org.example.controller.PatientDashboardController;
import org.example.controller.PsyDashboardController;
import org.example.entities.User;
import org.example.utils.UserSession;

import java.io.IOException;

public final class AppNavigationService {
    private AppNavigationService() {
    }

    public static void goToDashboard(Scene scene, User user) {
        if (scene == null || user == null) {
            return;
        }

        try {
            UserSession.setInstance(user);
            String role = normalizeRole(user.getRole());
            String fxmlPath;
            if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
                fxmlPath = "/admin_dashboard.fxml";
            } else if ("ROLE_PSYCHOLOGUE".equalsIgnoreCase(role)) {
                fxmlPath = "/psy_dashboard.fxml";
            } else if (user.isHasChild()) {
                fxmlPath = "/EspaceEnfant.fxml";
            } else {
                fxmlPath = "/patient_dashboard.fxml";
            }

            FXMLLoader loader = new FXMLLoader(AppNavigationService.class.getResource(fxmlPath));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof PatientDashboardController patientDashboardController) {
                patientDashboardController.setUserData(user);
            } else if (controller instanceof PsyDashboardController psyDashboardController) {
                psyDashboardController.setUserData(user);
            } else if (controller instanceof AdminDashboardController adminDashboardController) {
                adminDashboardController.setUserData(user);
            }

            scene.setRoot(view);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de chargement du dashboard : " + e.getMessage(), e);
        }
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "ROLE_PATIENT";
        }

        String value = role.trim().toUpperCase();
        return switch (value) {
            case "ROLE_ADMIN", "ADMIN" -> "ROLE_ADMIN";
            case "ROLE_PSYCHOLOGUE", "PSYCHOLOGUE", "PSY", "ROLE_PSY" -> "ROLE_PSYCHOLOGUE";
            case "ROLE_PATIENT", "PATIENT", "USER" -> "ROLE_PATIENT";
            default -> value.startsWith("ROLE_") ? value : "ROLE_" + value;
        };
    }
}
