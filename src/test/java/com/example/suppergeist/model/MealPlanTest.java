package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MealPlanTest {

    @Test
    void fullConstructor_setsAllFields() {
        MealPlan plan = new MealPlan(1, 42, LocalDate.of(2026, 4, 7));
        assertEquals(1, plan.getId());
        assertEquals(42, plan.getUserId());
        assertEquals(LocalDate.of(2026, 4, 7), plan.getStartDate());
    }

    @Test
    void fullConstructor_acceptsNullId() {
        MealPlan plan = new MealPlan(null, 1, LocalDate.of(2026, 4, 7));
        assertNull(plan.getId());
    }

    @Test
    void shortConstructor_setsNullId() {
        MealPlan plan = new MealPlan(42, LocalDate.of(2026, 4, 7));
        assertNull(plan.getId());
    }

    @Test
    void shortConstructor_setsUserIdAndStartDate() {
        MealPlan plan = new MealPlan(42, LocalDate.of(2026, 4, 7));
        assertEquals(42, plan.getUserId());
        assertEquals(LocalDate.of(2026, 4, 7), plan.getStartDate());
    }
}
