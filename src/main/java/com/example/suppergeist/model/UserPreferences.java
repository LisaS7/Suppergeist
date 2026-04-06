package com.example.suppergeist.model;

import java.util.List;
import java.util.Set;

public record UserPreferences(Set<String> dietaryConstraints,
                              List<String> avoidIngredients,
                              int servingsPerMeal) {


    public UserPreferences defaults() {
        return new UserPreferences(Set.of(), List.of(), 2);
    }
}
