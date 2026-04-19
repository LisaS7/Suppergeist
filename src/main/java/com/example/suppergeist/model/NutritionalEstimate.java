package com.example.suppergeist.model;

public record NutritionalEstimate(
        int cal,
        double proteinG,
        double carbsG,
        double fatG,
        double totalSugarsG,
        double fibreG,
        double vitaminAMcg,
        double vitaminCMg,
        double vitaminDMcg,
        double vitaminEMg,
        double vitaminB12Mcg,
        double folateMcg
) {
}
