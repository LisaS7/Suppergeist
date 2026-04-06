package com.example.suppergeist.database;

import java.sql.Connection;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {
    private final Path dbPath;
    private static final Logger log = Logger.getLogger(DatabaseManager.class.getName());

    public DatabaseManager() {
        this(Path.of("app.db"));
    }

    public DatabaseManager(Path path) {
        this.dbPath = path;
    }

    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        Connection conn = DriverManager.getConnection(url);
        log.info("Opened connection to " + dbPath.toAbsolutePath());
        return conn;
    }
}
