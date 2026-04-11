package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class UserRepository {
    private final DatabaseManager dbManager;
    private static final Logger log = Logger.getLogger(UserRepository.class.getName());

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void ensureDefaultUserExists() throws SQLException {

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE id = 1")) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                log.info("Default user already exists, skipping creation.");
                return;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (id, name) VALUES (1, ?)")) {
                insertStmt.setString(1, "Default User");
                insertStmt.executeUpdate();
                log.info("Default user created.");
            }

        }
    }
}

