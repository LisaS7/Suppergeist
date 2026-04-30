package com.example.suppergeist.repository;

import com.example.suppergeist.model.Ingredient;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RowMappers {
    private static Double nullableDouble(ResultSet rs, String col) throws SQLException {
        double val = rs.getDouble(col);
        return rs.wasNull() ? null : val;
    }

    static Ingredient mapIngredient(ResultSet rs, String idColumn) throws SQLException {
        return new Ingredient(
                rs.getInt(idColumn),
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
}
