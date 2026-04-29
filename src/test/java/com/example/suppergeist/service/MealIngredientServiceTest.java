package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.MealIngredientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MealIngredientServiceTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private MealIngredientService service;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-meal-ingredient-service-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
        dbManager.init();

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (1, 'Test User')");
            conn.createStatement().execute("INSERT INTO meal_plans (id, user_id, start_date) VALUES (1, 1, '2026-01-01')");
        }

        service = new MealIngredientService(
                new MealIngredientRepository(dbManager),
                new IngredientRepository(dbManager)
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    // --- helpers ---

    private int insertMeal(String name, int dayOffset) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meals (meal_plan_id, day_offset, meal_type, name) VALUES (1, ?, 'dinner', ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, dayOffset);
            stmt.setString(2, name);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    private int insertIngredient(String name) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO ingredients (name) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    // --- getIngredientsForMeal ---

    @Test
    void getIngredientsForMeal_returnsEmptyList_whenNoIngredients() throws SQLException {
        int mealId = insertMeal("Pasta", 0);

        List<MealIngredientRow> result = service.getIngredientsForMeal(mealId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIngredientsForMeal_returnsAddedIngredients() throws SQLException {
        int mealId = insertMeal("Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");
        service.addIngredientToMeal(mealId, ingredientId, 200.0, "g");

        List<MealIngredientRow> result = service.getIngredientsForMeal(mealId);

        assertEquals(1, result.size());
        assertEquals("Spaghetti", result.get(0).ingredient().getName());
        assertEquals(200.0, result.get(0).quantity());
        assertEquals("g", result.get(0).unit());
    }

    // --- addIngredientToMeal ---

    @Test
    void addIngredientToMeal_returnsGeneratedId() throws SQLException {
        int mealId = insertMeal("Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");

        int id = service.addIngredientToMeal(mealId, ingredientId, 200.0, "g");

        assertTrue(id > 0);
    }

    @Test
    void addIngredientToMeal_persistsRow() throws SQLException {
        int mealId = insertMeal("Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");

        service.addIngredientToMeal(mealId, ingredientId, 200.0, "g");

        assertEquals(1, service.getIngredientsForMeal(mealId).size());
    }

    // --- removeIngredientFromMeal ---

    @Test
    void removeIngredientFromMeal_removesRow() throws SQLException {
        int mealId = insertMeal("Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");
        int mealIngredientId = service.addIngredientToMeal(mealId, ingredientId, 200.0, "g");

        service.removeIngredientFromMeal(mealIngredientId);

        assertTrue(service.getIngredientsForMeal(mealId).isEmpty());
    }

    @Test
    void removeIngredientFromMeal_doesNotAffectOtherIngredients() throws SQLException {
        int mealId = insertMeal("Pasta", 0);
        int ing1 = insertIngredient("Spaghetti");
        int ing2 = insertIngredient("Tomato");
        int firstId = service.addIngredientToMeal(mealId, ing1, 200.0, "g");
        service.addIngredientToMeal(mealId, ing2, 50.0, "g");

        service.removeIngredientFromMeal(firstId);

        List<MealIngredientRow> remaining = service.getIngredientsForMeal(mealId);
        assertEquals(1, remaining.size());
        assertEquals("Tomato", remaining.get(0).ingredient().getName());
    }

    // --- searchIngredients ---

    @Test
    void searchIngredients_returnsEmptyList_whenNoMatches() throws SQLException {
        insertIngredient("Almonds");

        List<Ingredient> result = service.searchIngredients("Banana");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchIngredients_returnsMatchingIngredients() throws SQLException {
        insertIngredient("Chicken Breast");
        insertIngredient("Chicken Thigh");
        insertIngredient("Beef Mince");

        List<Ingredient> result = service.searchIngredients("Chicken");

        assertEquals(2, result.size());
    }

    @Test
    void searchIngredients_matchesPartialName() throws SQLException {
        insertIngredient("Chicken Breast");

        List<Ingredient> result = service.searchIngredients("Breast");

        assertEquals(1, result.size());
        assertEquals("Chicken Breast", result.get(0).getName());
    }
}
