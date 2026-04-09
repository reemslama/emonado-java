package org.example.utils;
import org.example.entities.User;

public class UserSession {
    private static User instance;
    public static void setInstance(User user) { instance = user; }
    public static User getInstance() { return instance; }
}