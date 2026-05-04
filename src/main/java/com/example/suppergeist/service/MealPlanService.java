package com.example.suppergeist.service;

import com.example.suppergeist.model.Meal;
import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.repository.MealRepository;
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
    private final MealRepository mealRepository;
    private final MealPlanRepository mealPlanRepository;
    private static final DateTimeFormatter dayLabelFormatter = DateTimeFormatter.ofPattern("EEEE d MMM");

    public MealPlanService(MealRepository mealRepository, MealPlanRepository mealPlanRepository) {
        this.mealRepository = mealRepository;
        this.mealPlanRepository = mealPlanRepository;

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
        List<Meal> entries = mealRepository.getMeals(plan.id());
        List<WeeklyMealViewModel> weeklyMeals = new ArrayList<>();

        for (Meal entry : entries) {
            LocalDate mealDate = startDate.plusDays(entry.getDayOffset());
            String dayLabel = formatDayLabel(mealDate);

            WeeklyMealViewModel vm = new WeeklyMealViewModel(entry.getMealPlanId(), entry.getId(), mealDate, dayLabel, entry.getMealType(), entry.getMealName());
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

    public int addMealToSlot(String mealName, String mealType, int mealPlanId, int dayOffset) throws SQLException {
        Meal entry = new Meal(mealPlanId, dayOffset, mealType, mealName);
        mealRepository.create(entry);
        return entry.getId();
    }

    public void updateMeal(int mealId, String mealName, String mealType) throws SQLException {
        mealRepository.update(mealId, mealName, mealType);
    }

    public void deleteMeal(int mealId) throws SQLException {
        mealRepository.delete(mealId);
    }
}
