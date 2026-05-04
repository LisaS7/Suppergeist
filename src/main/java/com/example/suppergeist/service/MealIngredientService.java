package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.MealIngredientRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class MealIngredientService {
    private static final Logger log = Logger.getLogger(MealIngredientService.class.getName());

    private final MealIngredientRepository mealIngredientRepository;
    private final IngredientRepository ingredientRepository;

    public MealIngredientService(MealIngredientRepository mealIngredientRepository, IngredientRepository ingredientRepository) {
        this.mealIngredientRepository = mealIngredientRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public List<MealIngredientRow> getIngredientsForMeal(int mealId) throws SQLException {
        return mealIngredientRepository.getIngredientsWithNutritionForMeal(mealId);
    }

    public Set<Integer> getMealIdsWithIngredients(List<Integer> mealIds) throws SQLException {
        return mealIngredientRepository.getMealIdsWithIngredients(mealIds);
    }

    public int addIngredientToMeal(int mealId, int ingredientId, double quantity, String unit) throws SQLException {
        int mealIngredientId = mealIngredientRepository.create(mealId, ingredientId, quantity, unit);
        log.info(() -> "Added ingredient " + ingredientId + " to meal " + mealId + " as row " + mealIngredientId);
        return mealIngredientId;
    }

    public void removeIngredientFromMeal(int mealIngredientId) throws SQLException {
        mealIngredientRepository.delete(mealIngredientId);
        log.info(() -> "Removed meal ingredient row " + mealIngredientId);
    }

    public List<Ingredient> searchIngredients(String searchTerm) throws SQLException {
        return ingredientRepository.searchByName(searchTerm);
    }
}
