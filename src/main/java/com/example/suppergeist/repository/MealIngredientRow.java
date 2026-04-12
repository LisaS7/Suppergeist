package com.example.suppergeist.repository;

import com.example.suppergeist.model.Ingredient;

public record MealIngredientRow(Ingredient ingredient, double quantity, String unit) {
}
