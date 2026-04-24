package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MealTest {

    @Test
    void fullConstructor_setsAllFields() {
        Meal meal = new Meal(10, 1, 3, "dinner", "Spaghetti Bolognese");
        assertEquals(10, meal.getId());
        assertEquals(1, meal.getMealPlanId());
        assertEquals(3, meal.getDayOffset());
        assertEquals("dinner", meal.getMealType());
        assertEquals("Spaghetti Bolognese", meal.getMealName());
    }

    @Test
    void fullConstructor_acceptsNullId() {
        Meal meal = new Meal(null, 1, 0, "breakfast", "Porridge");
        assertNull(meal.getId());
    }

    @Test
    void shortConstructor_setsNullId() {
        Meal meal = new Meal(1, 0, "breakfast", "Porridge");
        assertNull(meal.getId());
    }

    @Test
    void shortConstructor_setsRemainingFields() {
        Meal meal = new Meal(2, 5, "lunch", "Chicken Salad");
        assertEquals(2, meal.getMealPlanId());
        assertEquals(5, meal.getDayOffset());
        assertEquals("lunch", meal.getMealType());
        assertEquals("Chicken Salad", meal.getMealName());
    }
}
