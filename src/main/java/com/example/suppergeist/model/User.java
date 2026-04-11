package com.example.suppergeist.model;

import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
public class User {
    private Integer id;
    private String name;
    private final Set<String> dietaryConstraints;
    private final List<String> avoidIngredients;
    private int servingsPerMeal;

    public User(Integer id, String name, Set<String> dietaryConstraints, List<String> avoidIngredients, int servingsPerMeal) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name must not be null or blank");
        }

        if (servingsPerMeal <= 0) {
            throw new IllegalArgumentException("Servings per meal must be greater than 0 (got: " + servingsPerMeal + ")");
        }

        this.id = id;
        this.name = name;
        this.dietaryConstraints = dietaryConstraints == null ? Set.of() : Set.copyOf(dietaryConstraints);
        this.avoidIngredients = avoidIngredients == null ? List.of() : List.copyOf(avoidIngredients);
        this.servingsPerMeal = servingsPerMeal;
    }

    public User(String name) {
        this(null, name, Set.of(), List.of(), 2);
    }
}
