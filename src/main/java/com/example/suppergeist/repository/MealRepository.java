package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Meal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealRepository {

    private final DatabaseManager dbManager;

    public MealRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Meal> getMeals(int mealPlanId) throws SQLException {
        String sql = "SELECT id, meal_plan_id, day_offset, meal_type, name " +
                "FROM meals WHERE meal_plan_id = ? ORDER BY day_offset, meal_type";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, mealPlanId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Meal> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Meal(rs.getInt("id"), rs.getInt("meal_plan_id"), rs.getInt("day_offset"), rs.getString("meal_type"), rs.getString("name")));
                }
                return rows;
            }
        }
    }

    public Meal create(Meal meal) throws SQLException {
        String sql = "INSERT INTO meals (meal_plan_id, day_offset, meal_type, name) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, meal.getMealPlanId());
            stmt.setInt(2, meal.getDayOffset());
            stmt.setString(3, meal.getMealType());
            stmt.setString(4, meal.getMealName());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return new Meal(rs.getInt(1), meal.getMealPlanId(), meal.getDayOffset(), meal.getMealType(), meal.getMealName());
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM meals WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

}
