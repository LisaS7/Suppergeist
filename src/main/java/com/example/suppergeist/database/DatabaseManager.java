package com.example.suppergeist.database;

import java.sql.Connection;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final Path dbPath;

    public DatabaseManager() {
        this(Path.of("app.db"));
    }

    public DatabaseManager(Path path) {
        this.dbPath = path;
    }

    public Connection getConnection() throws SQLException {
        var url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        return DriverManager.getConnection(url);
    }
}
