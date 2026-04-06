package com.example.suppergeist.service;

import com.example.suppergeist.model.Meal;
import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.model.MealPlanEntry;
import com.example.suppergeist.repository.MealPlanEntryRepository;
import com.example.suppergeist.repository.MealPlanRepository;
import com.example.suppergeist.repository.MealRepository;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MealPlanService {
    private final MealRepository mealRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanEntryRepository mealPlanEntryRepository;

    public MealPlanService(MealRepository mealRepository, MealPlanRepository mealPlanRepository, MealPlanEntryRepository mealPlanEntryRepository) {
        this.mealRepository = mealRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanEntryRepository = mealPlanEntryRepository;
    }

    private String formatDayLabel(LocalDate date) {
        DateTimeFormatter dayLabelFormatter = DateTimeFormatter.ofPattern("EEEE d MMM");
        return date.format(dayLabelFormatter);
    }

    public List<WeeklyMealViewModel> getWeeklyMeals(int userId, LocalDate referenceDate, DayOfWeek weekStartDay) throws SQLException {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.previousOrSame(weekStartDay));
        Optional<MealPlan> mealPlan = mealPlanRepository.getMealPlanByUserAndStartDate(userId, startDate);
        if (mealPlan.isEmpty()) {
            return Collections.emptyList();
        }

        MealPlan plan = mealPlan.get();
        List<MealPlanEntry> entries = mealPlanEntryRepository.getEntriesByMealPlanId(plan.getId());

        List<WeeklyMealViewModel> weeklyMeals = new ArrayList<>();

        for (MealPlanEntry entry : entries) {
            Optional<Meal> meal = mealRepository.getMealById(entry.getMealId());
            if (meal.isEmpty()) {
                continue;
            }
            Meal resolvedMeal = meal.get();

            LocalDate mealDate = startDate.plusDays(entry.getDayOffset());
            String dayLabel = formatDayLabel(mealDate);

            WeeklyMealViewModel vm = new WeeklyMealViewModel(mealDate, dayLabel, entry.getMealType(), resolvedMeal.getName());
            weeklyMeals.add(vm);
        }

        return weeklyMeals;
    }
}
