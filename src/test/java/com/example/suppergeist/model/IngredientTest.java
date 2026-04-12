package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredientTest {

    @Test
    void fullConstructor_setsAllFields() {
        Ingredient ingredient = new Ingredient(1, "Almonds", "11-002");
        assertEquals(1, ingredient.getId());
        assertEquals("Almonds", ingredient.getName());
        assertEquals("11-002", ingredient.getFoodCode());
    }

    @Test
    void fullConstructor_acceptsNullId() {
        Ingredient ingredient = new Ingredient(null, "Almonds", "11-002");
        assertNull(ingredient.getId());
    }

    @Test
    void fullConstructor_acceptsNullFoodCode() {
        Ingredient ingredient = new Ingredient(1, "Mystery Herb", null);
        assertNull(ingredient.getFoodCode());
    }

    @Test
    void shortConstructor_setsNullId() {
        Ingredient ingredient = new Ingredient("Almonds", "11-002");
        assertNull(ingredient.getId());
    }

    @Test
    void shortConstructor_setsNameAndFoodCode() {
        Ingredient ingredient = new Ingredient("Almonds", "11-002");
        assertEquals("Almonds", ingredient.getName());
        assertEquals("11-002", ingredient.getFoodCode());
    }

    @Test
    void shortConstructor_acceptsNullFoodCode() {
        Ingredient ingredient = new Ingredient("Mystery Herb", null);
        assertNull(ingredient.getFoodCode());
    }
}
