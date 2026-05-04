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
import java.util.logging.Logger;

public class MealPlanService {
    private static final Logger log = Logger.getLogger(MealPlanService.class.getName());

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
            log.fine(() -> "No meal plan found for user " + userId + " and week " + startDate);
            return Collections.emptyList();
        }

        MealPlan plan = mealPlan.get();
        List<Meal> entries = mealRepository.getMeals(plan.id());
        log.fine(() -> "Loaded " + entries.size() + " meals for plan " + plan.id());
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
        MealPlan plan = mealPlanRepository.create(new MealPlan(null, userId, startDate));
        log.info(() -> "Created meal plan " + plan.id() + " for user " + userId + " and week " + startDate);
        return plan;
    }


    public void deletePlan(int id) throws SQLException {
        mealPlanRepository.delete(id);
        log.info(() -> "Deleted meal plan " + id);
    }

    public int addMealToSlot(String mealName, String mealType, int mealPlanId, int dayOffset) throws SQLException {
        Meal entry = new Meal(mealPlanId, dayOffset, mealType, mealName);
        Meal savedEntry = mealRepository.create(entry);
        log.info(() -> "Added meal " + savedEntry.getId() + " to plan " + mealPlanId + " at day offset " + dayOffset);
        return savedEntry.getId();
    }

    public void updateMeal(int mealId, String mealName, String mealType) throws SQLException {
        mealRepository.update(mealId, mealName, mealType);
        log.info(() -> "Updated meal " + mealId + " with type " + mealType);
    }

    public void deleteMeal(int mealId) throws SQLException {
        mealRepository.delete(mealId);
        log.info(() -> "Deleted meal " + mealId);
    }
}
