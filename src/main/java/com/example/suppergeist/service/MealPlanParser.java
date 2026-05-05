package com.example.suppergeist.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;

public class MealPlanParser {
    record ParsedIngredient(String foodCode, String name, double quantity, String unit) {}

    record ParsedMeal(String name, String mealType, List<ParsedIngredient> ingredients) {}

    private static class MealPlanResponse {
        private List<ParsedMeal> meals;
    }

    public List<ParsedMeal> parse(String json) throws MealPlanParseException {
        Gson gson = new Gson();
        try {
            MealPlanResponse response = gson.fromJson(json, MealPlanResponse.class);
            if (response == null || response.meals == null) {
                throw new MealPlanParseException("Failed to parse meal plan response");
            }
            validate(response.meals);
            return response.meals;
        } catch (JsonSyntaxException e) {
            throw new MealPlanParseException("Invalid JSON: " + e);
        }
    }

    private void validate(List<ParsedMeal> meals) throws MealPlanParseException {
        if (meals.size() != 7) {
            throw new MealPlanParseException("Expected exactly 7 meals but got " + meals.size());
        }

        for (int i = 0; i < meals.size(); i++) {
            ParsedMeal meal = meals.get(i);
            int mealNumber = i + 1;
            if (meal == null) {
                throw new MealPlanParseException("Meal " + mealNumber + " is missing");
            }
            if (isBlank(meal.name())) {
                throw new MealPlanParseException("Meal " + mealNumber + " is missing a name");
            }
            if (isBlank(meal.mealType())) {
                throw new MealPlanParseException("Meal " + mealNumber + " is missing a mealType");
            }
            if (meal.ingredients() == null) {
                throw new MealPlanParseException("Meal " + mealNumber + " is missing ingredients");
            }

            for (int j = 0; j < meal.ingredients().size(); j++) {
                ParsedIngredient ingredient = meal.ingredients().get(j);
                int ingredientNumber = j + 1;
                if (ingredient == null) {
                    throw new MealPlanParseException("Meal " + mealNumber + " ingredient " + ingredientNumber + " is missing");
                }
                if (isBlank(ingredient.foodCode())) {
                    throw new MealPlanParseException("Meal " + mealNumber + " ingredient " + ingredientNumber + " is missing a foodCode");
                }
                if (isBlank(ingredient.name())) {
                    throw new MealPlanParseException("Meal " + mealNumber + " ingredient " + ingredientNumber + " is missing a name");
                }
                if (ingredient.quantity() <= 0) {
                    throw new MealPlanParseException("Meal " + mealNumber + " ingredient " + ingredientNumber + " must have a positive quantity");
                }
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
