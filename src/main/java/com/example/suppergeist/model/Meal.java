package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Meal {
    private Integer id;
    private String name;

    public Meal(String name) {
        this.name = name;
    }
}
