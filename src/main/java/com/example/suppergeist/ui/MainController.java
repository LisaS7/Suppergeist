package com.example.suppergeist.ui;

import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.*;
import com.example.suppergeist.service.*;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import lombok.Setter;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    @Setter private MealPlanService mealPlanService;
    @Setter private UserPreferencesService userPreferencesService;
    @Setter private ShoppingListService shoppingListService;
    @Setter private NutritionService nutritionService;

    private User user;
    private LocalDate currentWeekStart;
    private List<WeeklyMealViewModel> weeklyMeals;
    private static final Logger log = Logger.getLogger(MainController.class.getName());
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd LLLL yyyy");

    // UI Elements
    @FXML private GridPane mealPlanGrid;
    @FXML private Label weekLabel;

    @FXML private PreferencesSidebarController preferencesSidebarController;
    @FXML private ShoppingListController shoppingListController;

    @FXML
    private void togglePrefs() {
        preferencesSidebarController.toggleVisibility();
    }

    private void updateWeekLabel() {
        this.weekLabel.setText(this.currentWeekStart.format(formatter) + " - " + this.currentWeekStart.plusDays(6).format(formatter));
    }

    private void refreshMealPlanGrid() throws SQLException {
        mealPlanGrid.getChildren().clear();
        DayOfWeek weekStart = DayOfWeek.MONDAY;
        weeklyMeals = mealPlanService.getWeeklyMeals(user.getId(), currentWeekStart);

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

            NutritionalEstimate estimate = nutritionService.estimateForMeal(meal.mealId());
            StackPane card = buildMealCard(meal, estimate);

            int row = nextRowForDate.getOrDefault(meal.date(), 1);
            nextRowForDate.put(meal.date(), row + 1);
            mealPlanGrid.add(card, column, row);
        }
    }

    private StackPane buildMealCard(WeeklyMealViewModel meal, NutritionalEstimate estimate) {
        VBox front = new VBox();
        front.getStyleClass().add("meal-card");
        Label nameLabel = new Label(meal.mealName());
        Label calorieLabel = new Label(estimate != null ? estimate.cal() + " kcal" : "-- kcal");
        calorieLabel.getStyleClass().add("meal-kcal");
        front.getChildren().addAll(nameLabel, calorieLabel);

        VBox back = new VBox();
        back.getStyleClass().add("meal-card-back");
        back.setVisible(false);

        StackPane card = new StackPane(front, back);
        boolean[] flipped = {false};
        card.setOnMouseClicked(e -> flipCard(card, front, back, flipped));
        return card;
    }

    private void flipCard(StackPane card, VBox front, VBox back, boolean[] flipped) {
        RotateTransition firstHalf = new RotateTransition(Duration.millis(150), card);
        firstHalf.setAxis(Rotate.Y_AXIS);
        firstHalf.setFromAngle(0);
        firstHalf.setToAngle(90);
        firstHalf.setOnFinished(e -> {
            flipped[0] = !flipped[0];
            front.setVisible(!flipped[0]);
            back.setVisible(flipped[0]);
            RotateTransition secondHalf = new RotateTransition(Duration.millis(150), card);
            secondHalf.setAxis(Rotate.Y_AXIS);
            secondHalf.setFromAngle(-90);
            secondHalf.setToAngle(0);
            secondHalf.play();
        });
        firstHalf.play();
    }

    public void setup() throws SQLException {
        log.info("Initializing MainController");

        // TODO: resolve if multi-user support is added
        this.user = userPreferencesService.loadUser(1);
        this.currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        updateWeekLabel();

        // Sidebar
        preferencesSidebarController.setUserPreferencesService(userPreferencesService);
        preferencesSidebarController.setOnPreferencesSaved((User updatedUser) -> {
            try {
                this.user = updatedUser;
                refreshMealPlanGrid();
            } catch (SQLException e) {
                log.log(Level.SEVERE, "Failed to refresh meal plan grid", e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Refresh Error");
                alert.setHeaderText("Meal plan refresh failed");
                alert.setContentText("The grid could not be refreshed.\n\n" + e.getMessage());
                alert.showAndWait();
            }
        });

        preferencesSidebarController.setFormValues(this.user);
        refreshMealPlanGrid();
        log.info("Loaded " + weeklyMeals.size() + " meals");

        if (!weeklyMeals.isEmpty()) {
            int planId = weeklyMeals.getFirst().mealPlanId();
            LinkedHashMap<String, List<ShoppingItem>> shoppingList = shoppingListService.buildList(planId);
            shoppingListController.refresh(shoppingList);
        }
    }

    @FXML
    private void goToPreviousWeek() throws SQLException {
        this.currentWeekStart = currentWeekStart.minusDays(7);
        updateWeekLabel();
        refreshMealPlanGrid();
    }

    @FXML
    private void goToNextWeek() throws SQLException {
        this.currentWeekStart = currentWeekStart.plusDays(7);
        updateWeekLabel();
        refreshMealPlanGrid();
    }
}
