package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.repository.MealIngredientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class NutritionServiceTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private NutritionService nutritionService;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
        dbManager.init();
        nutritionService = new NutritionService(new MealIngredientRepository(dbManager));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    private int insertMeal(Connection conn, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO meals (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int insertIngredientWithNutrition(Connection conn, String name,
                                              double kcal, double protein, double fat, double carbs) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO ingredients (name, energy_kcal, protein_g, fat_g, carbohydrate_g) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setDouble(2, kcal);
            stmt.setDouble(3, protein);
            stmt.setDouble(4, fat);
            stmt.setDouble(5, carbs);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int insertIngredientNoNutrition(Connection conn, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO ingredients (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void linkIngredient(Connection conn, int mealId, int ingredientId, double quantity) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO meal_ingredients (meal_id, ingredient_id, quantity, unit) VALUES (?, ?, ?, 'g')")) {
            stmt.setInt(1, mealId);
            stmt.setInt(2, ingredientId);
            stmt.setDouble(3, quantity);
            stmt.executeUpdate();
        }
    }

    @Test
    void estimateForMeal_returnsEstimateWithCorrectTotals() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            int mealId = insertMeal(conn, "Test Meal");
            // 200g of an ingredient with 100 kcal/100g, 10g protein/100g
            int ingId = insertIngredientWithNutrition(conn, "Test Ingredient", 100.0, 10.0, 5.0, 20.0);
            linkIngredient(conn, mealId, ingId, 200.0);

            NutritionalEstimate result = nutritionService.estimateForMeals(mealId);

            assertNotNull(result);
            assertEquals(200, result.cal());          // (200/100) * 100
            assertEquals(20.0, result.proteinG());    // (200/100) * 10
            assertEquals(10.0, result.fatG());        // (200/100) * 5
            assertEquals(40.0, result.carbsG());      // (200/100) * 20
        }
    }

    @Test
    void estimateForMeals_sumsContributionsAcrossMultipleIngredients() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            int mealId = insertMeal(conn, "Multi-Ingredient Meal");
            int ing1 = insertIngredientWithNutrition(conn, "Ingredient A", 200.0, 0.0, 0.0, 0.0);
            int ing2 = insertIngredientWithNutrition(conn, "Ingredient B", 100.0, 0.0, 0.0, 0.0);
            linkIngredient(conn, mealId, ing1, 100.0); // contributes 200 kcal
            linkIngredient(conn, mealId, ing2, 100.0); // contributes 100 kcal

            NutritionalEstimate result = nutritionService.estimateForMeals(mealId);

            assertNotNull(result);
            assertEquals(300, result.cal());
        }
    }

    @Test
    void estimateForMeals_returnsNullWhenNoIngredientHasKcalData() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            int mealId = insertMeal(conn, "No Nutrition Meal");
            int ingId = insertIngredientNoNutrition(conn, "Unknown Ingredient");
            linkIngredient(conn, mealId, ingId, 100.0);

            assertNull(nutritionService.estimateForMeals(mealId));
        }
    }

    @Test
    void estimateForMeal_returnsNullWhenMealsHasNoIngredients() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            int mealId = insertMeal(conn, "Empty Meal");

            assertNull(nutritionService.estimateForMeals(mealId));
        }
    }
}
