package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.MealPlanEntryRepository;
import com.example.suppergeist.repository.MealPlanRepository;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MealPlanServiceTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private MealPlanService service;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-service-test-", ".db");
        dbManager = new DatabaseManager(tempDb);

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS meal_plans (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id    INTEGER NOT NULL,
                    start_date DATE    NOT NULL
                )
            """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS meal_plan_entries (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    meal_plan_id INTEGER NOT NULL,
                    meal_id      INTEGER NOT NULL,
                    day_offset   INTEGER NOT NULL,
                    meal_type    TEXT    NOT NULL
                )
            """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS meals (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL
                )
            """);
        }

        service = new MealPlanService(
                new MealRepository(dbManager),
                new MealPlanRepository(dbManager),
                new MealPlanEntryRepository(dbManager)
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    // --- helpers ---

    private int insertMealPlan(int userId, LocalDate startDate) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_plans (user_id, start_date) VALUES (?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, startDate.toString());
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

    private void insertEntry(int mealPlanId, int mealId, int dayOffset, String mealType) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_plan_entries (meal_plan_id, meal_id, day_offset, meal_type) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, mealPlanId);
            stmt.setInt(2, mealId);
            stmt.setInt(3, dayOffset);
            stmt.setString(4, mealType);
            stmt.executeUpdate();
        }
    }

    // --- tests ---

    @Test
    void getWeeklyMeals_returnsEmptyList_whenNoPlanExistsForUser() throws SQLException {
        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 1), DayOfWeek.MONDAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_returnsEmptyList_whenPlanHasNoEntries() throws SQLException {
        insertMealPlan(1, LocalDate.of(2026, 3, 30));

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 1), DayOfWeek.MONDAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_adjustsReferenceDateToWeekStart() throws SQLException {
        // Week starting Monday 2026-03-30; reference date is Wednesday 2026-04-01
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        insertMealPlan(1, weekStart);

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 1), DayOfWeek.MONDAY);

        assertTrue(result.isEmpty()); // plan found, but no entries — confirms the week was resolved correctly
    }

    @Test
    void getWeeklyMeals_returnsEmptyList_whenReferenceDateIsInDifferentWeek() throws SQLException {
        insertMealPlan(1, LocalDate.of(2026, 3, 30));

        // Reference date resolves to the NEXT Monday (2026-04-06), not 2026-03-30
        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 6), DayOfWeek.MONDAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_returnsViewModel_withCorrectMealNameAndType() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        int mealId = insertMeal("Spaghetti Bolognese");
        insertEntry(planId, mealId, 0, "dinner");

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30), DayOfWeek.MONDAY);

        assertEquals(1, result.size());
        assertEquals("Spaghetti Bolognese", result.get(0).mealName());
        assertEquals("dinner", result.get(0).mealType());
    }

    @Test
    void getWeeklyMeals_computesMealDateFromStartDateAndDayOffset() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        int mealId = insertMeal("Omelette");
        insertEntry(planId, mealId, 2, "breakfast"); // day offset 2 → Wednesday 2026-04-01

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30), DayOfWeek.MONDAY);

        assertEquals(LocalDate.of(2026, 4, 1), result.get(0).date());
    }

    @Test
    void getWeeklyMeals_setsDayLabel_withDayOfWeekAndDayOfMonth() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30); // Monday
        int planId = insertMealPlan(1, weekStart);
        int mealId = insertMeal("Toast");
        insertEntry(planId, mealId, 0, "breakfast"); // Monday the 30th

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30), DayOfWeek.MONDAY);

        assertEquals("MONDAY 30", result.get(0).dayLabel());
    }

    @Test
    void getWeeklyMeals_skipsEntry_whenMealNotFound() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        insertEntry(planId, 999, 0, "dinner"); // meal ID 999 does not exist

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30), DayOfWeek.MONDAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_returnsMultipleEntries_inOrderDetermined_byRepository() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        int meal1 = insertMeal("Porridge");
        int meal2 = insertMeal("Curry");
        int meal3 = insertMeal("Sandwich");
        insertEntry(planId, meal1, 0, "breakfast");
        insertEntry(planId, meal2, 1, "dinner");
        insertEntry(planId, meal3, 2, "lunch");

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30), DayOfWeek.MONDAY);

        assertEquals(3, result.size());
        assertEquals("Porridge", result.get(0).mealName());
        assertEquals("Curry", result.get(1).mealName());
        assertEquals("Sandwich", result.get(2).mealName());
    }

    @Test
    void getWeeklyMeals_isolatesResultsByUser() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int plan1 = insertMealPlan(1, weekStart);
        int plan2 = insertMealPlan(2, weekStart);
        int meal1 = insertMeal("Soup");
        int meal2 = insertMeal("Steak");
        insertEntry(plan1, meal1, 0, "lunch");
        insertEntry(plan2, meal2, 0, "dinner");

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30), DayOfWeek.MONDAY);

        assertEquals(1, result.size());
        assertEquals("Soup", result.get(0).mealName());
    }
}
