package com.example.suppergeist.model;

import lombok.Getter;

@Getter
public class Meal {
    private final Integer id;
    private final String name;

    public Meal(Integer id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Meal name must not be blank");
        }
        this.id = id;
        this.name = name;
    }

    public Meal(String name) {
        this(null, name);
    }

}
