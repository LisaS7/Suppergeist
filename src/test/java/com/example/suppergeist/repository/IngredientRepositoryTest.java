package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IngredientRepositoryTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private IngredientRepository repository;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS ingredients (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT    NOT NULL UNIQUE,
                    food_code       TEXT,
                    energy_kcal     REAL,
                    protein_g       REAL,
                    fat_g           REAL,
                    carbohydrate_g  REAL
                )
            """);
        }

        repository = new IngredientRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    @Test
    void getIngredientById_returnsIngredient_whenFound() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO ingredients (name, food_code) VALUES ('Almonds', '11-002')"
            );
        }

        Optional<Ingredient> result = repository.getIngredientById(1);

        assertTrue(result.isPresent());
        assertEquals("Almonds", result.get().getName());
        assertEquals("11-002", result.get().getFoodCode());
    }

    @Test
    void getIngredientById_returnsEmpty_whenNotFound() throws SQLException {
        Optional<Ingredient> result = repository.getIngredientById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIngredientById_handlesNullFoodCode() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO ingredients (name, food_code) VALUES ('Unknown Herb', NULL)"
            );
        }

        Optional<Ingredient> result = repository.getIngredientById(1);

        assertTrue(result.isPresent());
        assertEquals("Unknown Herb", result.get().getName());
        assertNull(result.get().getFoodCode());
    }
}
