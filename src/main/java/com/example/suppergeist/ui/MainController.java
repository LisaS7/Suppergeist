package com.example.suppergeist.ui;

import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.model.User;
import com.example.suppergeist.service.*;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    private static final Logger log = Logger.getLogger(MainController.class.getName());
    private final DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMM");
    private static final DayOfWeek WEEK_START_DAY = DayOfWeek.MONDAY;

    private User user;
    private LocalDate currentWeekStart;
    private MealPlan currentPlan;

    @FXML private GridPane mealPlanGrid;
    @FXML private Label weekLabel;
    @FXML private Label weeklyCalories;
    @FXML private Button createButton;
    @FXML private Button deleteButton;

    @FXML private PreferencesSidebarController preferencesSidebarController;
    @FXML private ShoppingListController shoppingListController;

    @FXML
    private void togglePrefs() {
        this.preferencesSidebarController.toggleVisibility();
    }

    private int columnFor(LocalDate date) {
        return date.getDayOfWeek().getValue() - WEEK_START_DAY.getValue();
    }

    private void updateWeekLabel() {
        this.weekLabel.setText(this.currentWeekStart.format(this.weekFormatter) + " - " + this.currentWeekStart.plusDays(6).format(this.weekFormatter));
    }

    private void updateDayLabels() {
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = currentWeekStart.plusDays(i);
            Label dayLabel = new Label(currentDate.format(dayFormatter));
            int column = columnFor(currentDate);
            this.mealPlanGrid.add(dayLabel, column, 0);
        }
    }

    private void populateMealCards(List<WeeklyMealViewModel> weeklyMeals, Map<Integer, NutritionalEstimate> estimates, Map<LocalDate, Integer> nextRowForDate, Set<Integer> mealsWithNoIngredients) {
        for (WeeklyMealViewModel meal : weeklyMeals) {
            NutritionalEstimate estimate = estimates.get(meal.mealId());
            String toolTipText = null;
            if (estimate == null) {
                toolTipText = mealsWithNoIngredients.contains(meal.mealId())
                        ? "No ingredients recorded"
                        : "No nutrition data for this meal";
            }

            StackPane card = buildMealCard(meal, estimate, toolTipText);
            int column = columnFor(meal.date());
            int row = nextRowForDate.get(meal.date());
            this.mealPlanGrid.add(card, column, row);

            nextRowForDate.put(meal.date(), row + 1);
        }
    }

    private void appendCalorieTotals(Map<LocalDate, Integer> nextRowForDate, Map<LocalDate, Integer> calorieTotals) {
        for (Map.Entry<LocalDate, Integer> entry : calorieTotals.entrySet()) {
            int column = columnFor(entry.getKey());
            int row = nextRowForDate.get(entry.getKey());
            this.mealPlanGrid.add(new Label("Total Calories: " + entry.getValue()), column, row);
            nextRowForDate.put(entry.getKey(), row + 1);
        }
    }

    private void refreshMealPlanGrid() throws SQLException {
        this.mealPlanGrid.getChildren().clear();
        this.weeklyCalories.setText("");
        this.createButton.setVisible(false);
        this.deleteButton.setVisible(false);
        this.createButton.setManaged(false);
        this.deleteButton.setManaged(false);

        this.currentPlan = mealPlanService.findPlanForWeek(user.getId(), currentWeekStart);

        if (currentPlan == null) {
            this.mealPlanGrid.getChildren().add(new Label("No plan for this week"));
            this.shoppingListController.refresh(new HashMap<>());
            this.createButton.setVisible(true);
            this.createButton.setManaged(true);
            return;
        }

        Map<LocalDate, Integer> nextRowForDate = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            nextRowForDate.put(currentWeekStart.plusDays(i), 1);
        }

        List<WeeklyMealViewModel> weeklyMeals = this.mealPlanService.getWeeklyMeals(this.user.getId(), this.currentWeekStart);
        List<Integer> mealIds = weeklyMeals.stream().map(WeeklyMealViewModel::mealId).toList();
        Map<Integer, NutritionalEstimate> estimates = this.nutritionService.estimatesForMeals(mealIds);
        Set<Integer> mealsWithNoIngredients = this.nutritionService.mealIdsWithNoIngredients(mealIds);

        Map<LocalDate, Integer> calorieTotals = this.nutritionService.dailyCalorieTotals(weeklyMeals, estimates);

        updateDayLabels();

        populateMealCards(weeklyMeals, estimates, nextRowForDate, mealsWithNoIngredients);
        if (this.user.isShowCalories()) {
            appendCalorieTotals(nextRowForDate, calorieTotals);
            int weeklyTotal = calorieTotals.values().stream().reduce(0, Integer::sum);
            this.weeklyCalories.setText("Weekly calories: " + weeklyTotal);
        }

        // + meal buttons
        int row = Collections.max(nextRowForDate.values()) + 1;
        for (Map.Entry<LocalDate, Integer> entry : nextRowForDate.entrySet()) {
            int column = columnFor(entry.getKey());
            mealPlanGrid.add(new Button("+"), column, row);
        }

        // Buttons!
        this.deleteButton.setVisible(true);
        this.deleteButton.setManaged(true);

        // Shopping List
        Map<String, List<ShoppingItem>> shoppingList = this.shoppingListService.buildList(currentPlan.id());
        this.shoppingListController.refresh(shoppingList);
    }

    private StackPane buildMealCard(WeeklyMealViewModel meal, NutritionalEstimate estimate, String toolTipText) {
        // FRONT
        VBox front = new VBox();
        front.getStyleClass().add("meal-card");
        // Meal name
        Label nameLabel = new Label(meal.mealName());
        front.getChildren().add(nameLabel);
        // Calories
        if (this.user.isShowCalories()) {
            Label calorieLabel = new Label(estimate != null ? estimate.cal() + " kcal" : "-- kcal");
            calorieLabel.getStyleClass().add("meal-kcal");
            front.getChildren().add(calorieLabel);
        }
        // ToolTip
        if (toolTipText != null) {
            Label tooltipLabel = new Label("!");
            front.getChildren().add(tooltipLabel);
            Tooltip.install(tooltipLabel, new Tooltip(toolTipText));
        }

        // BACK
        GridPane back = new GridPane();
        back.getStyleClass().add("meal-card-back");
        back.setHgap(10);
        back.setVisible(false);
        if (this.user.isShowNutritionalInfo() && estimate != null) {
            Separator sep = new Separator();
            GridPane.setColumnSpan(sep, 2);

            // row 5 is intentionally blank — separator fills it
            back.add(sep, 0, 5);

            back.add(new Label("Protein:"), 0, 0);
            back.add(new Label("Carbs:"), 0, 1);
            back.add(new Label("Fat:"), 0, 2);
            back.add(new Label("Sugar:"), 0, 3);
            back.add(new Label("Fibre:"), 0, 4);
            back.add(new Label("Vitamin A:"), 0, 6);
            back.add(new Label("Vitamin C:"), 0, 7);
            back.add(new Label("Vitamin D:"), 0, 8);
            back.add(new Label("Vitamin E:"), 0, 9);
            back.add(new Label("Vitamin B12:"), 0, 10);
            back.add(new Label("Folate:"), 0, 11);
            back.add(new Label(Math.round(estimate.proteinG()) + "g"), 1, 0);
            back.add(new Label(Math.round(estimate.carbsG()) + "g"), 1, 1);
            back.add(new Label(Math.round(estimate.fatG()) + "g"), 1, 2);
            back.add(new Label(Math.round(estimate.totalSugarsG()) + "g"), 1, 3);
            back.add(new Label(Math.round(estimate.fibreG()) + "g"), 1, 4);
            back.add(new Label(Math.round(estimate.vitaminAMcg()) + "mcg"), 1, 6);
            back.add(new Label(Math.round(estimate.vitaminCMg()) + "mg"), 1, 7);
            back.add(new Label(Math.round(estimate.vitaminDMcg()) + "mcg"), 1, 8);
            back.add(new Label(Math.round(estimate.vitaminEMg()) + "mg"), 1, 9);
            back.add(new Label(Math.round(estimate.vitaminB12Mcg()) + "mcg"), 1, 10);
            back.add(new Label(Math.round(estimate.folateMcg()) + "mcg"), 1, 11);
        }

        // ARRANGE
        StackPane card = new StackPane(front, back);
        SimpleBooleanProperty flipped = new SimpleBooleanProperty(false);
        card.setOnMouseClicked(e -> flipCard(card, front, back, flipped));
        return card;
    }

    private void flipCard(StackPane card, VBox front, GridPane back, SimpleBooleanProperty flipped) {
        RotateTransition firstHalf = new RotateTransition(Duration.millis(150), card);
        firstHalf.setAxis(Rotate.Y_AXIS);
        firstHalf.setFromAngle(0);
        firstHalf.setToAngle(90);
        firstHalf.setOnFinished(e -> {
            flipped.set(!flipped.get());
            front.setVisible(!flipped.get());
            back.setVisible(flipped.get());
            RotateTransition secondHalf = new RotateTransition(Duration.millis(150), card);
            secondHalf.setAxis(Rotate.Y_AXIS);
            secondHalf.setFromAngle(-90);
            secondHalf.setToAngle(0);
            secondHalf.play();
        });
        firstHalf.play();
    }

    private void handleGridRefreshError(SQLException e) {
        log.log(Level.SEVERE, "Failed to refresh meal plan grid", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Refresh Error");
        alert.setHeaderText("Meal plan refresh failed");
        alert.setContentText("The grid could not be refreshed.\n\n" + e.getMessage());
        alert.showAndWait();
    }

    public void setup() throws SQLException {
        log.info("Initializing MainController");
        this.mealPlanGrid.setHgap(32);
        this.mealPlanGrid.setVgap(12);

        // TODO: resolve if multi-user support is added
        this.user = this.userPreferencesService.loadUser(1);
        this.currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        updateWeekLabel();

        // Sidebar
        this.preferencesSidebarController.setUserPreferencesService(this.userPreferencesService);
        this.preferencesSidebarController.setOnPreferencesSaved((User updatedUser) -> {
            try {
                this.user = updatedUser;
                refreshMealPlanGrid();
            } catch (SQLException e) {
                handleGridRefreshError(e);
            }
        });

        this.preferencesSidebarController.setFormValues(this.user);
        refreshMealPlanGrid();
    }


    @FXML
    private void goToPreviousWeek() {
        try {
            this.currentWeekStart = this.currentWeekStart.minusDays(7);
            updateWeekLabel();
            refreshMealPlanGrid();
        } catch (SQLException e) {
            handleGridRefreshError(e);
        }
    }

    @FXML
    private void goToNextWeek() {
        try {
            this.currentWeekStart = this.currentWeekStart.plusDays(7);
            updateWeekLabel();
            refreshMealPlanGrid();
        } catch (SQLException e) {
            handleGridRefreshError(e);
        }
    }

    @FXML
    private void createPlan() {
        try {
            mealPlanService.createEmptyPlan(user.getId(), currentWeekStart);
            refreshMealPlanGrid();
        } catch (SQLException e) {
            handleGridRefreshError(e);
        }
    }

    @FXML
    private void deletePlan() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Do you want to delete this meal plan?");
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isPresent() && response.get() == ButtonType.OK) {
            try {
                mealPlanService.deletePlan(currentPlan.id());
                refreshMealPlanGrid();
            } catch (SQLException e) {
                handleGridRefreshError(e);
            }
        }
    }
}
