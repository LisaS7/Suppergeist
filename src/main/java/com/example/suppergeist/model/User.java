package com.example.suppergeist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class User {
    private Integer id;
    private String name;

    public User(String name) {
        this.name = name;
    }

}
