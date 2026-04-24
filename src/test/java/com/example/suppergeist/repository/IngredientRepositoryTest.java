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
import java.util.List;
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
        dbManager.init();

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

    @Test
    void getAllIngredients_returnsEmptyList_whenTableIsEmpty() throws SQLException {
        List<Ingredient> results = repository.getAllIngredients();

        assertTrue(results.isEmpty());
    }

    @Test
    void getAllIngredients_returnsAllIngredients() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                INSERT INTO ingredients (name, food_code) VALUES
                    ('Almonds', '11-002'),
                    ('Broccoli', '13-201'),
                    ('Chicken Breast', '16-511')
            """);
        }

        List<Ingredient> results = repository.getAllIngredients();

        assertEquals(3, results.size());
    }

    @Test
    void getAllIngredients_mapsFieldsCorrectly() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO ingredients (name, food_code) VALUES ('Oats', '62-801')"
            );
        }

        List<Ingredient> results = repository.getAllIngredients();

        assertEquals(1, results.size());
        Ingredient ingredient = results.get(0);
        assertEquals("Oats", ingredient.getName());
        assertEquals("62-801", ingredient.getFoodCode());
    }

    @Test
    void getIngredientById_mapsNutritionFields_whenPresent() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO ingredients (name, food_code, energy_kcal, protein_g, fat_g, carbohydrate_g) " +
                "VALUES ('Almonds', '11-002', 579.0, 21.2, 49.9, 21.6)"
            );
        }

        Optional<Ingredient> result = repository.getIngredientById(1);

        assertTrue(result.isPresent());
        assertEquals(579.0, result.get().getEnergyKcal());
        assertEquals(21.2, result.get().getProteinG());
        assertEquals(49.9, result.get().getFatG());
        assertEquals(21.6, result.get().getCarbohydrateG());
    }

    @Test
    void getIngredientById_returnsNullNutritionFields_whenAbsent() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO ingredients (name, food_code) VALUES ('Unknown Herb', NULL)"
            );
        }

        Optional<Ingredient> result = repository.getIngredientById(1);

        assertTrue(result.isPresent());
        assertNull(result.get().getEnergyKcal());
        assertNull(result.get().getProteinG());
    }

    @Test
    void getAllIngredients_handlesNullFoodCode() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO ingredients (name, food_code) VALUES ('Mystery Spice', NULL)"
            );
        }

        List<Ingredient> results = repository.getAllIngredients();

        assertEquals(1, results.size());
        assertNull(results.get(0).getFoodCode());
    }
}
