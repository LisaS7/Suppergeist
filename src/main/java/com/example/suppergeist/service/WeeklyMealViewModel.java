package com.example.suppergeist.service;

import java.time.LocalDate;

public record WeeklyMealViewModel(
        int mealPlanId,
        int mealId,
        LocalDate date,
        String dayLabel,
        String mealType,
        String mealName
) {
}
