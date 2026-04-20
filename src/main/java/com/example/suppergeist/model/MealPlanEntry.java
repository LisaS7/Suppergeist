package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class MealPlanEntry {
    private final Integer id;
    private final int mealPlanId;
    private final int mealId;
    private final int dayOffset;
    private final String mealType;
    private final String mealName;

    public MealPlanEntry(Integer id, int mealPlanId, int mealId, int dayOffset, String mealType, String mealName) {
        this.id = id;
        this.mealPlanId = mealPlanId;
        this.mealId = mealId;
        this.dayOffset = dayOffset;
        this.mealType = mealType;
        this.mealName = mealName;
    }
    
    public MealPlanEntry(int mealPlanId, int mealId, int dayOffset, String mealType, String mealName) {
        this(null, mealPlanId, mealId, dayOffset, mealType, mealName);
    }
}
