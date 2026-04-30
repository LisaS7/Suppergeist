package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPreferencesServiceTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private UserPreferencesService service;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-user-preferences-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
        dbManager.init();

        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().execute("""
                    INSERT INTO users (
                        id, name, dietary_constraints, avoid_food_codes,
                        servings_per_meal, show_calories, show_nutritional_info
                    ) VALUES (
                        1, 'Test User', 'vegetarian, gluten-free', '12-001,13-002',
                        3, 1, 0
                    )
                    """);
        }

        service = new UserPreferencesService(
                new UserRepository(dbManager),
                new IngredientRepository(dbManager)
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    private void insertIngredient(String name, String foodCode) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO ingredients (name, food_code) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, foodCode);
            stmt.executeUpdate();
        }
    }

    @Test
    void loadUser_returnsStoredPreferences() throws SQLException {
        User user = service.loadUser(1);

        assertEquals(1, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals(Set.of("vegetarian", "gluten-free"), user.getDietaryConstraints());
        assertEquals(Set.of("12-001", "13-002"), user.getAvoidFoodCodes());
        assertEquals(3, user.getServingsPerMeal());
        assertTrue(user.isShowCalories());
        assertFalse(user.isShowNutritionalInfo());
    }

    @Test
    void savePreferences_persistsUpdatedPreferences() throws SQLException {
        User updated = new User(1, "Test User", Set.of("vegan"), Set.of("14-001"), 4, false, true);

        service.savePreferences(updated);
        User reloaded = service.loadUser(1);

        assertEquals(Set.of("vegan"), reloaded.getDietaryConstraints());
        assertEquals(Set.of("14-001"), reloaded.getAvoidFoodCodes());
        assertEquals(4, reloaded.getServingsPerMeal());
        assertFalse(reloaded.isShowCalories());
        assertTrue(reloaded.isShowNutritionalInfo());
    }

    @Test
    void getAllIngredients_returnsStoredIngredients() throws SQLException {
        insertIngredient("Apple", "14-001");
        insertIngredient("Bread", "11-001");

        List<Ingredient> ingredients = service.getAllIngredients();

        assertEquals(2, ingredients.size());
        assertIterableEquals(List.of("Apple", "Bread"), ingredients.stream().map(Ingredient::getName).sorted().toList());
    }
}
