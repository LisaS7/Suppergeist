package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Meal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MealRepository {
    private final DatabaseManager dbManager;

    public MealRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private Meal mapRowToMeal(ResultSet rs) throws SQLException {
        int mealId = rs.getInt("id");
        String mealName = rs.getString("name");
        return new Meal(mealId, mealName);
    }

    public Optional<Meal> getMealById(int id) throws SQLException {
        String sql = "SELECT * FROM meals WHERE id = ?";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, id);
            try (
                    ResultSet rs = stmt.executeQuery();
            ) {
                if (rs.next()) {
                    return Optional.of(mapRowToMeal(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Meal> getAllMeals() throws SQLException {
        String sql = "SELECT * FROM meals";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
        ) {

            List<Meal> meals = new ArrayList<>();
            while (rs.next()) {
                Meal meal = mapRowToMeal(rs);
                meals.add(meal);
            }
            return meals;
        }
    }
}
