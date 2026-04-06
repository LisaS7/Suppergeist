package com.example.suppergeist.service;

import java.time.LocalDate;

public record WeeklyMealViewModel(
        LocalDate date,
        String dayLabel,
        String mealType,
        String mealName
) {
}
