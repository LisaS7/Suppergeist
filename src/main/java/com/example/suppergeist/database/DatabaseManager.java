package com.example.suppergeist.database;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

public class DatabaseManager {
    private final Path dbPath;
    private static final Logger log = Logger.getLogger(DatabaseManager.class.getName());

    public DatabaseManager() {
        this(Path.of(System.getProperty("user.home"), ".suppergeist", "app.db"));
    }

    public DatabaseManager(Path path) {
        this.dbPath = path;
    }

    public Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(this.dbPath.getParent());
        } catch (IOException e) {
            log.severe("Failed to create database directory: " + dbPath.getParent());
            throw new SQLException("Failed to create database directory", e);
        }

        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        
        return conn;
    }

    public void init() throws SQLException {
        List<String> statements = List.of(
                Schema.CREATE_USERS,
                Schema.CREATE_MEALS,
                Schema.CREATE_INGREDIENTS,
                Schema.CREATE_INDEX_INGREDIENTS,
                Schema.CREATE_MEAL_INGREDIENTS,
                Schema.CREATE_INDEX_MEAL_INGREDIENTS,
                Schema.CREATE_MEAL_PLANS,
                Schema.CREATE_INDEX_MEAL_PLANS_USER_WEEK,
                Schema.CREATE_MEAL_PLAN_ENTRIES,
                Schema.CREATE_INDEX_MEAL_PLAN_ENTRIES
        );

        log.info("Starting schema creation");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                stmt.execute(sql);
            }

            log.info("Schema creation complete");
        }
    }
}
