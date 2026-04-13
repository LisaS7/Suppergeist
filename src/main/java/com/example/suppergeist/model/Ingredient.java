package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Ingredient {
    private Integer id;
    private String name;
    private String foodCode;

    public Ingredient(String name, String foodCode) {
        this.name = name;
        this.foodCode = foodCode;
    }

    @Override
    public String toString() {
        return name;
    }
}


