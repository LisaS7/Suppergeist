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

    public Optional<Meal> getById(int id) throws SQLException {
        String sql = "SELECT * FROM meals WHERE id = ?";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, id);
            try (
                    ResultSet rs = stmt.executeQuery()
            ) {
                if (rs.next()) {
                    return Optional.of(mapRowToMeal(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Meal> getAll() throws SQLException {
        String sql = "SELECT * FROM meals";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            List<Meal> meals = new ArrayList<>();
            while (rs.next()) {
                Meal meal = mapRowToMeal(rs);
                meals.add(meal);
            }
            return meals;
        }
    }

    public Meal create(String name) throws SQLException {
        String sql = "INSERT INTO meals (name) VALUES (?)";
        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                int id = rs.getInt(1);
                return new Meal(id, name);
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM meals WHERE id = ?";
        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void update(Meal meal) throws SQLException {
        String sql = "UPDATE meals SET name = ? WHERE id = ?";
        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, meal.getName());
            stmt.setInt(2, meal.getId());
            stmt.executeUpdate();
        }
    }
}
