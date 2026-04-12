package com.example.suppergeist.service;

import com.example.suppergeist.model.MealPlanEntry;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.repository.MealIngredientRow;
import com.example.suppergeist.repository.MealPlanEntryRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShoppingListService {
    record ShoppingItem(String name, Double totalQuantity, String unit, String category) {
    }

    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final MealIngredientRepository mealIngredientRepository;

    public ShoppingListService(MealPlanEntryRepository mealPlanEntryRepository, MealIngredientRepository mealIngredientRepository) {
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.mealIngredientRepository = mealIngredientRepository;
    }

    public List<ShoppingItem> buildList(int mealPlanId) throws SQLException {
        List<MealPlanEntry> entries = mealPlanEntryRepository.getEntriesByMealPlanId(mealPlanId);

        HashMap<Integer, ShoppingItem> ingredientList = new HashMap<>();
        for (MealPlanEntry entry : entries) {
            int mealId = entry.getMealId();
            List<MealIngredientRow> mealIngredients = mealIngredientRepository.getIngredientsWithNameForMeal(mealId);
            for (MealIngredientRow row : mealIngredients) {
                int id = row.ingredient().getId();
                double current = ingredientList.containsKey(id) ? ingredientList.get(id).totalQuantity() : 0.0;

                ShoppingItem shoppingItem = new ShoppingItem(
                        row.ingredient().getName(),
                        current + row.quantity(),
                        row.unit(),
                        ""
                );
                ingredientList.put(id, shoppingItem);
            }
        }
        return new ArrayList<>(ingredientList.values());
    }
}
