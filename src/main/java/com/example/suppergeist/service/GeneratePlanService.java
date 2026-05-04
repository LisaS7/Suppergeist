package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

public class GeneratePlanService {
    private static final Logger log = Logger.getLogger(GeneratePlanService.class.getName());

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
        log.info(() -> "Generating meal plan for user " + user.getId() + " and week " + weekStart);
        List<Ingredient> ingredientList = ingredientRepository.getAllIngredients();
        log.info(() -> "Loaded " + ingredientList.size() + " candidate ingredients for generation");

        // Build prompt
        PromptBuilder promptBuilder = new PromptBuilder();
        String prompt = promptBuilder.build(user, ingredientList);

        // Generate
        String response = client.generate(prompt);

        // Parse response
        MealPlanParser parser = new MealPlanParser();
        List<MealPlanParser.ParsedMeal> parsedMeals = parser.parse(response);
        log.info(() -> "Parsed " + parsedMeals.size() + " generated meals");

        // Delete existing meal plan
        MealPlan existingPlan = mealPlanService.findPlanForWeek(user.getId(), weekStart);
        if (existingPlan != null) {
            log.info(() -> "Replacing existing meal plan " + existingPlan.id() + " for week " + weekStart);
            mealPlanService.deletePlan(existingPlan.id());
        }

        MealPlan newPlan = mealPlanService.createEmptyPlan(user.getId(), weekStart);
        int matchedIngredients = 0;
        int unmatchedIngredients = 0;
        for (int i = 0; i < parsedMeals.size(); i++) {
            MealPlanParser.ParsedMeal meal = parsedMeals.get(i);
            int mealId = mealPlanService.addMealToSlot(meal.name(), meal.mealType(), newPlan.id(), i);
            for (MealPlanParser.ParsedIngredient ing : meal.ingredients()) {
                boolean matched = false;
                for (Ingredient match : ingredientList) {
                    if (match.getName().equals(ing.name())) {
                        mealIngredientService.addIngredientToMeal(mealId, match.getId(), ing.quantity(), ing.unit());
                        matched = true;
                        matchedIngredients++;
                        break;
                    }
                }
                if (!matched) {
                    unmatchedIngredients++;
                    log.warning(() -> "Generated ingredient did not match repository ingredient and was skipped: " + ing.name());
                }
            }
        }
        int matchedCount = matchedIngredients;
        int unmatchedCount = unmatchedIngredients;
        log.info(() -> "Saved generated meal plan " + newPlan.id() + " with " + parsedMeals.size()
                + " meals, " + matchedCount + " matched ingredients, and " + unmatchedCount + " skipped ingredients");
        return newPlan;
    }
}
