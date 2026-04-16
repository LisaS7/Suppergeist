package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MealPlanEntry {
    private final Integer id;
    private final int mealPlanId;
    private final int mealId;
    private final int dayOffset;
    private final String mealType;

    public MealPlanEntry(int mealPlanId, int mealId, int dayOffset, String mealType) {
        this.id = null;
        this.mealPlanId = mealPlanId;
        this.mealId = mealId;
        this.dayOffset = dayOffset;
        this.mealType = mealType;
    }
}
