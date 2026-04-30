package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.repository.MealIngredientRepository;

import java.time.LocalDate;

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
import java.util.List;
import java.util.Map;

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

        try (var conn = dbManager.getConnection()) {
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (1, 'Test User')");
            conn.createStatement().execute("INSERT INTO meal_plans (id, user_id, start_date) VALUES (1, 1, '2026-01-01')");
        }

        nutritionService = new NutritionService(new MealIngredientRepository(dbManager));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    private int insertMeal(String name) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meals (meal_plan_id, day_offset, meal_type, name) VALUES (1, 0, 'dinner', ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int insertIngredientWithNutrition(String name,
                                              double kcal, double protein, double fat, double carbs) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
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

    private int insertIngredientWithDetailedNutrition(String name,
                                                      double kcal,
                                                      Double protein,
                                                      Double fat,
                                                      Double carbs,
                                                      Double sugars,
                                                      Double fibre,
                                                      Double vitaminA,
                                                      Double vitaminC,
                                                      Double vitaminD,
                                                      Double vitaminE,
                                                      Double vitaminB12,
                                                      Double folate) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                             INSERT INTO ingredients (
                                 name, energy_kcal, protein_g, fat_g, carbohydrate_g,
                                 total_sugars_g, fibre_g, vitamin_a_µg, vitamin_c_mg,
                                 vitamin_d_µg, vitamin_e_mg, vitamin_b12_µg, folate_µg
                             ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                             """,
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setDouble(2, kcal);
            stmt.setObject(3, protein);
            stmt.setObject(4, fat);
            stmt.setObject(5, carbs);
            stmt.setObject(6, sugars);
            stmt.setObject(7, fibre);
            stmt.setObject(8, vitaminA);
            stmt.setObject(9, vitaminC);
            stmt.setObject(10, vitaminD);
            stmt.setObject(11, vitaminE);
            stmt.setObject(12, vitaminB12);
            stmt.setObject(13, folate);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int insertIngredientNoNutrition(String name) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO ingredients (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void linkIngredient(int mealId, int ingredientId, double quantity) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_ingredients (meal_id, ingredient_id, quantity, unit) VALUES (?, ?, ?, 'g')")) {
            stmt.setInt(1, mealId);
            stmt.setInt(2, ingredientId);
            stmt.setDouble(3, quantity);
            stmt.executeUpdate();
        }
    }

    @Test
    void estimatesForMeals_returnsEstimateWithCorrectTotals() throws SQLException {
        int mealId = insertMeal("Test Meal");
        // 200g of an ingredient with 100 kcal/100g, 10g protein/100g
        int ingId = insertIngredientWithNutrition("Test Ingredient", 100.0, 10.0, 5.0, 20.0);
        linkIngredient(mealId, ingId, 200.0);

        Map<Integer, NutritionalEstimate> results = nutritionService.estimatesForMeals(List.of(mealId));
        NutritionalEstimate result = results.get(mealId);

        assertNotNull(result);
        assertEquals(200, result.cal());          // (200/100) * 100
        assertEquals(20.0, result.proteinG());    // (200/100) * 10
        assertEquals(10.0, result.fatG());        // (200/100) * 5
        assertEquals(40.0, result.carbsG());      // (200/100) * 20
    }

    @Test
    void estimatesForMeals_sumsContributionsAcrossMultipleIngredients() throws SQLException {
        int mealId = insertMeal("Multi-Ingredient Meal");
        int ing1 = insertIngredientWithNutrition("Ingredient A", 200.0, 0.0, 0.0, 0.0);
        int ing2 = insertIngredientWithNutrition("Ingredient B", 100.0, 0.0, 0.0, 0.0);
        linkIngredient(mealId, ing1, 100.0); // contributes 200 kcal
        linkIngredient(mealId, ing2, 100.0); // contributes 100 kcal

        Map<Integer, NutritionalEstimate> results = nutritionService.estimatesForMeals(List.of(mealId));

        assertNotNull(results.get(mealId));
        assertEquals(300, results.get(mealId).cal());
    }

    @Test
    void estimatesForMeals_omitsMealWhenNoIngredientHasKcalData() throws SQLException {
        int mealId = insertMeal("No Nutrition Meal");
        int ingId = insertIngredientNoNutrition("Unknown Ingredient");
        linkIngredient(mealId, ingId, 100.0);

        Map<Integer, NutritionalEstimate> results = nutritionService.estimatesForMeals(List.of(mealId));

        assertNull(results.get(mealId));
    }

    @Test
    void estimatesForMeals_omitsMealWithNoIngredients() throws SQLException {
        int mealId = insertMeal("Empty Meal");

        Map<Integer, NutritionalEstimate> results = nutritionService.estimatesForMeals(List.of(mealId));

        assertNull(results.get(mealId));
    }

    @Test
    void estimatesForMeals_onlyCountsIngredientsWithKcalData_whenMixed() throws SQLException {
        int mealId = insertMeal("Mixed Meal");
        int ingWithKcal = insertIngredientWithNutrition("Known Ingredient", 200.0, 10.0, 5.0, 30.0);
        int ingNoKcal = insertIngredientNoNutrition("Unknown Ingredient");
        linkIngredient(mealId, ingWithKcal, 100.0); // contributes 200 kcal
        linkIngredient(mealId, ingNoKcal, 100.0);   // skipped — no kcal data

        Map<Integer, NutritionalEstimate> results = nutritionService.estimatesForMeals(List.of(mealId));

        assertNotNull(results.get(mealId));
        assertEquals(200, results.get(mealId).cal());
    }

    @Test
    void estimatesForMeals_scalesMicronutrientsAndTreatsNullNutrientsAsZero() throws SQLException {
        int mealId = insertMeal("Detailed Nutrition Meal");
        int ingId = insertIngredientWithDetailedNutrition(
                "Fortified Ingredient",
                120.0,
                8.0,
                4.0,
                16.0,
                null,
                6.0,
                400.0,
                20.0,
                5.0,
                null,
                2.0,
                300.0
        );
        linkIngredient(mealId, ingId, 150.0);

        NutritionalEstimate result = nutritionService.estimatesForMeals(List.of(mealId)).get(mealId);

        assertNotNull(result);
        assertEquals(180, result.cal());
        assertEquals(12.0, result.proteinG());
        assertEquals(24.0, result.carbsG());
        assertEquals(6.0, result.fatG());
        assertEquals(0.0, result.totalSugarsG());
        assertEquals(9.0, result.fibreG());
        assertEquals(600.0, result.vitaminAMcg());
        assertEquals(30.0, result.vitaminCMg());
        assertEquals(7.5, result.vitaminDMcg());
        assertEquals(0.0, result.vitaminEMg());
        assertEquals(3.0, result.vitaminB12Mcg());
        assertEquals(450.0, result.folateMcg());
    }

    // --- dailyCalorieTotals ---

    @Test
    void dailyCalorieTotals_returnsEmptyMap_forEmptyInput() {
        Map<LocalDate, Integer> result = nutritionService.dailyCalorieTotals(List.of(), Map.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void dailyCalorieTotals_returnsTotalForSingleMeal() {
        LocalDate date = LocalDate.of(2026, 4, 14);
        WeeklyMealViewModel meal = new WeeklyMealViewModel(1, 42, date, "Monday 14 Apr", "dinner", "Pasta");
        NutritionalEstimate estimate = new NutritionalEstimate(500, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        Map<LocalDate, Integer> result = nutritionService.dailyCalorieTotals(List.of(meal), Map.of(42, estimate));

        assertEquals(500, result.get(date));
    }

    @Test
    void dailyCalorieTotals_sumsCaloriesForMealsOnSameDate() {
        LocalDate date = LocalDate.of(2026, 4, 14);
        WeeklyMealViewModel breakfast = new WeeklyMealViewModel(1, 1, date, "Monday 14 Apr", "breakfast", "Porridge");
        WeeklyMealViewModel dinner = new WeeklyMealViewModel(1, 2, date, "Monday 14 Apr", "dinner", "Pasta");
        NutritionalEstimate est1 = new NutritionalEstimate(300, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        NutritionalEstimate est2 = new NutritionalEstimate(500, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        Map<LocalDate, Integer> result = nutritionService.dailyCalorieTotals(
                List.of(breakfast, dinner), Map.of(1, est1, 2, est2));

        assertEquals(800, result.get(date));
    }

    @Test
    void dailyCalorieTotals_excludesMealsWithNoEstimate() {
        LocalDate date = LocalDate.of(2026, 4, 14);
        WeeklyMealViewModel meal = new WeeklyMealViewModel(1, 99, date, "Monday 14 Apr", "dinner", "Mystery");

        Map<LocalDate, Integer> result = nutritionService.dailyCalorieTotals(List.of(meal), Map.of());

        assertTrue(result.isEmpty());
    }
}
