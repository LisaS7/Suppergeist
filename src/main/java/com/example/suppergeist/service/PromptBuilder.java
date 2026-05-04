package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;

import java.util.List;

public class PromptBuilder {
    public String build(User user, List<Ingredient> ingredients) {
        List<String> avoidFoodNames = ingredients.stream()
                .filter(i -> user.getAvoidFoodCodes().contains(i.getFoodCode()))
                .map(Ingredient::getName)
                .toList();
        List<String> allowedIngredients = ingredients.stream()
                .map(Ingredient::getName)
                .filter(name -> !avoidFoodNames.contains(name))
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
                Output format must be valid JSON only. No extra text.
                """ +
                "\nYou must only use ingredients from this list: " + String.join(";", allowedIngredients) +
                "\nDietary constraints: " + user.getDietaryConstraints() +
                "\nAvoid foods: " + String.join(";", avoidFoodNames) +
                "\nServings per meal: " + user.getServingsPerMeal() +
                """
                
                Return 7 meals.
                The JSON structure must be:
                
                {
                  "meals": [
                    {
                      "day": "Monday",
                      "mealType": "Dinner",
                      "name": "Meal name",
                      "ingredients": [
                        {
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
}
