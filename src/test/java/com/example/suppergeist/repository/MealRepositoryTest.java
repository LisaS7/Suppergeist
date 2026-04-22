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

        dbManager.init();

        repository = new MealRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    @Test
    void getMealById_returns_whenFound() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                    "INSERT INTO meals (name) VALUES ('Spaghetti Bolognese')"
            );
        }

        Optional<Meal> result = repository.getById(1);

        assertTrue(result.isPresent());
        assertEquals("Spaghetti Bolognese", result.get().getName());
    }

    @Test
    void getById_returnsEmpty_whenNotFound() throws SQLException {
        Optional<Meal> result = repository.getById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void getById_mapsIdCorrectly() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                    "INSERT INTO meals (name) VALUES ('Chicken Stir Fry')"
            );
        }

        Optional<Meal> result = repository.getById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void getAll_returnsEmptyList_whenTableIsEmpty() throws SQLException {
        List<Meal> results = repository.getAll();

        assertTrue(results.isEmpty());
    }

    @Test
    void getAllMeals_returnsAll() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                        INSERT INTO meals (name) VALUES
                            ('Porridge'),
                            ('Lentil Soup'),
                            ('Salmon and Rice')
                    """);
        }

        List<Meal> results = repository.getAll();

        assertEquals(3, results.size());
    }

    @Test
    void getAll_mapsFieldsCorrectly() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute(
                    "INSERT INTO meals (name) VALUES ('Vegetable Curry')"
            );
        }

        List<Meal> results = repository.getAll();

        assertEquals(1, results.size());
        Meal meal = results.get(0);
        assertEquals(1, meal.getId());
        assertEquals("Vegetable Curry", meal.getName());
    }

    // --- create ---

    @Test
    void create_returnsMealWithGeneratedId() throws SQLException {
        Meal result = repository.create("Pasta Bake");

        assertNotNull(result.getId());
        assertTrue(result.getId() > 0);
    }

    @Test
    void create_returnsMealWithCorrectName() throws SQLException {
        Meal result = repository.create("Pasta Bake");

        assertEquals("Pasta Bake", result.getName());
    }

    @Test
    void create_persistsMeal() throws SQLException {
        Meal created = repository.create("Pasta Bake");

        Optional<Meal> fetched = repository.getById(created.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Pasta Bake", fetched.get().getName());
    }

    @Test
    void create_multipleMeals_assignsDistinctIds() throws SQLException {
        Meal first = repository.create("Soup");
        Meal second = repository.create("Salad");

        assertNotEquals(first.getId(), second.getId());
    }

    // --- update ---

    @Test
    void update_changesName() throws SQLException {
        Meal created = repository.create("Old Name");
        repository.update(new Meal(created.getId(), "New Name"));

        Optional<Meal> fetched = repository.getById(created.getId());
        assertTrue(fetched.isPresent());
        assertEquals("New Name", fetched.get().getName());
    }

    @Test
    void update_doesNotAffectOtherMeals() throws SQLException {
        Meal first = repository.create("Soup");
        Meal second = repository.create("Salad");
        repository.update(new Meal(first.getId(), "Updated Soup"));

        Optional<Meal> fetched = repository.getById(second.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Salad", fetched.get().getName());
    }

    // --- delete ---

    @Test
    void delete_removesMeal() throws SQLException {
        Meal created = repository.create("Soup");
        repository.delete(created.getId());

        assertTrue(repository.getById(created.getId()).isEmpty());
    }

    @Test
    void delete_doesNotAffectOtherMeals() throws SQLException {
        Meal first = repository.create("Soup");
        Meal second = repository.create("Salad");
        repository.delete(first.getId());

        assertTrue(repository.getById(second.getId()).isPresent());
    }
}
