package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.repository.MealPlanEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingListServiceTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private ShoppingListService service;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-shopping-list-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
        dbManager.init();

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (1, 'Test User')");
        }

        service = new ShoppingListService(
                new MealPlanEntryRepository(dbManager),
                new MealIngredientRepository(dbManager)
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    // --- helpers ---

    private int insertMealPlan() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_plans (user_id, start_date) VALUES (1, '2026-04-07')",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    private int insertMeal(String name) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meals (name) VALUES (?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    private int insertIngredient(String name) throws SQLException {
        return insertIngredient(name, null);
    }

    private int insertIngredient(String name, String foodCode) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO ingredients (name, food_code) VALUES (?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, foodCode);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    private void insertMealPlanEntry(int mealPlanId, int mealId, int dayOffset) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_plan_entries (meal_plan_id, meal_id, day_offset, meal_type) VALUES (?, ?, ?, 'dinner')")) {
            stmt.setInt(1, mealPlanId);
            stmt.setInt(2, mealId);
            stmt.setInt(3, dayOffset);
            stmt.executeUpdate();
        }
    }

    private void insertMealIngredient(int mealId, int ingredientId, double quantity, String unit) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_ingredients (meal_id, ingredient_id, quantity, unit) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, mealId);
            stmt.setInt(2, ingredientId);
            stmt.setDouble(3, quantity);
            stmt.setString(4, unit);
            stmt.executeUpdate();
        }
    }

    // --- tests ---

    @Test
    void buildList_returnsEmptyList_whenPlanHasNoEntries() throws SQLException {
        int planId = insertMealPlan();

        List<ShoppingItem> result = service.buildList(planId);

        assertTrue(result.isEmpty());
    }

    @Test
    void buildList_returnsSingleItem_forOneMealWithOneIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealPlanEntry(planId, mealId, 0);
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        List<ShoppingItem> result = service.buildList(planId);

        assertEquals(1, result.size());
    }

    @Test
    void buildList_carriesNameAndUnit_fromIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealPlanEntry(planId, mealId, 0);
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        ShoppingItem item = service.buildList(planId).get(0);

        assertEquals("Spaghetti", item.name());
        assertEquals("g", item.unit());
    }

    @Test
    void buildList_setsQuantity_forSingleIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal("Pasta");
        int ingredientId = insertIngredient("Spaghetti");
        insertMealPlanEntry(planId, mealId, 0);
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        ShoppingItem item = service.buildList(planId).get(0);

        assertEquals(200.0, item.totalQuantity());
    }

    @Test
    void buildList_aggregatesQuantity_forSameIngredientAcrossMultipleMeals() throws SQLException {
        int planId = insertMealPlan();
        int meal1 = insertMeal("Pasta");
        int meal2 = insertMeal("Chicken Soup");
        int ingredientId = insertIngredient("Chicken");
        insertMealPlanEntry(planId, meal1, 0);
        insertMealPlanEntry(planId, meal2, 1);
        insertMealIngredient(meal1, ingredientId, 200.0, "g");
        insertMealIngredient(meal2, ingredientId, 150.0, "g");

        List<ShoppingItem> result = service.buildList(planId);

        assertEquals(1, result.size());
        assertEquals(350.0, result.get(0).totalQuantity());
    }

    @Test
    void buildList_keepsSeparateItems_forDifferentIngredients() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal("Salad");
        int ing1 = insertIngredient("Lettuce");
        int ing2 = insertIngredient("Tomato");
        insertMealPlanEntry(planId, mealId, 0);
        insertMealIngredient(mealId, ing1, 100.0, "g");
        insertMealIngredient(mealId, ing2, 50.0, "g");

        List<ShoppingItem> result = service.buildList(planId);

        assertEquals(2, result.size());
    }

    @Test
    void buildList_aggregatesAcrossThreeMeals_forSharedIngredient() throws SQLException {
        int planId = insertMealPlan();
        int meal1 = insertMeal("Monday Dinner");
        int meal2 = insertMeal("Wednesday Dinner");
        int meal3 = insertMeal("Friday Dinner");
        int ingredientId = insertIngredient("Olive Oil");
        insertMealPlanEntry(planId, meal1, 0);
        insertMealPlanEntry(planId, meal2, 2);
        insertMealPlanEntry(planId, meal3, 4);
        insertMealIngredient(meal1, ingredientId, 10.0, "ml");
        insertMealIngredient(meal2, ingredientId, 15.0, "ml");
        insertMealIngredient(meal3, ingredientId, 20.0, "ml");

        List<ShoppingItem> result = service.buildList(planId);

        assertEquals(1, result.size());
        assertEquals(45.0, result.get(0).totalQuantity());
    }

    @Test
    void buildList_returnsEmptyList_forNonExistentPlanId() throws SQLException {
        List<ShoppingItem> result = service.buildList(9999);

        assertTrue(result.isEmpty());
    }

    // --- food code carry-through ---

    @Test
    void buildList_carriesFoodCode_fromIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal("Omelette");
        int ingredientId = insertIngredient("Egg", "12-001");
        insertMealPlanEntry(planId, mealId, 0);
        insertMealIngredient(mealId, ingredientId, 2.0, "whole");

        ShoppingItem item = service.buildList(planId).get(0);

        assertEquals("12-001", item.foodCode());
    }

    @Test
    void buildList_carriesNullFoodCode_whenIngredientHasNoFoodCode() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal("Mystery Dish");
        int ingredientId = insertIngredient("Unknown ingredient", null);
        insertMealPlanEntry(planId, mealId, 0);
        insertMealIngredient(mealId, ingredientId, 50.0, "g");

        ShoppingItem item = service.buildList(planId).get(0);

        assertNull(item.foodCode());
    }
}
