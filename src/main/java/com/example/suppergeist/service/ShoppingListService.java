package com.example.suppergeist.service;

import com.example.suppergeist.model.Meal;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.repository.MealRepository;

import java.sql.SQLException;
import java.util.*;

public class ShoppingListService {

    private final MealRepository mealPlanEntryRepository;
    private final MealIngredientRepository mealIngredientRepository;


    public ShoppingListService(MealRepository mealPlanEntryRepository, MealIngredientRepository mealIngredientRepository) {
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.mealIngredientRepository = mealIngredientRepository;
    }

    private String deriveCategory(String foodCode) {
        if (foodCode == null) {
            return "General";
        }

        String prefix = foodCode.split("-")[0];
        return switch (prefix) {
            case "11" -> "Bakery & Grains";
            case "12" -> "Dairy & Eggs";
            case "13" -> "Vegetables & Beans";
            case "14" -> "Fruit & Nuts";
            case "18", "19" -> "Meat";
            case "17", "50" -> "Food Cupboard";
            default -> "General";
        };
    }

    public Map<String, List<ShoppingItem>> buildList(int mealPlanId) throws SQLException {
        List<Meal> mealPlanEntries = mealPlanEntryRepository.getMeals(mealPlanId);
        Map<String, ShoppingItem> ingredients = new HashMap<>();
        for (Meal entry : mealPlanEntries) {
            int mealId = entry.getId();
            List<MealIngredientRow> rows = mealIngredientRepository.getIngredientsWithNutritionForMeal(mealId);
            for (MealIngredientRow row : rows) {
                String key = row.ingredient().getId() + "|" + row.unit();

                // merge: on first encounter, store newShoppingItem directly; on repeat, add quantities together
                ShoppingItem newShoppingItem = new ShoppingItem(row.ingredient().getName(), row.quantity(), row.unit(), row.ingredient().getFoodCode());
                ingredients.merge(key, newShoppingItem, (oldItem, newItem) -> new ShoppingItem(
                        oldItem.name(),
                        oldItem.totalQuantity() + newItem.totalQuantity(),
                        oldItem.unit(),
                        oldItem.foodCode()
                ));
            }
        }

        TreeMap<String, List<ShoppingItem>> grouped = new TreeMap<>();
        for (ShoppingItem item : ingredients.values()) {
            String category = deriveCategory(item.foodCode());
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
        }

        for (List<ShoppingItem> items : grouped.values()) {
            items.sort(Comparator.comparing(ShoppingItem::name));
        }

        return grouped;
    }
}
