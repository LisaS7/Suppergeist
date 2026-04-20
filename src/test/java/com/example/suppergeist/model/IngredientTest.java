package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredientTest {

    @Test
    void fullConstructor_setsAllFields() {
        Ingredient ingredient = new Ingredient(1, "Almonds", "11-002",
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(1, ingredient.getId());
        assertEquals("Almonds", ingredient.getName());
        assertEquals("11-002", ingredient.getFoodCode());
    }

    @Test
    void fullConstructor_acceptsNullId() {
        Ingredient ingredient = new Ingredient(null, "Almonds", "11-002",
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertNull(ingredient.getId());
    }

    @Test
    void fullConstructor_acceptsNullFoodCode() {
        Ingredient ingredient = new Ingredient(1, "Mystery Herb", null,
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertNull(ingredient.getFoodCode());
    }

    @Test
    void fullConstructor_setsNutritionFields() {
        Ingredient ingredient = new Ingredient(1, "Almonds", "11-002",
                579.0, 21.2, 49.9, 21.6, 4.8, 12.5, 1.0, 0.0, 0.0, 25.6, 0.0, 44.0);
        assertEquals(579.0, ingredient.getEnergyKcal());
        assertEquals(21.2, ingredient.getProteinG());
        assertEquals(49.9, ingredient.getFatG());
        assertEquals(21.6, ingredient.getCarbohydrateG());
    }

    @Test
    void fullConstructor_acceptsNullNutritionFields() {
        Ingredient ingredient = new Ingredient(1, "Unknown Herb", "99-999",
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertNull(ingredient.getEnergyKcal());
        assertNull(ingredient.getProteinG());
        assertNull(ingredient.getFatG());
    }

    @Test
    void shortConstructor_setsNameAndFoodCode() {
        Ingredient ingredient = new Ingredient(1, "Almonds", "11-002");
        assertEquals(1, ingredient.getId());
        assertEquals("Almonds", ingredient.getName());
        assertEquals("11-002", ingredient.getFoodCode());
    }

    @Test
    void shortConstructor_acceptsNullFoodCode() {
        Ingredient ingredient = new Ingredient(1, "Mystery Herb", null);
        assertNull(ingredient.getFoodCode());
    }

    @Test
    void shortConstructor_setsNullNutritionFields() {
        Ingredient ingredient = new Ingredient(1, "Almonds", "11-002");
        assertNull(ingredient.getEnergyKcal());
        assertNull(ingredient.getProteinG());
        assertNull(ingredient.getFatG());
        assertNull(ingredient.getCarbohydrateG());
    }
}
