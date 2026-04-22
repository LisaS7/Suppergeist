package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealPlanEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealPlanEntryRepository {

    private final DatabaseManager dbManager;

    public MealPlanEntryRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<MealPlanEntry> getMealPlanEntries(int mealPlanId) throws SQLException {
        String sql = "SELECT mpe.meal_plan_id, mpe.meal_id, mpe.day_offset, mpe.meal_type, m.name " +
                "FROM meal_plan_entries mpe JOIN meals m ON m.id = mpe.meal_id " +
                "WHERE mpe.meal_plan_id = ? ORDER BY mpe.day_offset, mpe.meal_type";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, mealPlanId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<MealPlanEntry> rows = new ArrayList<>();
                while (rs.next()) {
                    MealPlanEntry row = new MealPlanEntry(mealPlanId, rs.getInt("meal_id"), rs.getInt("day_offset"), rs.getString("meal_type"), rs.getString("name"));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public MealPlanEntry create(MealPlanEntry mealPlanEntry) throws SQLException {
        String sql = "INSERT INTO meal_plan_entries (meal_plan_id, meal_id, day_offset, meal_type) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, mealPlanEntry.getMealPlanId());
            stmt.setInt(2, mealPlanEntry.getMealId());
            stmt.setInt(3, mealPlanEntry.getDayOffset());
            stmt.setString(4, mealPlanEntry.getMealType());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return new MealPlanEntry(rs.getInt(1), mealPlanEntry.getMealPlanId(), mealPlanEntry.getMealId(), mealPlanEntry.getDayOffset(), mealPlanEntry.getMealType(), mealPlanEntry.getMealName());
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM meal_plan_entries WHERE id = ?";
        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

}
