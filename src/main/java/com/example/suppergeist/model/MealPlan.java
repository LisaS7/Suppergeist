package com.example.suppergeist.model;

import java.time.LocalDate;

public record MealPlan(Integer id, int userId, LocalDate startDate) {
}
