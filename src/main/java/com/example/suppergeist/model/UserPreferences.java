package com.example.suppergeist.model;

import java.util.List;
import java.util.Set;

public record UserPreferences(Set<String> dietaryConstraints,
                              List<String> avoidIngredients,
                              int servingsPerMeal) {

    public UserPreferences {
        if (servingsPerMeal <= 0) {
            throw new IllegalArgumentException("Servings per meal must be greater than 0 (got: " + servingsPerMeal + ")");
        }
    }

    public UserPreferences defaults() {
        return new UserPreferences(Set.of(), List.of(), 2);
    }
}
