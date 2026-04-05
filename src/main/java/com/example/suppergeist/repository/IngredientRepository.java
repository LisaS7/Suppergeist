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

    private Ingredient mapRowToIngredient(ResultSet rs) throws SQLException {
        int ingredientId = rs.getInt("id");
        String name = rs.getString("name");
        String foodCode = rs.getString("food_code");
        return new Ingredient(ingredientId, name, foodCode);
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

    public List<Ingredient> getAllIngredients() throws SQLException {
        String sql = "SELECT * FROM ingredients";

        try (
                Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            List<Ingredient> results = new ArrayList<>();
            while (rs.next()) {
                Ingredient ingredient = mapRowToIngredient(rs);
                results.add(ingredient);
            }
            return results;
        }
    }
}
