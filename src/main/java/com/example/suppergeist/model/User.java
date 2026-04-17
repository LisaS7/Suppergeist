package com.example.suppergeist.model;

import lombok.Getter;

import java.util.Set;

@Getter
public class User {
    private final Integer id;
    private final String name;
    private final Set<String> dietaryConstraints;
    private final Set<String> avoidFoodCodes;
    private final int servingsPerMeal;
    private final boolean showCalories;
    private final boolean showNutritionalInfo;

    public User(Integer id, String name, Set<String> dietaryConstraints, Set<String> avoidFoodCodes, int servingsPerMeal, boolean showCalories, boolean showNutritionalInfo) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name must not be null or blank");
        }

        if (servingsPerMeal <= 0) {
            throw new IllegalArgumentException("Servings per meal must be greater than 0 (got: " + servingsPerMeal + ")");
        }

        this.showCalories = showCalories;
        this.showNutritionalInfo = showNutritionalInfo;
        this.id = id;
        this.name = name;
        this.dietaryConstraints = dietaryConstraints == null ? Set.of() : Set.copyOf(dietaryConstraints);
        this.avoidFoodCodes = avoidFoodCodes == null ? Set.of() : Set.copyOf(avoidFoodCodes);
        this.servingsPerMeal = servingsPerMeal;
    }

    public User(String name) {
        this(null, name, Set.of(), Set.of(), 2, true, true);
    }

}
