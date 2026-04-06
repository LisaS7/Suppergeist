package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.MealPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class MealPlanRepository {
    private final DatabaseManager dbManager;

    public MealPlanRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private MealPlan mapRowToMealPlan(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        LocalDate startDate = LocalDate.parse(rs.getString("start_date"));
        return new MealPlan(id, userId, startDate);
    }

    public Optional<MealPlan> getMealPlanByUserAndStartDate(int userId, LocalDate startDate) throws SQLException {
        String sql = "SELECT * FROM meal_plans WHERE user_id=? AND start_date=?";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, userId);
            stmt.setString(2, startDate.toString());

            try (
                    ResultSet rs = stmt.executeQuery()
            ) {
                if (rs.next()) {
                    return Optional.of(mapRowToMealPlan(rs));
                }
                return Optional.empty();
            }
        }
    }

}
