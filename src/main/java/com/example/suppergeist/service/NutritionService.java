package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.repository.MealIngredientRepository;

import java.sql.SQLException;
import java.util.List;

public class NutritionService {
    private final MealIngredientRepository mealIngredientRepository;

    public NutritionService(MealIngredientRepository mealIngredientRepository) {
        this.mealIngredientRepository = mealIngredientRepository;
    }

    // nutrients are per 100g; scale by actual quantity before summing
    private double scaledNutrient(Double nutrient, double quantity) {
        if (nutrient == null) {
            return 0.0;
        }
        return (quantity / 100) * nutrient;
    }

    /** Returns null if no ingredient in the meal has energyKcal data; caller should show "-- kcal". */
    public NutritionalEstimate estimateForMeal(int mealId) throws SQLException {
        List<MealIngredientRow> rows = mealIngredientRepository.getIngredientsWithNutritionForMeal(mealId);
        int calorieTotal = 0;
        double proteinTotal = 0;
        double carbsTotal = 0;
        double fatTotal = 0;
        double sugarTotal = 0;
        double fibreTotal = 0;
        double vitaminATotal = 0;
        double vitaminCTotal = 0;
        double vitaminDTotal = 0;
        double vitaminETotal = 0;
        double vitaminB12Total = 0;
        double folateTotal = 0;
        for (MealIngredientRow row : rows) {
            Ingredient ingredient = row.ingredient();
            // skip the whole row if kcal is absent — other nutrients without kcal are too incomplete to use
            if (ingredient.getEnergyKcal() == null) {
                continue;
            }

            calorieTotal += (int) ((row.quantity() / 100) * ingredient.getEnergyKcal());
            proteinTotal += scaledNutrient(ingredient.getProteinG(), row.quantity());
            carbsTotal += scaledNutrient(ingredient.getCarbohydrateG(), row.quantity());
            fatTotal += scaledNutrient(ingredient.getFatG(), row.quantity());
            sugarTotal += scaledNutrient(ingredient.getTotalSugarsG(), row.quantity());
            fibreTotal += scaledNutrient(ingredient.getFibreG(), row.quantity());
            vitaminATotal += scaledNutrient(ingredient.getVitaminAMcg(), row.quantity());
            vitaminCTotal += scaledNutrient(ingredient.getVitaminCMg(), row.quantity());
            vitaminDTotal += scaledNutrient(ingredient.getVitaminDMcg(), row.quantity());
            vitaminETotal += scaledNutrient(ingredient.getVitaminEMg(), row.quantity());
            vitaminB12Total += scaledNutrient(ingredient.getVitaminB12Mcg(), row.quantity());
            folateTotal += scaledNutrient(ingredient.getFolateMcg(), row.quantity());

        }
        if (calorieTotal == 0) {
            return null;
        }
        return new NutritionalEstimate(calorieTotal, proteinTotal, carbsTotal, fatTotal, sugarTotal, fibreTotal, vitaminATotal, vitaminCTotal, vitaminDTotal, vitaminETotal, vitaminB12Total, folateTotal);
    }

}
