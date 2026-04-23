package com.example.suppergeist.ui;

import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.model.ShoppingItem;
import com.example.suppergeist.model.User;
import com.example.suppergeist.service.*;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
    private static final DayOfWeek WEEK_START_DAY = DayOfWeek.MONDAY;
    private static final Logger log = Logger.getLogger(MainController.class.getName());
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd LLLL yyyy");

    @FXML private GridPane mealPlanGrid;
    @FXML private Label weekLabel;
    @FXML private HBox footerBox;
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
        this.weekLabel.setText(this.currentWeekStart.format(this.formatter) + " - " + this.currentWeekStart.plusDays(6).format(this.formatter));
    }

    private void populateMealCards(List<WeeklyMealViewModel> weeklyMeals, Map<Integer, NutritionalEstimate> estimates, Map<LocalDate, Integer> nextRowForDate, Set<Integer> mealsWithNoIngredients) {
        Set<LocalDate> labelledDates = new HashSet<>();
        for (WeeklyMealViewModel meal : weeklyMeals) {
            // Day label
            int column = columnFor(meal.date());
            if (!labelledDates.contains(meal.date())) {
                Label dayLabel = new Label(meal.dayLabel());
                this.mealPlanGrid.add(dayLabel, column, 0);
                labelledDates.add(meal.date());
            }

            NutritionalEstimate estimate = estimates.get(meal.mealId());
            String toolTipText = null;
            if (estimate == null) {
                toolTipText = mealsWithNoIngredients.contains(meal.mealId())
                        ? "No ingredients recorded"
                        : "No nutrition data for this meal";
            }

            StackPane card = buildMealCard(meal, estimate, toolTipText);
            int row = nextRowForDate.getOrDefault(meal.date(), 1);
            nextRowForDate.put(meal.date(), row + 1);

            this.mealPlanGrid.add(card, column, row);
        }
    }

    private void appendCalorieTotals(Map<LocalDate, Integer> nextRowForDate, Map<LocalDate, Integer> calorieTotals) {
        for (Map.Entry<LocalDate, Integer> entry : calorieTotals.entrySet()) {
            int column = columnFor(entry.getKey());
            this.mealPlanGrid.add(new Label("Total Calories: " + entry.getValue()), column, nextRowForDate.getOrDefault(entry.getKey(), 1));
        }
    }

    private void refreshMealPlanGrid() throws SQLException {
        this.mealPlanGrid.getChildren().clear();
        this.weeklyCalories.setText("");
        this.createButton.setVisible(false);
        this.deleteButton.setVisible(false);
        this.createButton.setManaged(false);
        this.deleteButton.setManaged(false);

        List<WeeklyMealViewModel> weeklyMeals = this.mealPlanService.getWeeklyMeals(this.user.getId(), this.currentWeekStart);
        if (weeklyMeals.isEmpty()) {
            this.mealPlanGrid.getChildren().add(new Label("No plan for this week"));
            this.shoppingListController.refresh(new HashMap<>());
            this.createButton.setVisible(true);
            this.createButton.setManaged(true);
            return;
        }

        List<Integer> mealIds = weeklyMeals.stream().map(WeeklyMealViewModel::mealId).toList();
        Map<Integer, NutritionalEstimate> estimates = this.nutritionService.estimatesForMeals(mealIds);
        Set<Integer> mealsWithNoIngredients = this.nutritionService.mealIdsWithNoIngredients(mealIds);

        Map<LocalDate, Integer> nextRowForDate = new HashMap<>();
        Map<LocalDate, Integer> calorieTotals = this.nutritionService.dailyCalorieTotals(weeklyMeals, estimates);

        populateMealCards(weeklyMeals, estimates, nextRowForDate, mealsWithNoIngredients);
        if (this.user.isShowCalories()) {
            appendCalorieTotals(nextRowForDate, calorieTotals);
            int weeklyTotal = calorieTotals.values().stream().reduce(0, Integer::sum);
            this.weeklyCalories.setText("Weekly calories: " + weeklyTotal);
        }

        // Buttons!
        this.deleteButton.setVisible(true);
        this.deleteButton.setManaged(true);

        // Shopping List
        int planId = weeklyMeals.getFirst().mealPlanId();
        Map<String, List<ShoppingItem>> shoppingList = this.shoppingListService.buildList(planId);
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
}
