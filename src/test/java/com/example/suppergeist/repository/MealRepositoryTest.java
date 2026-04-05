package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Meal;
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

class MealRepositoryTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private MealRepository repository;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS meals (
                    id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    name  TEXT    NOT NULL
                )
            """);
        }

        repository = new MealRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    @Test
    void getMealById_returnsMeal_whenFound() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO meals (name) VALUES ('Spaghetti Bolognese')"
            );
        }

        Optional<Meal> result = repository.getMealById(1);

        assertTrue(result.isPresent());
        assertEquals("Spaghetti Bolognese", result.get().getName());
    }

    @Test
    void getMealById_returnsEmpty_whenNotFound() throws SQLException {
        Optional<Meal> result = repository.getMealById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMealById_mapsIdCorrectly() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO meals (name) VALUES ('Chicken Stir Fry')"
            );
        }

        Optional<Meal> result = repository.getMealById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void getAllMeals_returnsEmptyList_whenTableIsEmpty() throws SQLException {
        List<Meal> results = repository.getAllMeals();

        assertTrue(results.isEmpty());
    }

    @Test
    void getAllMeals_returnsAllMeals() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                INSERT INTO meals (name) VALUES
                    ('Porridge'),
                    ('Lentil Soup'),
                    ('Salmon and Rice')
            """);
        }

        List<Meal> results = repository.getAllMeals();

        assertEquals(3, results.size());
    }

    @Test
    void getAllMeals_mapsFieldsCorrectly() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                "INSERT INTO meals (name) VALUES ('Vegetable Curry')"
            );
        }

        List<Meal> results = repository.getAllMeals();

        assertEquals(1, results.size());
        Meal meal = results.get(0);
        assertEquals(1, meal.getId());
        assertEquals("Vegetable Curry", meal.getName());
    }
}
