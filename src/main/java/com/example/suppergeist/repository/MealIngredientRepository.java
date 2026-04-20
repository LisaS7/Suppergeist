package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class MealIngredientRepository {
    private final DatabaseManager dbManager;

    public MealIngredientRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private static Double nullableDouble(ResultSet rs, String col) throws SQLException {
        double val = rs.getDouble(col);
        return rs.wasNull() ? null : val;
    }

    public Map<Integer, List<MealIngredientRow>> getIngredientsWithNutritionForMeals(List<Integer> mealIds) throws SQLException {
        String placeholders = String.join(", ", Collections.nCopies(mealIds.size(), "?"));
        String sql = """
                SELECT  mi.meal_id, mi.ingredient_id, i.name, mi.quantity, mi.unit, i.food_code,
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
                    Ingredient ingredient = new Ingredient(
                            rs.getInt("ingredient_id"),
                            rs.getString("name"),
                            rs.getString("food_code"),
                            nullableDouble(rs, "energy_kcal"),
                            nullableDouble(rs, "protein_g"),
                            nullableDouble(rs, "fat_g"),
                            nullableDouble(rs, "carbohydrate_g"),
                            nullableDouble(rs, "total_sugars_g"),
                            nullableDouble(rs, "fibre_g"),
                            nullableDouble(rs, "vitamin_a_µg"),
                            nullableDouble(rs, "vitamin_c_mg"),
                            nullableDouble(rs, "vitamin_d_µg"),
                            nullableDouble(rs, "vitamin_e_mg"),
                            nullableDouble(rs, "vitamin_b12_µg"),
                            nullableDouble(rs, "folate_µg")
                    );
                    MealIngredientRow row = new MealIngredientRow(ingredient, rs.getDouble("quantity"), rs.getString("unit"));
                    results.computeIfAbsent(mealId, k -> new ArrayList<>()).add(row);
                }
                return results;
            }
        }
    }

    public List<MealIngredientRow> getIngredientsWithNutritionForMeal(int mealId) throws SQLException {
        String sql = """
                SELECT mi.ingredient_id, i.name, mi.quantity, mi.unit, i.food_code,
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
                    Ingredient ingredient = new Ingredient(
                            rs.getInt("ingredient_id"),
                            rs.getString("name"),
                            rs.getString("food_code"),
                            nullableDouble(rs, "energy_kcal"),
                            nullableDouble(rs, "protein_g"),
                            nullableDouble(rs, "fat_g"),
                            nullableDouble(rs, "carbohydrate_g"),
                            nullableDouble(rs, "total_sugars_g"),
                            nullableDouble(rs, "fibre_g"),
                            nullableDouble(rs, "vitamin_a_µg"),
                            nullableDouble(rs, "vitamin_c_mg"),
                            nullableDouble(rs, "vitamin_d_µg"),
                            nullableDouble(rs, "vitamin_e_mg"),
                            nullableDouble(rs, "vitamin_b12_µg"),
                            nullableDouble(rs, "folate_µg")
                    );
                    results.add(new MealIngredientRow(ingredient, rs.getDouble("quantity"), rs.getString("unit")));
                }
                return results;
            }
        }
    }
}
