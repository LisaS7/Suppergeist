package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class GeneratePlanService {
    private final IngredientRepository ingredientRepository;
    private final OllamaClient client;
    private final MealPlanService mealPlanService;
    private final MealIngredientService mealIngredientService;

    public GeneratePlanService(IngredientRepository ingredientRepository, OllamaClient client, MealPlanService mealPlanService, MealIngredientService mealIngredientService) {
        this.ingredientRepository = ingredientRepository;
        this.client = client;
        this.mealPlanService = mealPlanService;
        this.mealIngredientService = mealIngredientService;
    }

    public MealPlan generateAndSave(User user, LocalDate weekStart) throws IOException, MealPlanParseException, SQLException {
        List<Ingredient> ingredientList = ingredientRepository.getAllIngredients();

        // Build prompt
        PromptBuilder promptBuilder = new PromptBuilder();
        String prompt = promptBuilder.build(user, ingredientList);

        // Generate
        String response = client.generate(prompt);

        // Parse response
        MealPlanParser parser = new MealPlanParser();
        List<MealPlanParser.ParsedMeal> parsedMeals = parser.parse(response);

        // Delete existing meal plan
        MealPlan existingPlan = mealPlanService.findPlanForWeek(user.getId(), weekStart);
        if (existingPlan != null) {
            mealPlanService.deletePlan(existingPlan.id());
        }

        MealPlan newPlan = mealPlanService.createEmptyPlan(user.getId(), weekStart);
        for (int i = 0; i < parsedMeals.size(); i++) {
            MealPlanParser.ParsedMeal meal = parsedMeals.get(i);
            int mealId = mealPlanService.addMealToSlot(meal.name(), meal.mealType(), newPlan.id(), i);
            for (MealPlanParser.ParsedIngredient ing : meal.ingredients()) {
                for (Ingredient match : ingredientList) {
                    if (match.getName().equals(ing.name())) {
                        mealIngredientService.addIngredientToMeal(mealId, match.getId(), ing.quantity(), ing.unit());
                        break;
                    }
                }
            }
        }
        return newPlan;
    }
}
