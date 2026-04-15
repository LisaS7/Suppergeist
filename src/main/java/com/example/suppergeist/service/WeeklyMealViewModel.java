package com.example.suppergeist.service;

import java.time.LocalDate;

public record WeeklyMealViewModel(
        int mealPlanId,
        LocalDate date,
        String dayLabel,
        String mealType,
        String mealName
) {
}
