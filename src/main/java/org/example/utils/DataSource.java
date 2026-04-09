package org.example.utils;

import java.sql.Connection;

public class DataSource {
    public static DataSource instance;

    private DataSource() {}

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }
}