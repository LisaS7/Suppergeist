package com.example.suppergeist.service;

import com.example.suppergeist.model.MealPlanEntry;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.repository.MealPlanEntryRepository;

import java.sql.SQLException;
import java.util.*;

public class ShoppingListService {

    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final MealIngredientRepository mealIngredientRepository;


    public ShoppingListService(MealPlanEntryRepository mealPlanEntryRepository, MealIngredientRepository mealIngredientRepository) {
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

    public LinkedHashMap<String, List<ShoppingItem>> buildList(int mealPlanId) throws SQLException {
        List<MealPlanEntry> mealPlanEntries = mealPlanEntryRepository.getEntriesByMealPlanId(mealPlanId);
        Map<String, ShoppingItem> ingredients = new HashMap<>();
        for (MealPlanEntry entry : mealPlanEntries) {
            int mealId = entry.getMealId();
            List<MealIngredientRow> rows = mealIngredientRepository.getIngredientsWithNameForMeal(mealId);
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

        LinkedHashMap<String, List<ShoppingItem>> grouped = new LinkedHashMap<>();
        for (ShoppingItem item : ingredients.values()) {
            String category = deriveCategory(item.foodCode());
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
        }

        for (List<ShoppingItem> items : grouped.values()) {
            items.sort(Comparator.comparing(ShoppingItem::name));
        }
        
        LinkedHashMap<String, List<ShoppingItem>> sorted = new LinkedHashMap<>();
        grouped.keySet().stream().sorted().forEach(key -> sorted.put(key, grouped.get(key)));
        return sorted;
    }
}
