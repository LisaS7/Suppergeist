package com.example.suppergeist.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private Path tempDb;
    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() throws IOException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    @Test
    void getConnection_returnsOpenConnection() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void getConnection_enablesForeignKeys() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void init_createsAllTables() throws SQLException {
        dbManager.init();

        List<String> tables = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("meals"));
        assertTrue(tables.contains("ingredients"));
        assertTrue(tables.contains("meal_ingredients"));
        assertTrue(tables.contains("meal_plans"));
    }

    @Test
    void init_isIdempotent() throws SQLException {
        assertDoesNotThrow(() -> {
            dbManager.init();
            dbManager.init();
        });
    }
}
