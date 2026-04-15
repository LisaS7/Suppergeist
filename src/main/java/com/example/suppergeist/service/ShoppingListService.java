package com.example.suppergeist.service;

import com.example.suppergeist.model.MealPlanEntry;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.repository.MealIngredientRow;
import com.example.suppergeist.repository.MealPlanEntryRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListService {

    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final MealIngredientRepository mealIngredientRepository;

    public ShoppingListService(MealPlanEntryRepository mealPlanEntryRepository, MealIngredientRepository mealIngredientRepository) {
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.mealIngredientRepository = mealIngredientRepository;
    }

    public List<ShoppingItem> buildList(int mealPlanId) throws SQLException {
        List<MealPlanEntry> mealPlanEntries = mealPlanEntryRepository.getEntriesByMealPlanId(mealPlanId);
        Map<String, ShoppingItem> ingredients = new HashMap<>();
        for (MealPlanEntry entry : mealPlanEntries) {
            int mealId = entry.getMealId();
            List<MealIngredientRow> rows = mealIngredientRepository.getIngredientsWithNameForMeal(mealId);
            for (MealIngredientRow row : rows) {
                String key = row.ingredient().getId() + "|" + row.unit();

                // merge: on first encounter, store newShoppingItem directly; on repeat, add quantities together
                ShoppingItem newShoppingItem = new ShoppingItem(row.ingredient().getName(), row.quantity(), row.unit(), "");
                ingredients.merge(key, newShoppingItem, (oldItem, newItem) -> new ShoppingItem(
                        oldItem.name(),
                        oldItem.totalQuantity() + newItem.totalQuantity(),
                        oldItem.unit(),
                        oldItem.category()
                ));
            }
        }
        return new ArrayList<>(ingredients.values());
    }
}
