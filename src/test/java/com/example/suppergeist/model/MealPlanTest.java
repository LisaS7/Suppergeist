package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MealPlanTest {

    @Test
    void constructor_setsAllFields() {
        MealPlan plan = new MealPlan(1, 42, LocalDate.of(2026, 4, 7));
        assertEquals(1, plan.id());
        assertEquals(42, plan.userId());
        assertEquals(LocalDate.of(2026, 4, 7), plan.startDate());
    }

    @Test
    void constructor_acceptsNullId() {
        MealPlan plan = new MealPlan(null, 1, LocalDate.of(2026, 4, 7));
        assertNull(plan.id());
    }

}
