package com.example.suppergeist.ui;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.MealPlanEntryRepository;
import com.example.suppergeist.repository.MealPlanRepository;
import com.example.suppergeist.repository.MealRepository;
import com.example.suppergeist.repository.UserRepository;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private MealPlanService mealPlanService;
    private UserRepository userRepository;
    private List<WeeklyMealViewModel> weeklyMeals;
    private static final Logger log = Logger.getLogger(MainController.class.getName());

    // UI Elements
    @FXML private GridPane mealPlanGrid;
    @FXML private PreferencesSidebarController preferencesSidebarController;

    @FXML
    private void togglePrefs() {
        preferencesSidebarController.toggleVisibility();
    }

    private void refreshMealPlanGrid() throws SQLException {
        mealPlanGrid.getChildren().clear();
        User user = userRepository.getUser(1);
        DayOfWeek weekStart = DayOfWeek.of(user.getWeekStartDay());
        weeklyMeals = mealPlanService.getWeeklyMeals(user.getId(), LocalDate.of(2026, 4, 6));

        Set<LocalDate> labelledDates = new HashSet<>();
        Map<LocalDate, Integer> nextRowForDate = new HashMap<>();

        for (WeeklyMealViewModel meal : weeklyMeals) {

            // Day label
            int column = meal.date().getDayOfWeek().getValue() - weekStart.getValue();
            if (column < 0) {
                column += 7;
            }

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
            mealPlanGrid.add(card, column, row);
        }
    }

    public void initialize() {
        log.info("Initializing MainController");
        DatabaseManager dbManager = new DatabaseManager();
        this.userRepository = new UserRepository(dbManager);
        MealRepository mealRepository = new MealRepository(dbManager);
        MealPlanRepository mealPlanRepository = new MealPlanRepository(dbManager);
        MealPlanEntryRepository mealPlanEntryRepository = new MealPlanEntryRepository(dbManager);

        // Sidebar
        preferencesSidebarController.setUserRepository(userRepository);
        preferencesSidebarController.setOnPreferencesSaved(() -> {
            try {
                refreshMealPlanGrid();
            } catch (SQLException e) {
                log.log(Level.SEVERE, "Failed to refresh meal plan grid", e);
            }
        });

        mealPlanService = new MealPlanService(mealRepository, mealPlanRepository, mealPlanEntryRepository);

        try {
            // TODO - remove duplication of loading user twice
            preferencesSidebarController.loadUser(1);
            refreshMealPlanGrid();
            log.info("Loaded " + weeklyMeals.size() + " meals");

        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to load meals", e);
        }
    }
}
