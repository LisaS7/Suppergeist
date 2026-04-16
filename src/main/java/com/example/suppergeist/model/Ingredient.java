package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Ingredient {
    private final Integer id;
    private final String name;
    private final String foodCode;

    public Ingredient(String name, String foodCode) {
        this.id = null;
        this.name = name;
        this.foodCode = foodCode;
    }

    @Override
    public String toString() {
        return name;
    }
}


