package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class MealIngredientRepository {
    private final DatabaseManager dbManager;

    public MealIngredientRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<MealIngredient> getIngredientsForMeal(int mealId) throws SQLException {
        String sql = "SELECT * FROM meal_ingredients WHERE meal_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mealId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<MealIngredient> mealIngredients = new ArrayList<>();
                while (rs.next()) {
                    MealIngredient mealIngredient = new MealIngredient(
                            rs.getInt("id"),
                            rs.getInt("meal_id"),
                            rs.getInt("ingredient_id"),
                            rs.getDouble("quantity"),
                            rs.getString("unit")
                    );
                    mealIngredients.add(mealIngredient);
                }
                return mealIngredients;
            }
        }
    }

    public List<MealIngredientRow> getIngredientsWithNameForMeal(int mealId) throws SQLException {
        String sql = """
                SELECT mi.ingredient_id, i.name, mi.quantity, mi.unit, i.food_code
                FROM meal_ingredients mi
                JOIN ingredients i ON mi.ingredient_id = i.id
                WHERE mi.meal_id = ?
                ORDER BY i.name
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mealId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<MealIngredientRow> results = new ArrayList<>();
                while (rs.next()) {
                    Ingredient ingredient = new Ingredient(
                            rs.getInt("ingredient_id"),
                            rs.getString("name"),
                            rs.getString("food_code")
                    );
                    MealIngredientRow row = new MealIngredientRow(
                            ingredient,
                            rs.getDouble("quantity"),
                            rs.getString("unit")
                    );
                    results.add(row);
                }
                return results;
            }
        }
    }
}
