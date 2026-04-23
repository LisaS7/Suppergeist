package com.example.suppergeist.service;

import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.model.MealPlanEntry;
import com.example.suppergeist.repository.MealPlanEntryRepository;
import com.example.suppergeist.repository.MealPlanRepository;

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
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanEntryRepository mealPlanEntryRepository;
    private static final DateTimeFormatter dayLabelFormatter = DateTimeFormatter.ofPattern("EEEE d MMM");

    public MealPlanService(MealPlanRepository mealPlanRepository, MealPlanEntryRepository mealPlanEntryRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanEntryRepository = mealPlanEntryRepository;
    }

    private String formatDayLabel(LocalDate date) {
        return date.format(dayLabelFormatter);
    }

    public List<WeeklyMealViewModel> getWeeklyMeals(int userId, LocalDate referenceDate) throws SQLException {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Optional<MealPlan> mealPlan = mealPlanRepository.getByUserAndStartDate(userId, startDate);
        if (mealPlan.isEmpty()) {
            return Collections.emptyList();
        }

        MealPlan plan = mealPlan.get();
        List<MealPlanEntry> entries = mealPlanEntryRepository.getMealPlanEntries(plan.id());
        List<WeeklyMealViewModel> weeklyMeals = new ArrayList<>();

        for (MealPlanEntry entry : entries) {
            LocalDate mealDate = startDate.plusDays(entry.getDayOffset());
            String dayLabel = formatDayLabel(mealDate);

            WeeklyMealViewModel vm = new WeeklyMealViewModel(entry.getMealPlanId(), entry.getMealId(), mealDate, dayLabel, entry.getMealType(), entry.getMealName());
            weeklyMeals.add(vm);
        }
        return weeklyMeals;
    }

    public MealPlan findPlanForWeek(int userId, LocalDate startDate) throws SQLException {
        Optional<MealPlan> mealPlan = mealPlanRepository.getByUserAndStartDate(userId, startDate);
        return mealPlan.orElse(null);
    }

    public MealPlan createEmptyPlan(int userId, LocalDate startDate) throws SQLException {
        return mealPlanRepository.create(new MealPlan(null, userId, startDate));
    }

    public void deletePlan(int id) throws SQLException {
        mealPlanRepository.delete(id);
    }
}
