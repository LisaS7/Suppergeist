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

    @Test
    void getMealPlanByUserAndStartDate_returnsPlan_whenFound() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getMealPlanByUserAndStartDate(1, LocalDate.of(2026, 3, 31));

        assertTrue(result.isPresent());
    }

    @Test
    void getMealPlanByUserAndStartDate_returnsEmpty_whenNoMatchingUser() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getMealPlanByUserAndStartDate(99, LocalDate.of(2026, 3, 31));

        assertTrue(result.isEmpty());
    }

    @Test
    void getMealPlanByUserAndStartDate_returnsEmpty_whenNoMatchingDate() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getMealPlanByUserAndStartDate(1, LocalDate.of(2026, 4, 7));

        assertTrue(result.isEmpty());
    }

    @Test
    void getMealPlanByUserAndStartDate_returnsEmpty_whenTableIsEmpty() throws SQLException {
        Optional<MealPlan> result = repository.getMealPlanByUserAndStartDate(1, LocalDate.of(2026, 3, 31));

        assertTrue(result.isEmpty());
    }

    @Test
    void getMealPlanByUserAndStartDate_mapsFieldsCorrectly() throws SQLException {
        insertPlan(42, LocalDate.of(2026, 4, 7));

        Optional<MealPlan> result = repository.getMealPlanByUserAndStartDate(42, LocalDate.of(2026, 4, 7));

        assertTrue(result.isPresent());
        MealPlan plan = result.get();
        assertEquals(1, plan.id());
        assertEquals(42, plan.userId());
        assertEquals(LocalDate.of(2026, 4, 7), plan.startDate());
    }

    @Test
    void getMealPlanByUserAndStartDate_returnsOnlyMatchingPlan_whenMultiplePlansExist() throws SQLException {
        insertPlan(1, LocalDate.of(2026, 3, 24));
        insertPlan(1, LocalDate.of(2026, 3, 31));
        insertPlan(2, LocalDate.of(2026, 3, 31));

        Optional<MealPlan> result = repository.getMealPlanByUserAndStartDate(1, LocalDate.of(2026, 3, 31));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().userId());
        assertEquals(LocalDate.of(2026, 3, 31), result.get().startDate());
    }
}
