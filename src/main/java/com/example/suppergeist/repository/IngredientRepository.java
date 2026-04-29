package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredientRepository {
    private final DatabaseManager dbManager;

    public IngredientRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private static Double nullableDouble(ResultSet rs, String col) throws SQLException {
        double val = rs.getDouble(col);
        return rs.wasNull() ? null : val;
    }

    private Ingredient mapRowToIngredient(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("id"),
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
    }

    public Optional<Ingredient> getIngredientById(int id) throws SQLException {
        String sql = "SELECT * FROM ingredients WHERE id = ?";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToIngredient(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Ingredient> searchByName(String name) throws SQLException {
        String sql = "SELECT id, name, food_code FROM ingredients WHERE name LIKE ? LIMIT 20";
        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, "%" + name + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                List<Ingredient> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new Ingredient(rs.getInt("id"), rs.getString("name"), rs.getString("food_code")));
                }
                return results;
            }
        }
    }

    public List<Ingredient> getAllIngredients() throws SQLException {
        String sql = "SELECT id, name, food_code FROM ingredients";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            List<Ingredient> results = new ArrayList<>();
            while (rs.next()) {
                results.add(new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("food_code")
                ));
            }
            return results;
        }
    }
}
