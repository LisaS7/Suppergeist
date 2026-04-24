package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.repository.MealRepository;
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
import java.util.Map;

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
                new MealRepository(dbManager),
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

    private int insertMeal(int planId, String name, int dayOffset) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meals (meal_plan_id, day_offset, meal_type, name) VALUES (?, ?, 'dinner', ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, planId);
            stmt.setInt(2, dayOffset);
            stmt.setString(3, name);
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

    /**
     * Flattens all items across all categories into a single list.
     */
    private List<ShoppingItem> allItems(Map<String, List<ShoppingItem>> result) {
        return result.values().stream().flatMap(List::stream).toList();
    }

    // --- tests ---

    @Test
    void buildList_returnsEmptyMap_whenPlanHasNoEntries() throws SQLException {
        int planId = insertMealPlan();

        Map<String, List<ShoppingItem>> result = service.buildList(planId);

        assertTrue(result.isEmpty());
    }

    @Test
    void buildList_returnsSingleItem_forOneMealWithOneIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        List<ShoppingItem> items = allItems(service.buildList(planId));

        assertEquals(1, items.size());
    }

    @Test
    void buildList_carriesNameAndUnit_fromIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        ShoppingItem item = allItems(service.buildList(planId)).get(0);

        assertEquals("Spaghetti", item.name());
        assertEquals("g", item.unit());
    }

    @Test
    void buildList_setsQuantity_forSingleIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Pasta", 0);
        int ingredientId = insertIngredient("Spaghetti");
        insertMealIngredient(mealId, ingredientId, 200.0, "g");

        ShoppingItem item = allItems(service.buildList(planId)).get(0);

        assertEquals(200.0, item.totalQuantity());
    }

    @Test
    void buildList_aggregatesQuantity_forSameIngredientAcrossMultipleMeals() throws SQLException {
        int planId = insertMealPlan();
        int meal1 = insertMeal(planId, "Pasta", 0);
        int meal2 = insertMeal(planId, "Chicken Soup", 1);
        int ingredientId = insertIngredient("Chicken");
        insertMealIngredient(meal1, ingredientId, 200.0, "g");
        insertMealIngredient(meal2, ingredientId, 150.0, "g");

        List<ShoppingItem> items = allItems(service.buildList(planId));

        assertEquals(1, items.size());
        assertEquals(350.0, items.get(0).totalQuantity());
    }

    @Test
    void buildList_keepsSeparateItems_forDifferentIngredients() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Salad", 0);
        int ing1 = insertIngredient("Lettuce");
        int ing2 = insertIngredient("Tomato");
        insertMealIngredient(mealId, ing1, 100.0, "g");
        insertMealIngredient(mealId, ing2, 50.0, "g");

        List<ShoppingItem> items = allItems(service.buildList(planId));

        assertEquals(2, items.size());
    }

    @Test
    void buildList_aggregatesAcrossThreeMeals_forSharedIngredient() throws SQLException {
        int planId = insertMealPlan();
        int meal1 = insertMeal(planId, "Monday Dinner", 0);
        int meal2 = insertMeal(planId, "Wednesday Dinner", 2);
        int meal3 = insertMeal(planId, "Friday Dinner", 4);
        int ingredientId = insertIngredient("Olive Oil");
        insertMealIngredient(meal1, ingredientId, 10.0, "ml");
        insertMealIngredient(meal2, ingredientId, 15.0, "ml");
        insertMealIngredient(meal3, ingredientId, 20.0, "ml");

        List<ShoppingItem> items = allItems(service.buildList(planId));

        assertEquals(1, items.size());
        assertEquals(45.0, items.get(0).totalQuantity());
    }

    @Test
    void buildList_returnsEmptyMap_forNonExistentPlanId() throws SQLException {
        Map<String, List<ShoppingItem>> result = service.buildList(9999);

        assertTrue(result.isEmpty());
    }

    // --- food code carry-through ---

    @Test
    void buildList_carriesFoodCode_fromIngredient() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Omelette", 0);
        int ingredientId = insertIngredient("Egg", "12-001");
        insertMealIngredient(mealId, ingredientId, 2.0, "whole");

        ShoppingItem item = allItems(service.buildList(planId)).get(0);

        assertEquals("12-001", item.foodCode());
    }

    @Test
    void buildList_carriesNullFoodCode_whenIngredientHasNoFoodCode() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Mystery Dish", 0);
        int ingredientId = insertIngredient("Unknown ingredient", null);
        insertMealIngredient(mealId, ingredientId, 50.0, "g");

        ShoppingItem item = allItems(service.buildList(planId)).get(0);

        assertNull(item.foodCode());
    }

    // --- categorisation ---

    @Test
    void buildList_groupsItemByFoodCode_intoDairyCategory() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Omelette", 0);
        int ingredientId = insertIngredient("Egg", "12-001");
        insertMealIngredient(mealId, ingredientId, 2.0, "whole");

        Map<String, List<ShoppingItem>> result = service.buildList(planId);

        assertTrue(result.containsKey("Dairy & Eggs"));
        assertEquals("Egg", result.get("Dairy & Eggs").get(0).name());
    }

    @Test
    void buildList_groupsItemWithNullFoodCode_intoGeneralCategory() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Mystery Dish", 0);
        int ingredientId = insertIngredient("Unknown ingredient", null);
        insertMealIngredient(mealId, ingredientId, 50.0, "g");

        Map<String, List<ShoppingItem>> result = service.buildList(planId);

        assertTrue(result.containsKey("General"));
    }

    @Test
    void buildList_separatesItemsAcrossCategories_whenFoodCodesdiffer() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Salad", 0);
        int egg = insertIngredient("Egg", "12-001");       // Dairy & Eggs
        int lettuce = insertIngredient("Lettuce", "13-001"); // Vegetables & Beans
        insertMealIngredient(mealId, egg, 2.0, "whole");
        insertMealIngredient(mealId, lettuce, 100.0, "g");

        Map<String, List<ShoppingItem>> result = service.buildList(planId);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("Dairy & Eggs"));
        assertTrue(result.containsKey("Vegetables & Beans"));
    }

    @Test
    void buildList_returnsCategoriesInAlphabeticalOrder() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Mixed Meal", 0);
        int egg = insertIngredient("Egg", "12-001");         // Dairy & Eggs
        int bread = insertIngredient("Bread", "11-001");     // Bakery & Grains
        int carrot = insertIngredient("Carrot", "13-001");   // Vegetables & Beans
        insertMealIngredient(mealId, egg, 2.0, "whole");
        insertMealIngredient(mealId, bread, 1.0, "slice");
        insertMealIngredient(mealId, carrot, 100.0, "g");

        List<String> categories = service.buildList(planId).keySet().stream().toList();

        assertEquals(List.of("Bakery & Grains", "Dairy & Eggs", "Vegetables & Beans"), categories);
    }

    @Test
    void buildList_returnsItemsWithinCategoryInAlphabeticalOrder() throws SQLException {
        int planId = insertMealPlan();
        int mealId = insertMeal(planId, "Veggie Stew", 0);
        int zucchini = insertIngredient("Zucchini", "13-010");
        int aubergine = insertIngredient("Aubergine", "13-020");
        insertMealIngredient(mealId, zucchini, 100.0, "g");
        insertMealIngredient(mealId, aubergine, 150.0, "g");

        List<ShoppingItem> items = service.buildList(planId).get("Vegetables & Beans");

        assertEquals("Aubergine", items.get(0).name());
        assertEquals("Zucchini", items.get(1).name());
    }
}
