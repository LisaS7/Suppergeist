package com.example.suppergeist.model;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class UserPreferences {
    private Set<String> dietaryConstraints;
    private List<String> avoidIngredients;
    int servingsPerMeal;

    public UserPreferences defaults() {
        return new UserPreferences(Set.of(), List.of(), 2);
    }
}
