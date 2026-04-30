package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;

import java.sql.*;
import java.util.*;


public class MealIngredientRepository {
    private final DatabaseManager dbManager;

    public MealIngredientRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }


    public Map<Integer, List<MealIngredientRow>> getIngredientsWithNutritionForMeals(List<Integer> mealIds) throws SQLException {
        String placeholders = String.join(", ", Collections.nCopies(mealIds.size(), "?"));
        String sql = """
                SELECT  mi.id, mi.meal_id, mi.ingredient_id, i.name, mi.quantity, mi.unit, i.food_code,
                       i.energy_kcal, i.protein_g, i.fat_g, i.carbohydrate_g,
                       i.total_sugars_g, i.fibre_g, i.vitamin_a_µg, i.vitamin_c_mg,
                       i.vitamin_d_µg, i.vitamin_e_mg, i.vitamin_b12_µg, i.folate_µg
                FROM meal_ingredients mi
                JOIN ingredients i ON mi.ingredient_id = i.id
                WHERE mi.meal_id IN (""" + placeholders + """
                )
                ORDER BY i.name
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < mealIds.size(); i++) {
                stmt.setInt(i + 1, mealIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                Map<Integer, List<MealIngredientRow>> results = new HashMap<>();
                while (rs.next()) {
                    int mealId = rs.getInt("meal_id");
                    Ingredient ingredient = RowMappers.mapIngredient(rs, "ingredient_id");
                    MealIngredientRow row = new MealIngredientRow(rs.getInt("id"), ingredient, rs.getDouble("quantity"), rs.getString("unit"));
                    results.computeIfAbsent(mealId, k -> new ArrayList<>()).add(row);
                }
                return results;
            }
        }
    }

    public List<MealIngredientRow> getIngredientsWithNutritionForMeal(int mealId) throws SQLException {
        String sql = """
                SELECT mi.id, mi.ingredient_id, i.name, mi.quantity, mi.unit, i.food_code,
                       i.energy_kcal, i.protein_g, i.fat_g, i.carbohydrate_g,
                       i.total_sugars_g, i.fibre_g, i.vitamin_a_µg, i.vitamin_c_mg,
                       i.vitamin_d_µg, i.vitamin_e_mg, i.vitamin_b12_µg, i.folate_µg
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
                    Ingredient ingredient = RowMappers.mapIngredient(rs, "ingredient_id");
                    results.add(new MealIngredientRow(rs.getInt("id"), ingredient, rs.getDouble("quantity"), rs.getString("unit")));
                }
                return results;
            }
        }
    }

    public int create(int mealId, int ingredientId, double quantity, String unit) throws SQLException {
        String sql = "INSERT INTO meal_ingredients (meal_id, ingredient_id, quantity, unit) " +
                "VALUES (?, ?, ?, ?)";
        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {
            stmt.setInt(1, mealId);
            stmt.setInt(2, ingredientId);
            stmt.setDouble(3, quantity);
            stmt.setString(4, unit);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }

    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM meal_ingredients WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}
