package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealIngredientRow;
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

        dbManager.init();

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

    private int insertIngredientWithNutrition(String name, double kcal, double proteinG, double fatG, double carbG) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            var stmt = conn.prepareStatement(
                    "INSERT INTO ingredients (name, energy_kcal, protein_g, fat_g, carbohydrate_g) VALUES (?, ?, ?, ?, ?)",
                    java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, name);
            stmt.setDouble(2, kcal);
            stmt.setDouble(3, proteinG);
            stmt.setDouble(4, fatG);
            stmt.setDouble(5, carbG);
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

    // --- getIngredientsWithNameForMeal ---

    @Test
    void getIngredientsWithNameForMeal_returnsEmptyList_whenNoIngredients() throws SQLException {
        int mealId = insertMeal("Pasta");

        List<MealIngredientRow> result = repository.getIngredientsWithNutritionForMeal(mealId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIngredientsWithNameForMeal_returnsNameFromIngredientsTable() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        List<MealIngredientRow> result = repository.getIngredientsWithNutritionForMeal(mealId);

        assertEquals(1, result.size());
        assertEquals("Spaghetti", result.get(0).ingredient().getName());
        assertEquals(ingredientId, result.get(0).ingredient().getId());
        assertEquals(200.0, result.get(0).quantity());
        assertEquals("g", result.get(0).unit());
    }

    @Test
    void getIngredientsWithNameForMeal_returnsResultsOrderedByNutrition() throws SQLException {
        int mealId = insertMeal("Stir Fry");
        int ingZ = insertIngredient("Zucchini");
        int ingA = insertIngredient("Aubergine");
        int ingM = insertIngredient("Mushroom");
        insertMealIngredient(mealId, ingZ, 100.0, "g");
        insertMealIngredient(mealId, ingA, 50.0, "g");
        insertMealIngredient(mealId, ingM, 80.0, "g");

        List<MealIngredientRow> result = repository.getIngredientsWithNutritionForMeal(mealId);

        assertEquals("Aubergine", result.get(0).ingredient().getName());
        assertEquals("Mushroom", result.get(1).ingredient().getName());
        assertEquals("Zucchini", result.get(2).ingredient().getName());
    }

    @Test
    void getIngredientsWithNutritionForMeal_mapsNutritionFields_whenPresent() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredientWithNutrition("Spaghetti", 371.0, 13.0, 1.5, 74.0);
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        MealIngredientRow result = repository.getIngredientsWithNutritionForMeal(mealId).get(0);

        assertEquals(371.0, result.ingredient().getEnergyKcal());
        assertEquals(13.0, result.ingredient().getProteinG());
        assertEquals(1.5, result.ingredient().getFatG());
        assertEquals(74.0, result.ingredient().getCarbohydrateG());
    }

    @Test
    void getIngredientsWithNutritionForMeal_returnsNullNutritionFields_whenAbsent() throws SQLException {
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Mystery Herb");
        insertMealIngredient(mealId, ingredientId, 5.0, "g");

        MealIngredientRow result = repository.getIngredientsWithNutritionForMeal(mealId).get(0);

        assertNull(result.ingredient().getEnergyKcal());
        assertNull(result.ingredient().getProteinG());
    }

    @Test
    void getIngredientsWithNameForMeal_doesNotReturnIngredients_forOtherMeals() throws SQLException {
        int meal1 = insertMeal("Soup");
        int meal2 = insertMeal("Curry");
        int ingredientId = insertIngredient("Lentils");
        insertMealIngredient(meal2, ingredientId, 200.0, "g");

        List<MealIngredientRow> result = repository.getIngredientsWithNutritionForMeal(meal1);

        assertTrue(result.isEmpty());
    }
}
