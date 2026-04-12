package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealPlanEntry;
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

class MealPlanEntryRepositoryTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private MealPlanEntryRepository repository;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);

        dbManager.init();

        try (Connection conn = dbManager.getConnection()) {
            // Seed parent rows required by FK constraints.
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (1, 'Test User')");
            // Meal IDs used by tests: 10, 11, 12, 20, 42
            conn.createStatement().execute("""
                INSERT INTO meals (id, name) VALUES
                    (10, 'Test Meal A'), (11, 'Test Meal B'),
                    (12, 'Test Meal C'), (20, 'Test Meal D'), (42, 'Test Meal E')
            """);
            // Meal plan IDs used by tests: 1, 2
            conn.createStatement().execute("""
                INSERT INTO meal_plans (id, user_id, start_date) VALUES
                    (1, 1, '2026-01-01'), (2, 1, '2026-01-08')
            """);
        }

        repository = new MealPlanEntryRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
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

    @Test
    void getEntriesByMealPlanId_returnsEmptyList_whenTableIsEmpty() throws SQLException {
        List<MealPlanEntry> result = repository.getEntriesByMealPlanId(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntriesByMealPlanId_returnsEmptyList_whenNoEntriesForPlan() throws SQLException {
        insertEntry(2, 10, 0, "dinner");

        List<MealPlanEntry> result = repository.getEntriesByMealPlanId(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntriesByMealPlanId_returnsEntries_whenMatchingPlanExists() throws SQLException {
        insertEntry(1, 10, 0, "dinner");

        List<MealPlanEntry> result = repository.getEntriesByMealPlanId(1);

        assertEquals(1, result.size());
    }

    @Test
    void getEntriesByMealPlanId_mapsFieldsCorrectly() throws SQLException {
        insertEntry(1, 42, 3, "lunch");

        MealPlanEntry entry = repository.getEntriesByMealPlanId(1).get(0);

        assertEquals(1, entry.getMealPlanId());
        assertEquals(42, entry.getMealId());
        assertEquals(3, entry.getDayOffset());
        assertEquals("lunch", entry.getMealType());
        assertNotNull(entry.getId());
    }

    @Test
    void getEntriesByMealPlanId_excludesEntriesFromOtherPlans() throws SQLException {
        insertEntry(1, 10, 0, "dinner");
        insertEntry(2, 20, 1, "lunch");

        List<MealPlanEntry> result = repository.getEntriesByMealPlanId(1);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMealPlanId());
    }

    @Test
    void getEntriesByMealPlanId_returnsAllEntriesForPlan() throws SQLException {
        insertEntry(1, 10, 0, "breakfast");
        insertEntry(1, 11, 1, "dinner");
        insertEntry(1, 12, 2, "lunch");

        List<MealPlanEntry> result = repository.getEntriesByMealPlanId(1);

        assertEquals(3, result.size());
    }

    @Test
    void getEntriesByMealPlanId_returnsEntriesOrderedByDayOffsetThenMealType() throws SQLException {
        insertEntry(1, 10, 2, "lunch");
        insertEntry(1, 11, 0, "dinner");
        insertEntry(1, 12, 0, "breakfast");

        List<MealPlanEntry> result = repository.getEntriesByMealPlanId(1);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getDayOffset());
        assertEquals("breakfast", result.get(0).getMealType());
        assertEquals(0, result.get(1).getDayOffset());
        assertEquals("dinner", result.get(1).getMealType());
        assertEquals(2, result.get(2).getDayOffset());
        assertEquals("lunch", result.get(2).getMealType());
    }
}
