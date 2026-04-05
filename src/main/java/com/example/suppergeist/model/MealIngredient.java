package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MealIngredient {
    private Integer id;
    private int mealId;
    private int ingredientId;
    private double quantity;
    private String unit;

    public MealIngredient(int mealId, int ingredientId, double quantity, String unit) {
        this.mealId = mealId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
    }

}
