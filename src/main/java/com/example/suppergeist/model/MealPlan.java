package com.example.suppergeist.model;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;


@Getter
@AllArgsConstructor
public class MealPlan {
    private Integer id;
    private int userId;
    private LocalDate startDate;

    public MealPlan(int userId, LocalDate startDate) {
        this.userId = userId;
        this.startDate = startDate;
    }
}
