package org.example.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class IntegrationConfigService {
    private IntegrationConfigService() {
    }

    public static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        Path configPath = Paths.get("config", fileName);
        if (!Files.exists(configPath)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("[IntegrationConfigService] Impossible de lire " + configPath + " : " + e.getMessage());
        }
        return properties;
    }

    public static String read(Properties properties, String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return defaultValue;
    }

    public static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        String value = read(properties, key, "");
        if (value.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }
}
