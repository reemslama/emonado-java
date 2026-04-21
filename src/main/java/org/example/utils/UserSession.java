package org.example.utils;

import org.example.entities.User;

public class UserSession {

    private static User currentUser;

    public static void setInstance(User user) {
        currentUser = user;
    }

    public static User getInstance() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}