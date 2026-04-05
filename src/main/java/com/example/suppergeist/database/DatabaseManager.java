package com.example.suppergeist.database;
import java.sql.Connection;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final Path dbPath = Path.of("app.db");

    public Connection getConnection() throws SQLException {
        var url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        return DriverManager.getConnection(url);
    }
}
