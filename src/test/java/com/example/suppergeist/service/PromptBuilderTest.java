package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void build_includesCoreInstructionsAndJsonShape() {
        User user = new User(1, "Alice", Set.of(), Set.of(), 2, true, true);
        List<Ingredient> ingredients = List.of(
                new Ingredient(1, "Rice", "11-001"),
                new Ingredient(2, "Tomato", "12-001")
        );

        String prompt = promptBuilder.build(user, ingredients);

        assertTrue(prompt.contains("You are a meal planning assistant."));
        assertTrue(prompt.contains("Output format must be valid JSON only. No extra text."));
        assertTrue(prompt.contains("Return 7 meals."));
        assertTrue(prompt.contains("\"meals\""));
        assertTrue(prompt.contains("\"day\": \"Monday\""));
        assertTrue(prompt.contains("\"mealType\": \"Dinner\""));
        assertTrue(prompt.contains("\"ingredients\""));
        assertTrue(prompt.contains("\"quantity\": 200"));
        assertTrue(prompt.contains("\"unit\": \"g\""));
    }

    @Test
    void build_excludesAvoidedFoodsFromAllowedIngredients() {
        User user = new User(1, "Alice", Set.of(), Set.of("12-001"), 2, true, true);
        List<Ingredient> ingredients = List.of(
                new Ingredient(1, "Rice", "11-001"),
                new Ingredient(2, "Peanuts", "12-001"),
                new Ingredient(3, "Tomato", "13-001")
        );

        String prompt = promptBuilder.build(user, ingredients);

        assertTrue(prompt.contains("You must only use ingredients from this list: Rice;Tomato"));
        assertTrue(prompt.contains("Avoid foods: Peanuts"));
        assertFalse(prompt.contains("You must only use ingredients from this list: Rice;Peanuts;Tomato"));
    }

    @Test
    void build_includesDietaryConstraintsAndServings() {
        User user = new User(1, "Alice", Set.of("vegan"), Set.of(), 4, true, true);
        List<Ingredient> ingredients = List.of(new Ingredient(1, "Lentils", "14-001"));

        String prompt = promptBuilder.build(user, ingredients);

        assertTrue(prompt.contains("Dietary constraints: [vegan]"));
        assertTrue(prompt.contains("Servings per meal: 4"));
    }

    @Test
    void build_listsAllIngredientsWhenUserHasNoAvoidFoods() {
        User user = new User(1, "Alice", Set.of(), Set.of(), 2, true, true);
        List<Ingredient> ingredients = List.of(
                new Ingredient(1, "Rice", "11-001"),
                new Ingredient(2, "Tomato", "12-001")
        );

        String prompt = promptBuilder.build(user, ingredients);

        assertTrue(prompt.contains("You must only use ingredients from this list: Rice;Tomato"));
        assertTrue(prompt.contains("Avoid foods: "));
    }
}
