package com.example.suppergeist;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.MealPlanEntryRepository;
import com.example.suppergeist.repository.MealPlanRepository;
import com.example.suppergeist.repository.MealRepository;
import com.example.suppergeist.service.MealPlanService;
import com.example.suppergeist.service.WeeklyMealViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class MainController {
    private MealPlanService mealPlanService;
    private List<WeeklyMealViewModel> weeklyMeals;

    @FXML
    private VBox prefsSidebar;

    @FXML
    private GridPane mealPlanGrid;

    @FXML
    private void togglePrefs() {
        prefsSidebar.setVisible(!prefsSidebar.isVisible());
        prefsSidebar.setManaged(prefsSidebar.isVisible());
    }

    public void initialize() {
        DatabaseManager dbManager = new DatabaseManager();
        MealRepository mealRepository = new MealRepository(dbManager);
        MealPlanRepository mealPlanRepository = new MealPlanRepository(dbManager);
        MealPlanEntryRepository mealPlanEntryRepository = new MealPlanEntryRepository(dbManager);

        mealPlanService = new MealPlanService(mealRepository, mealPlanRepository, mealPlanEntryRepository);

        try {
            weeklyMeals = mealPlanService.getWeeklyMeals(1, LocalDate.of(2026, 4, 6), DayOfWeek.MONDAY);

            Set<LocalDate> labelledDates = new HashSet<>();
            Map<LocalDate, Integer> nextRowForDate = new HashMap<>();

            for (WeeklyMealViewModel meal : weeklyMeals) {

                // Day label
                int column = meal.date().getDayOfWeek().getValue() - 1;

                if (!labelledDates.contains(meal.date())) {
                    Label dayLabel = new Label(meal.dayLabel());
                    mealPlanGrid.add(dayLabel, column, 0);
                    labelledDates.add(meal.date());
                }

                // Create card
                VBox card = new VBox();
                card.getStyleClass().add("meal-card");

                // Title
                Label nameLabel = new Label(meal.mealName());

                // Calories placeholder #TODO
                Label calorieLabel = new Label("-- kcal");

                card.getChildren().addAll(nameLabel, calorieLabel);
                
                int row = nextRowForDate.getOrDefault(meal.date(), 1);
                nextRowForDate.put(meal.date(), row + 1);
                mealPlanGrid.add(card, column, 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
