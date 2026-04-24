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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (1, 'Test User')");
            conn.createStatement().execute("""
                        INSERT INTO meal_plans (id, user_id, start_date) VALUES
                            (1, 1, '2026-01-01'), (2, 1, '2026-01-08')
                    """);
        }

        repository = new MealRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    private int insertMeal(int planId, int dayOffset, String mealType, String name) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meals (meal_plan_id, day_offset, meal_type, name) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, planId);
            stmt.setInt(2, dayOffset);
            stmt.setString(3, mealType);
            stmt.setString(4, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // --- getMeals ---

    @Test
    void getMeals_returnsEmptyList_whenTableIsEmpty() throws SQLException {
        List<Meal> result = repository.getMeals(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMeals_returnsEmptyList_whenNoMealsForPlan() throws SQLException {
        insertMeal(2, 0, "dinner", "Pasta");

        List<Meal> result = repository.getMeals(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMeals_returnsMeals_whenMatchingPlanExists() throws SQLException {
        insertMeal(1, 0, "dinner", "Pasta");

        List<Meal> result = repository.getMeals(1);

        assertEquals(1, result.size());
    }

    @Test
    void getMeals_mapsFieldsCorrectly() throws SQLException {
        insertMeal(1, 3, "lunch", "Chicken Salad");

        Meal meal = repository.getMeals(1).get(0);

        assertEquals(1, meal.getMealPlanId());
        assertEquals(3, meal.getDayOffset());
        assertEquals("lunch", meal.getMealType());
        assertEquals("Chicken Salad", meal.getMealName());
        assertNotNull(meal.getId());
    }

    @Test
    void getMeals_isolatesByPlan() throws SQLException {
        insertMeal(1, 0, "dinner", "Soup");
        insertMeal(2, 1, "lunch", "Steak");

        List<Meal> result = repository.getMeals(1);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMealPlanId());
    }

    @Test
    void getMeals_returnsAllMealsForPlan() throws SQLException {
        insertMeal(1, 0, "breakfast", "Porridge");
        insertMeal(1, 1, "dinner", "Curry");
        insertMeal(1, 2, "lunch", "Sandwich");

        List<Meal> result = repository.getMeals(1);

        assertEquals(3, result.size());
    }

    @Test
    void getMeals_returnsInOrderByDayOffsetThenMealType() throws SQLException {
        insertMeal(1, 2, "lunch", "Sandwich");
        insertMeal(1, 0, "dinner", "Pasta");
        insertMeal(1, 0, "breakfast", "Porridge");

        List<Meal> result = repository.getMeals(1);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getDayOffset());
        assertEquals("breakfast", result.get(0).getMealType());
        assertEquals(0, result.get(1).getDayOffset());
        assertEquals("dinner", result.get(1).getMealType());
        assertEquals(2, result.get(2).getDayOffset());
        assertEquals("lunch", result.get(2).getMealType());
    }

    // --- create ---

    @Test
    void create_returnsWithGeneratedId() throws SQLException {
        Meal result = repository.create(new Meal(1, 0, "dinner", "Pasta"));

        assertNotNull(result.getId());
        assertTrue(result.getId() > 0);
    }

    @Test
    void create_persistsMeal() throws SQLException {
        repository.create(new Meal(1, 0, "dinner", "Pasta"));

        assertEquals(1, repository.getMeals(1).size());
    }

    @Test
    void create_mapsAllFieldsCorrectly() throws SQLException {
        Meal result = repository.create(new Meal(1, 3, "lunch", "Chicken Salad"));

        assertEquals(1, result.getMealPlanId());
        assertEquals(3, result.getDayOffset());
        assertEquals("lunch", result.getMealType());
        assertEquals("Chicken Salad", result.getMealName());
    }

    // --- delete ---

    @Test
    void delete_removesMeal() throws SQLException {
        Meal created = repository.create(new Meal(1, 0, "dinner", "Pasta"));
        repository.delete(created.getId());

        assertTrue(repository.getMeals(1).isEmpty());
    }

    @Test
    void delete_doesNotAffectOtherMeals() throws SQLException {
        Meal first = repository.create(new Meal(1, 0, "dinner", "Pasta"));
        repository.create(new Meal(1, 1, "lunch", "Salad"));
        repository.delete(first.getId());

        List<Meal> remaining = repository.getMeals(1);
        assertEquals(1, remaining.size());
        assertEquals("Salad", remaining.get(0).getMealName());
    }
}
