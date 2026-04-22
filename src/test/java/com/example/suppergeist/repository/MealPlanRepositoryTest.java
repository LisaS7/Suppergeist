package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealPlan;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MealPlanRepositoryTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private MealPlanRepository repository;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);

        dbManager.init();
        insertUser(1);
        insertUser(2);
        insertUser(42);

        repository = new MealPlanRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    private void insertUser(int id) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (id, name) VALUES (?, ?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, "User " + id);
            stmt.executeUpdate();
        }
    }

    private void insertPlan(int userId, LocalDate startDate) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_plans (user_id, start_date) VALUES (?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setString(2, startDate.toString());
            stmt.executeUpdate();
        }
    }

    private void insertMeal(int id, String name) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO meals (id, name) VALUES (?, ?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    private void insertPlanEntry(int planId, int mealId) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO meal_plan_entries (meal_plan_id, meal_id, day_offset, meal_type) VALUES (?, ?, 0, 'dinner')")) {
            stmt.setInt(1, planId);
            stmt.setInt(2, mealId);
            stmt.executeUpdate();
        }
    }

    private int countEntriesForPlan(int planId) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM meal_plan_entries WHERE meal_plan_id = ?")) {
            stmt.setInt(1, planId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void get_whenFound() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getByUserAndStartDate(1, LocalDate.of(2026, 3, 31));

        assertTrue(result.isPresent());
    }

    @Test
    void getByUserAndStartDate_returnsEmpty_whenNoMatchingUser() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getByUserAndStartDate(99, LocalDate.of(2026, 3, 31));

        assertTrue(result.isEmpty());
    }

    @Test
    void getByUserAndStartDate_returnsEmpty_whenNoMatchingDate() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getByUserAndStartDate(1, LocalDate.of(2026, 4, 7));

        assertTrue(result.isEmpty());
    }

    @Test
    void getByUserAndStartDate_returnsEmpty_whenTableIsEmpty() throws SQLException {
        Optional<MealPlan> result = repository.getByUserAndStartDate(1, LocalDate.of(2026, 3, 31));

        assertTrue(result.isEmpty());
    }

    @Test
    void getByUserAndStartDate_mapsFieldsCorrectly() throws SQLException {
        insertPlan(42, LocalDate.of(2026, 4, 7));

        Optional<MealPlan> result = repository.getByUserAndStartDate(42, LocalDate.of(2026, 4, 7));

        assertTrue(result.isPresent());
        MealPlan plan = result.get();
        assertEquals(1, plan.id());
        assertEquals(42, plan.userId());
        assertEquals(LocalDate.of(2026, 4, 7), plan.startDate());
    }

    @Test
    void get_whenMultiplePlansExist() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 24));
        insertPlan(1, LocalDate.of(2026, 3, 31));
        insertPlan(2, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getByUserAndStartDate(1, LocalDate.of(2026, 3, 31));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().userId());
        assertEquals(LocalDate.of(2026, 3, 31), result.get().startDate());
    }

    // --- create ---

    @Test
    void create_returnsPlanWithGeneratedId() throws SQLException {
        MealPlan result = repository.create(new MealPlan(null, 1, LocalDate.of(2026, 4, 14)));

        assertNotNull(result.id());
        assertTrue(result.id() > 0);
    }

    @Test
    void create_persistsPlan() throws SQLException {
        repository.create(new MealPlan(null, 1, LocalDate.of(2026, 4, 14)));

        assertTrue(repository.getByUserAndStartDate(1, LocalDate.of(2026, 4, 14)).isPresent());
    }

    @Test
    void create_mapsFieldsCorrectly() throws SQLException {
        MealPlan result = repository.create(new MealPlan(null, 42, LocalDate.of(2026, 4, 14)));

        assertEquals(42, result.userId());
        assertEquals(LocalDate.of(2026, 4, 14), result.startDate());
    }

    // --- delete ---

    @Test
    void delete_removesPlan() throws SQLException {
        MealPlan created = repository.create(new MealPlan(null, 1, LocalDate.of(2026, 4, 14)));
        repository.delete(created.id());

        assertTrue(repository.getByUserAndStartDate(1, LocalDate.of(2026, 4, 14)).isEmpty());
    }

    @Test
    void delete_cascadesToMealPlanEntries() throws SQLException {
        insertMeal(1, "Soup");
        MealPlan plan = repository.create(new MealPlan(null, 1, LocalDate.of(2026, 4, 14)));
        insertPlanEntry(plan.id(), 1);

        repository.delete(plan.id());

        assertEquals(0, countEntriesForPlan(plan.id()));
    }
}
