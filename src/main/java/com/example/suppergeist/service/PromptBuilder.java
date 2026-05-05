package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import com.google.gson.Gson;

import java.util.List;

public class PromptBuilder {
    private static final Gson GSON = new Gson();

    public String build(User user, List<Ingredient> ingredients) {
        List<Ingredient> ingredientsWithFoodCodes = ingredients.stream()
                .filter(i -> !isBlank(i.getFoodCode()))
                .toList();
        List<Ingredient> avoidedIngredients = ingredientsWithFoodCodes.stream()
                .filter(i -> user.getAvoidFoodCodes().contains(i.getFoodCode()))
                .toList();
        List<Ingredient> allowedIngredients = ingredientsWithFoodCodes.stream()
                .filter(i -> !user.getAvoidFoodCodes().contains(i.getFoodCode()))
                .toList();
        List<String> avoidFoodNames = avoidedIngredients.stream()
                .map(Ingredient::getName)
                .toList();
        List<AllowedIngredient> allowedIngredientEntries = allowedIngredients.stream()
                .map(i -> new AllowedIngredient(i.getFoodCode(), i.getName()))
                .toList();

        return """
                You are a meal planning assistant.
                Your job is to generate a weekly meal plan based on user preferences and constraints.
                You must follow these rules strictly:
                1. Respect all dietary constraints.
                2. Never include ingredients the user wants to avoid.
                3. Generate realistic, simple meals (no restaurant-style complexity).
                4. Meals should be practical for home cooking.
                5. Reuse ingredients across meals where reasonable to reduce waste.
                6. Each meal must include a list of ingredients with approximate quantities.
                7. Assume meals are for the specified number of servings.
                8. Every ingredient in the response must include the exact foodCode from the allowed ingredient list.
                Output format must be valid JSON only. No extra text.
                """ +
                "\nYou must only use ingredients from this list: " + GSON.toJson(allowedIngredientEntries) +
                "\nDietary constraints: " + user.getDietaryConstraints() +
                "\nAvoid foods: " + String.join(";", avoidFoodNames) +
                "\nServings per meal: " + user.getServingsPerMeal() +
                """
                        
                        Return 7 meals.
                        The JSON structure must be:
                        
                        {
                          "meals": [
                            {
                              "mealType": "Dinner",
                              "name": "Meal name",
                              "ingredients": [
                                {
                                  "foodCode": "11-001",
                                  "name": "Ingredient name",
                                  "quantity": 200,
                                  "unit": "g"
                                }
                              ]
                            }
                          ]
                        }
                        """;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AllowedIngredient(String foodCode, String name) {}
}
