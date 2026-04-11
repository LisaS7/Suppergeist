package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MealTest {

    @Test
    void constructor_throwsOnNullName() {
        assertThrows(IllegalArgumentException.class, () -> new Meal(null));
    }

    @Test
    void constructor_throwsOnBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Meal("   "));
    }

    @Test
    void constructor_throwsOnEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new Meal(""));
    }

    @Test
    void nameOnlyConstructor_setsNullId() {
        Meal meal = new Meal("Pasta");
        assertNull(meal.getId());
        assertEquals("Pasta", meal.getName());
    }

    @Test
    void fullConstructor_setsIdAndName() {
        Meal meal = new Meal(42, "Pasta");
        assertEquals(42, meal.getId());
        assertEquals("Pasta", meal.getName());
    }

    @Test
    void fullConstructor_acceptsNullId() {
        Meal meal = new Meal(null, "Pasta");
        assertNull(meal.getId());
    }
}
