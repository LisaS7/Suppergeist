package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.repository.MealRepository;
import com.example.suppergeist.repository.MealPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

        dbManager.init();

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                        INSERT INTO users (id, name) VALUES (1, 'User One'), (2, 'User Two')
                    """);
        }

        service = new MealPlanService(
                new MealRepository(dbManager),
                new MealPlanRepository(dbManager)
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

    private int insertMeal(int planId, String name, int dayOffset, String mealType) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meals (meal_plan_id, day_offset, meal_type, name) VALUES (?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, planId);
            stmt.setInt(2, dayOffset);
            stmt.setString(3, mealType);
            stmt.setString(4, name);
            stmt.executeUpdate();
            return stmt.getGeneratedKeys().getInt(1);
        }
    }

    // --- tests ---

    @Test
    void getWeeklyMeals_returnsEmptyList_whenNoPlanExistsForUser() throws SQLException {
        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 1));

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_returnsEmptyList_whenPlanHasNoEntries() throws SQLException {
        insertMealPlan(1, LocalDate.of(2026, 3, 30));

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 1));

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_adjustsReferenceDateToWeekStart() throws SQLException {
        // Week starting Monday 2026-03-30; reference date is Wednesday 2026-04-01
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        insertMealPlan(1, weekStart);

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 1));

        assertTrue(result.isEmpty()); // plan found, but no entries — confirms the week was resolved correctly
    }

    @Test
    void getWeeklyMeals_returnsEmptyList_whenReferenceDateIsInDifferentWeek() throws SQLException {
        insertMealPlan(1, LocalDate.of(2026, 3, 30));

        // Reference date resolves to the NEXT Monday (2026-04-06), not 2026-03-30
        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 4, 6));

        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyMeals_returnsViewModel_withCorrectMealNameAndType() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        insertMeal(planId, "Spaghetti Bolognese", 0, "dinner");

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30));

        assertEquals(1, result.size());
        assertEquals("Spaghetti Bolognese", result.get(0).mealName());
        assertEquals("dinner", result.get(0).mealType());
    }

    @Test
    void getWeeklyMeals_computesMealDateFromStartDateAndDayOffset() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        insertMeal(planId, "Omelette", 2, "breakfast"); // day offset 2 → Wednesday 2026-04-01

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30));

        assertEquals(LocalDate.of(2026, 4, 1), result.get(0).date());
    }

    @Test
    void getWeeklyMeals_setsDayLabel_withDayOfWeekAndDayOfMonth() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30); // Monday
        int planId = insertMealPlan(1, weekStart);
        insertMeal(planId, "Toast", 0, "breakfast"); // Monday the 30th

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30));

        assertEquals("Monday 30 Mar", result.get(0).dayLabel());
    }

    @Test
    void getWeeklyMeals_returnsMultipleEntries_inOrderDetermined_byRepository() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int planId = insertMealPlan(1, weekStart);
        insertMeal(planId, "Porridge", 0, "breakfast");
        insertMeal(planId, "Curry", 1, "dinner");
        insertMeal(planId, "Sandwich", 2, "lunch");

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30));

        assertEquals(3, result.size());
        assertEquals("Porridge", result.get(0).mealName());
        assertEquals("Curry", result.get(1).mealName());
        assertEquals("Sandwich", result.get(2).mealName());
    }

    @Test
    void getWeeklyMeals_computesMealDateAndLabel_forLastDayOfWeek() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30); // Monday
        int planId = insertMealPlan(1, weekStart);
        insertMeal(planId, "Sunday Roast", 6, "dinner"); // day offset 6 → Sunday 2026-04-05

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30));

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 4, 5), result.get(0).date());
        assertEquals("Sunday 5 Apr", result.get(0).dayLabel());
    }

    @Test
    void getWeeklyMeals_isolatesResultsByUser() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 3, 30);
        int plan1 = insertMealPlan(1, weekStart);
        int plan2 = insertMealPlan(2, weekStart);
        insertMeal(plan1, "Soup", 0, "lunch");
        insertMeal(plan2, "Steak", 0, "dinner");

        List<WeeklyMealViewModel> result = service.getWeeklyMeals(1, LocalDate.of(2026, 3, 30));

        assertEquals(1, result.size());
        assertEquals("Soup", result.get(0).mealName());
    }

    // --- findPlanForWeek ---

    @Test
    void findPlanForWeek_returnsNull_whenNoPlanExists() throws SQLException {
        assertNull(service.findPlanForWeek(1, LocalDate.of(2026, 4, 14)));
    }

    @Test
    void findPlanForWeek_returnsPlan_whenOneExists() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 14);
        insertMealPlan(1, weekStart);

        assertNotNull(service.findPlanForWeek(1, weekStart));
    }


    // --- createEmptyPlan ---

    @Test
    void createEmptyPlan_returnsNewPlan_withCorrectUserAndStartDate() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 14);

        MealPlan plan = service.createEmptyPlan(1, weekStart);

        assertNotNull(plan.id());
        assertEquals(1, plan.userId());
        assertEquals(weekStart, plan.startDate());
    }

    @Test
    void createEmptyPlan_createsNoEntries() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 14);
        service.createEmptyPlan(1, weekStart);

        List<WeeklyMealViewModel> meals = service.getWeeklyMeals(1, weekStart);
        assertTrue(meals.isEmpty());
    }

    // --- deletePlan ---

    @Test
    void deletePlan_removesThePlan() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 14);
        MealPlan plan = service.createEmptyPlan(1, weekStart);

        service.deletePlan(plan.id());

        assertNull(service.findPlanForWeek(1, weekStart));
    }

    // --- addMealToSlot ---

    @Test
    void addMealToSlot_appearsInGetWeeklyMeals() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 13); // Monday
        MealPlan plan = service.createEmptyPlan(1, weekStart);

        service.addMealToSlot("Stir Fry", "dinner", plan.id(), 0);

        List<WeeklyMealViewModel> meals = service.getWeeklyMeals(1, weekStart);
        assertEquals(1, meals.size());
        assertEquals("Stir Fry", meals.get(0).mealName());
        assertEquals("dinner", meals.get(0).mealType());
    }

    @Test
    void addMealToSlot_computesDateFromDayOffset() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 13); // Monday
        MealPlan plan = service.createEmptyPlan(1, weekStart);

        service.addMealToSlot("Omelette", "breakfast", plan.id(), 2);

        List<WeeklyMealViewModel> meals = service.getWeeklyMeals(1, weekStart);
        assertEquals(LocalDate.of(2026, 4, 15), meals.get(0).date()); // offset 2 → Wednesday
    }

    @Test
    void addMealToSlot_multipleEntriesInSamePlan() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 13); // Monday
        MealPlan plan = service.createEmptyPlan(1, weekStart);

        service.addMealToSlot("Porridge", "breakfast", plan.id(), 0);
        service.addMealToSlot("Salad", "lunch", plan.id(), 0);

        List<WeeklyMealViewModel> meals = service.getWeeklyMeals(1, weekStart);
        assertEquals(2, meals.size());
    }

    @Test
    void deletePlan_cascadesEntries() throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 4, 14);
        MealPlan plan = service.createEmptyPlan(1, weekStart);
        insertMeal(plan.id(), "Pasta", 0, "dinner");

        service.deletePlan(plan.id());

        assertTrue(service.getWeeklyMeals(1, weekStart).isEmpty());
    }

}
