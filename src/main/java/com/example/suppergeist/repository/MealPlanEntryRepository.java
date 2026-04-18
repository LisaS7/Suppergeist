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

}
