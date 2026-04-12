package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MealPlanEntryTest {

    @Test
    void fullConstructor_setsAllFields() {
        MealPlanEntry entry = new MealPlanEntry(10, 1, 2, 3, "dinner");
        assertEquals(10, entry.getId());
        assertEquals(1, entry.getMealPlanId());
        assertEquals(2, entry.getMealId());
        assertEquals(3, entry.getDayOffset());
        assertEquals("dinner", entry.getMealType());
    }

    @Test
    void fullConstructor_acceptsNullId() {
        MealPlanEntry entry = new MealPlanEntry(null, 1, 2, 0, "breakfast");
        assertNull(entry.getId());
    }

    @Test
    void shortConstructor_setsNullId() {
        MealPlanEntry entry = new MealPlanEntry(1, 2, 0, "breakfast");
        assertNull(entry.getId());
    }

    @Test
    void shortConstructor_setsAllFields() {
        MealPlanEntry entry = new MealPlanEntry(5, 10, 6, "lunch");
        assertEquals(5, entry.getMealPlanId());
        assertEquals(10, entry.getMealId());
        assertEquals(6, entry.getDayOffset());
        assertEquals("lunch", entry.getMealType());
    }
}
