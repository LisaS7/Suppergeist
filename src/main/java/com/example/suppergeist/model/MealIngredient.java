package com.example.suppergeist.model;

import lombok.Getter;

@Getter
public class MealIngredient {
    private Integer id;
    private int mealId;
    private int ingredientId;
    private double quantity;
    private String unit;

    public MealIngredient(Integer id, int mealId, int ingredientId, double quantity, String unit) {
        if (!(quantity > 0)) {
            throw new IllegalArgumentException("Quantity must be greater than 0 (got: " + quantity + ")");
        }
        this.id = id;
        this.mealId = mealId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
    }

    public MealIngredient(int mealId, int ingredientId, double quantity, String unit) {
        this(null, mealId, ingredientId, quantity, unit);
    }

}
