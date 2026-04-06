package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealPlanEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MealPlanEntryRepository {
    private final DatabaseManager dbManager;

    public MealPlanEntryRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private MealPlanEntry mapRowToMealPlanEntry(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int mealPlanId = rs.getInt("meal_plan_id");
        int mealId = rs.getInt("meal_id");
        int dayOffset = rs.getInt("day_offset");
        String mealType = rs.getString("meal_type");
        return new MealPlanEntry(id, mealPlanId, mealId, dayOffset, mealType);
    }

    public List<MealPlanEntry> getEntriesByMealPlanId(int mealPlanId) throws SQLException {
        String sql = "SELECT * FROM meal_plan_entries WHERE meal_plan_id = ? ORDER BY day_offset, meal_type";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, mealPlanId);

            try (
                    ResultSet rs = stmt.executeQuery()
            ) {
                List<MealPlanEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    MealPlanEntry entry = mapRowToMealPlanEntry(rs);
                    entries.add(entry);
                }
                return entries;
            }
        }

    }

}
