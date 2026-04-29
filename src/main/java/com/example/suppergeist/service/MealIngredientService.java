package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.MealIngredientRepository;

import java.sql.SQLException;
import java.util.List;

public class MealIngredientService {
    private final MealIngredientRepository mealIngredientRepository;
    private final IngredientRepository ingredientRepository;

    public MealIngredientService(MealIngredientRepository mealIngredientRepository, IngredientRepository ingredientRepository) {
        this.mealIngredientRepository = mealIngredientRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public List<MealIngredientRow> getIngredientsForMeal(int mealId) throws SQLException {
        return mealIngredientRepository.getIngredientsWithNutritionForMeal(mealId);
    }

    public int addIngredientToMeal(int mealId, int ingredientId, double quantity, String unit) throws SQLException {
        return mealIngredientRepository.create(mealId, ingredientId, quantity, unit);
    }

    public void removeIngredientFromMeal(int mealIngredientId) throws SQLException {
        mealIngredientRepository.delete(mealIngredientId);
    }

    public List<Ingredient> searchIngredients(String searchTerm) throws SQLException {
        return ingredientRepository.searchByName(searchTerm);
    }
}
