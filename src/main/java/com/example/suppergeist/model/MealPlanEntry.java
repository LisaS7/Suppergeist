package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MealPlanEntry {
    private Integer id;
    private int mealPlanId;
    private int mealId;
    private int dayOffset;
    private String mealType;

    public MealPlanEntry(int mealPlanId, int mealId, int dayOffset, String mealType) {
        this.mealPlanId = mealPlanId;
        this.mealId = mealId;
        this.dayOffset = dayOffset;
        this.mealType = mealType;
    }
}
