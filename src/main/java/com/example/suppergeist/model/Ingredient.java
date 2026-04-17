package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Ingredient {
    private final Integer id;
    private final String name;
    private final String foodCode;
    private final Double energyKcal;
    private final Double proteinG;
    private final Double fatG;
    private final Double carbohydrateG;
    private final Double totalSugarsG;
    private final Double fibreG;
    private final Double vitaminAMcg;
    private final Double vitaminCMg;
    private final Double vitaminDMcg;
    private final Double vitaminEMg;
    private final Double vitaminB12Mcg;
    private final Double folateMcg;

    public Ingredient(String name, String foodCode) {
        this.id = null;
        this.name = name;
        this.foodCode = foodCode;
        this.energyKcal = null;
        this.proteinG = null;
        this.fatG = null;
        this.carbohydrateG = null;
        this.totalSugarsG = null;
        this.fibreG = null;
        this.vitaminAMcg = null;
        this.vitaminCMg = null;
        this.vitaminDMcg = null;
        this.vitaminEMg = null;
        this.vitaminB12Mcg = null;
        this.folateMcg = null;
    }

    @Override
    public String toString() {
        return name;
    }
}
