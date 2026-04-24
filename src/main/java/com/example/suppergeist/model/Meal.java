package com.example.suppergeist.model;

import lombok.Getter;

@Getter
public class Meal {
    private final Integer id;
    private final int mealPlanId;
    private final int dayOffset;
    private final String mealType;
    private final String mealName;

    public Meal(Integer id, int mealPlanId, int dayOffset, String mealType, String mealName) {
        this.id = id;
        this.mealPlanId = mealPlanId;
        this.dayOffset = dayOffset;
        this.mealType = mealType;
        this.mealName = mealName;
    }

    public Meal(int mealPlanId, int dayOffset, String mealType, String mealName) {
        this(null, mealPlanId, dayOffset, mealType, mealName);
    }
}
