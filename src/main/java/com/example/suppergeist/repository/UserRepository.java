package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserRepository {
    private final DatabaseManager dbManager;
    private static final Logger log = Logger.getLogger(UserRepository.class.getName());

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void ensureDefaultUserExists() throws SQLException {

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE id = 1")) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                log.info("Default user already exists, skipping creation.");
                return;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (id, name) VALUES (1, ?)")) {
                insertStmt.setString(1, "Default User");
                insertStmt.executeUpdate();
                log.info("Default user created.");
            }

        }
    }

    public User getUser(int id) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, dietary_constraints, avoid_ingredients, servings_per_meal FROM users WHERE id = ?")) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String dietaryConstraintsRaw = rs.getString("dietary_constraints");
                    String avoidFoodCodesRaw = rs.getString("avoid_ingredients");
                    int servingsPerMeal = rs.getInt("servings_per_meal");

                    Set<String> dietaryConstraints = dietaryConstraintsRaw.isBlank() ? Set.of() :
                            Arrays.stream(dietaryConstraintsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
                    Set<String> avoidIngredientFoodCodes = avoidFoodCodesRaw.isBlank() ? Set.of() :
                            Arrays.stream(avoidFoodCodesRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

                    return new User(id, name, dietaryConstraints, avoidIngredientFoodCodes, servingsPerMeal);
                } else {
                    log.warning("No user found for id: " + id + " Returning default user.");
                    return new User(id, "Default User", Set.of(), Set.of(), 2);
                }
            }
        }
    }

    public void savePreferences(User user) throws SQLException {
        String sql = "UPDATE users SET dietary_constraints = ?, avoid_ingredients = ?, servings_per_meal = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String dietaryConstraints = String.join(",", user.getDietaryConstraints());
            String avoidFoodCodes = String.join(",", user.getAvoidFoodCodes());

            stmt.setString(1, dietaryConstraints);
            stmt.setString(2, avoidFoodCodes);
            stmt.setInt(3, user.getServingsPerMeal());
            stmt.setInt(4, user.getId());

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated == 0) {
                log.warning("No user updated for id: " + user.getId());
            }
        }
    }
}

