package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealIngredient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MealIngredientRepositoryTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private MealIngredientRepository repository;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-meal-ingredient-test-", ".db");
        dbManager = new DatabaseManager(tempDb);

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS meals (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL
                )
            """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS ingredients (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    name      TEXT NOT NULL UNIQUE,
                    food_code TEXT
                )
            """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS meal_ingredients (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    meal_id       INTEGER NOT NULL,
                    ingredient_id INTEGER NOT NULL,
                    quantity      REAL    NOT NULL,
                    unit          TEXT
                )
            """);
        }

        repository = new MealIngredientRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    // --- helpers ---

    private int insertMeal(String name) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            var stmt = conn.prepareStatement("INSERT INTO meals (name) VALUES (?)",
                    java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, name);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    private int insertIngredient(String name) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            var stmt = conn.prepareStatement("INSERT INTO ingredients (name) VALUES (?)",
                    java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, name);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    private void insertMealIngredient(int mealId, int ingredientId, double quantity, String unit) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            var stmt = conn.prepareStatement(
                    "INSERT INTO meal_ingredients (meal_id, ingredient_id, quantity, unit) VALUES (?, ?, ?, ?)");
            stmt.setInt(1, mealId);
            stmt.setInt(2, ingredientId);
            stmt.setDouble(3, quantity);
            stmt.setString(4, unit);
            stmt.executeUpdate();
        }
    }

    // --- getIngredientsForMeal ---

    @Test
    void getIngredientsForMeal_returnsEmptyList_whenNoIngredients() throws SQLException {
        int mealId = insertMeal("Pasta");

        List<MealIngredient> result = repository.getIngredientsForMeal(mealId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIngredientsForMeal_returnsIngredients_forGivenMeal() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        List<MealIngredient> result = repository.getIngredientsForMeal(mealId);

        assertEquals(1, result.size());
    }

    @Test
    void getIngredientsForMeal_mapsAllFieldsCorrectly() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        MealIngredient result = repository.getIngredientsForMeal(mealId).get(0);

        assertEquals(mealId, result.getMealId());
        assertEquals(ingredientId, result.getIngredientId());
        assertEquals(200.0, result.getQuantity());
        assertEquals("g", result.getUnit());
        assertNotNull(result.getId());
    }

    @Test
    void getIngredientsForMeal_acceptsNullUnit() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Basil");
        insertMealIngredient(mealId, ingredientId, 1.0, null);

        MealIngredient result = repository.getIngredientsForMeal(mealId).get(0);

        assertNull(result.getUnit());
    }

    @Test
    void getIngredientsForMeal_doesNotReturnIngredients_forOtherMeals() throws SQLException {
        int meal1 = insertMeal("Pasta");
        int meal2 = insertMeal("Salad");
        int ingredientId = insertIngredient("Tomato");
        insertMealIngredient(meal2, ingredientId, 100.0, "g");

        List<MealIngredient> result = repository.getIngredientsForMeal(meal1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIngredientsForMeal_returnsMultipleIngredients() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ing1 = insertIngredient("Spaghetti");
        int ing2 = insertIngredient("Tomato Sauce");
        int ing3 = insertIngredient("Parmesan");
        insertMealIngredient(mealId, ing1, 200.0, "g");
        insertMealIngredient(mealId, ing2, 150.0, "ml");
        insertMealIngredient(mealId, ing3, 30.0, "g");

        List<MealIngredient> result = repository.getIngredientsForMeal(mealId);

        assertEquals(3, result.size());
    }

    // --- getIngredientsWithNameForMeal ---

    @Test
    void getIngredientsWithNameForMeal_returnsEmptyList_whenNoIngredients() throws SQLException {
        int mealId = insertMeal("Pasta");

        List<MealIngredientRow> result = repository.getIngredientsWithNameForMeal(mealId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIngredientsWithNameForMeal_returnsNameFromIngredientsTable() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        List<MealIngredientRow> result = repository.getIngredientsWithNameForMeal(mealId);

        assertEquals(1, result.size());
        assertEquals("Spaghetti", result.get(0).ingredientName());
        assertEquals(ingredientId, result.get(0).ingredientId());
        assertEquals(200.0, result.get(0).quantity());
        assertEquals("g", result.get(0).unit());
    }

    @Test
    void getIngredientsWithNameForMeal_returnsResultsOrderedByName() throws SQLException {
        int mealId = insertMeal("Stir Fry");
        int ingZ = insertIngredient("Zucchini");
        int ingA = insertIngredient("Aubergine");
        int ingM = insertIngredient("Mushroom");
        insertMealIngredient(mealId, ingZ, 100.0, "g");
        insertMealIngredient(mealId, ingA, 50.0, "g");
        insertMealIngredient(mealId, ingM, 80.0, "g");

        List<MealIngredientRow> result = repository.getIngredientsWithNameForMeal(mealId);

        assertEquals("Aubergine", result.get(0).ingredientName());
        assertEquals("Mushroom", result.get(1).ingredientName());
        assertEquals("Zucchini", result.get(2).ingredientName());
    }

    @Test
    void getIngredientsWithNameForMeal_doesNotReturnIngredients_forOtherMeals() throws SQLException {
        int meal1 = insertMeal("Soup");
        int meal2 = insertMeal("Curry");
        int ingredientId = insertIngredient("Lentils");
        insertMealIngredient(meal2, ingredientId, 200.0, "g");

        List<MealIngredientRow> result = repository.getIngredientsWithNameForMeal(meal1);

        assertTrue(result.isEmpty());
    }
}
