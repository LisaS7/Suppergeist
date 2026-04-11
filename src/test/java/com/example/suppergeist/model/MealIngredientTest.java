package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MealIngredientTest {

    @Test
    void constructor_throwsWhenQuantityIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new MealIngredient(1, 1, 0.0, "g"));
    }

    @Test
    void constructor_throwsWhenQuantityIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new MealIngredient(1, 1, -50.0, "g"));
    }

    @Test
    void constructor_acceptsPositiveQuantity() {
        MealIngredient mi = new MealIngredient(1, 2, 150.0, "g");
        assertEquals(1, mi.getMealId());
        assertEquals(2, mi.getIngredientId());
        assertEquals(150.0, mi.getQuantity());
        assertEquals("g", mi.getUnit());
    }

    @Test
    void shortConstructor_setsNullId() {
        MealIngredient mi = new MealIngredient(1, 2, 50.0, "cups");
        assertNull(mi.getId());
    }

    @Test
    void fullConstructor_setsId() {
        MealIngredient mi = new MealIngredient(99, 1, 2, 100.0, "ml");
        // NOTE: this test will fail — the 5-arg constructor does not assign this.id = id
        assertEquals(99, mi.getId());
    }

    @Test
    void constructor_acceptsNullUnit() {
        MealIngredient mi = new MealIngredient(1, 2, 1.0, null);
        assertNull(mi.getUnit());
    }
}
