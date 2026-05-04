package com.example.suppergeist.ui;

import com.example.suppergeist.model.*;
import com.example.suppergeist.service.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController {
    record MealFormResult(String name, String type) {
    }

    @Setter private MealPlanService mealPlanService;
    @Setter private MealIngredientService mealIngredientService;
    @Setter private UserPreferencesService userPreferencesService;
    @Setter private ShoppingListService shoppingListService;
    @Setter private NutritionService nutritionService;

    private static final Logger log = Logger.getLogger(MainController.class.getName());
    private final DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMM");
    private static final DayOfWeek WEEK_START_DAY = DayOfWeek.MONDAY;
    private static final String STYLESHEET = "/com/example/suppergeist/style.css";

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

    private void applyStylesheet(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource(STYLESHEET).toExternalForm()
        );
    }

    private Alert styledAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.setGraphic(null);
        applyStylesheet(alert);
        return alert;
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

    private void populateMealCards(MealCardBuilder builder, List<WeeklyMealViewModel> weeklyMeals, Map<Integer, NutritionalEstimate> estimates, Map<LocalDate, Integer> nextRowForDate, Set<Integer> mealsWithNoIngredients) {
        for (WeeklyMealViewModel meal : weeklyMeals) {
            NutritionalEstimate estimate = estimates.get(meal.mealId());
            String toolTipText = null;
            if (estimate == null) {
                toolTipText = mealsWithNoIngredients.contains(meal.mealId())
                        ? "No ingredients recorded"
                        : "No nutrition data for this meal";
            }

            StackPane card = builder.buildMealCard(meal, estimate, toolTipText, () -> showEditMealDialog(meal), () -> showEditIngredientDialog(meal.mealId()), () -> onRemove(meal.mealId()));
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

    private Optional<MealFormResult> buildDialog(String title, String header, String initialMealName, String initialMealType) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setHeaderText(header);

        VBox box = new VBox();

        TextField textField = new TextField();
        if (initialMealName == null) {
            textField.setPromptText("Enter a meal");
        } else {
            textField.setText(initialMealName);
        }

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("Breakfast", "Lunch", "Dinner");
        comboBox.setValue(initialMealType);

        box.getChildren().addAll(textField, comboBox);

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyStylesheet(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            return Optional.of(new MealFormResult(textField.getText(), comboBox.getValue()));
        }
        return Optional.empty();
    }

    private void showAddMealDialog(LocalDate mealDate) {
        Optional<MealFormResult> result = buildDialog("Add Meal", "Add a meal", null, "Dinner");
        if (result.isPresent()) {
            int dayOffset = (int) ChronoUnit.DAYS.between(currentWeekStart, mealDate);
            try {
                mealPlanService.addMealToSlot(result.get().name(), result.get().type(), currentPlan.id(), dayOffset);
                refreshMealPlanGrid();
            } catch (SQLException e) {
                handleGridRefreshError(e);
            }
        }
    }

    private void showEditMealDialog(WeeklyMealViewModel meal) {
        Optional<MealFormResult> result = buildDialog("Edit Meal", "Edit a meal", meal.mealName(), meal.mealType());
        if (result.isPresent()) {
            try {
                mealPlanService.updateMeal(meal.mealId(), result.get().name(), result.get().type());
                refreshMealPlanGrid();
            } catch (SQLException e) {
                handleGridRefreshError(e);
            }
        }
    }

    private HBox buildIngredientRow(VBox container, String name, double quantity, String unit, Runnable onRemove) {
        HBox row = new HBox();
        row.getStyleClass().add("ingredient-row");
        Label nameLabel = new Label(name);
        Label quantityLabel = new Label(String.valueOf(quantity));
        Label unitLabel = new Label(unit);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button removeButton = new Button("X");
        removeButton.setOnAction(e -> {
            onRemove.run();
            container.getChildren().remove(row);
        });
        row.getChildren().addAll(nameLabel, quantityLabel, unitLabel, spacer, removeButton);
        return row;
    }

    private HBox buildAddIngredientRow(int mealId, VBox ingredientList) {
        ComboBox<Ingredient> ingredientBox = new ComboBox<>();
        ingredientBox.setEditable(true);
        ingredientBox.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.length() < 2) return;
            try {
                List<Ingredient> results = mealIngredientService.searchIngredients(newValue);
                ingredientBox.getItems().setAll(results);
            } catch (SQLException e) {
                handleGridRefreshError(e);
            }
        }));


        TextField quantityField = new TextField();
        TextField unitField = new TextField();

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> {
            Ingredient ingredient = ingredientBox.getValue();
            if (ingredient == null) return;

            double quantity;
            try {
                quantity = Double.parseDouble(quantityField.getText());
            } catch (NumberFormatException ex) {
                return;
            }

            try {
                int mealIngredientId = mealIngredientService.addIngredientToMeal(mealId, ingredient.getId(), quantity, unitField.getText());
                HBox newRow = buildIngredientRow(ingredientList, ingredient.getName(), quantity, unitField.getText(), () -> {
                    try {
                        mealIngredientService.removeIngredientFromMeal(mealIngredientId);
                    } catch (SQLException ex) {
                        handleGridRefreshError(ex);
                    }
                });
                ingredientList.getChildren().add(newRow);
                ingredientBox.setValue(null);
                ingredientBox.getEditor().clear();
                quantityField.clear();
                unitField.clear();
            } catch (SQLException ex) {
                handleGridRefreshError(ex);
            }
        });

        HBox addRow = new HBox();
        addRow.getStyleClass().add("ingredient-row");
        addRow.getChildren().addAll(ingredientBox, quantityField, unitField, addButton);
        return addRow;
    }

    private void showEditIngredientDialog(int mealId) {
        List<MealIngredientRow> ingredients = null;
        try {
            ingredients = mealIngredientService.getIngredientsForMeal(mealId);
        } catch (SQLException e) {
            handleGridRefreshError(e);
            return;
        }

        VBox ingredientList = new VBox();
        for (MealIngredientRow ingredient : ingredients) {
            HBox row = buildIngredientRow(ingredientList, ingredient.ingredient().getName(), ingredient.quantity(), ingredient.unit(), () -> {
                try {
                    mealIngredientService.removeIngredientFromMeal(ingredient.id());
                } catch (SQLException ex) {
                    handleGridRefreshError(ex);
                }
            });
            ingredientList.getChildren().add(row);
        }

        HBox addRow = buildAddIngredientRow(mealId, ingredientList);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Ingredients");

        VBox box = new VBox();
        box.getChildren().addAll(ingredientList, addRow);

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        applyStylesheet(dialog);
        dialog.showAndWait();
        try {
            refreshMealPlanGrid();
        } catch (SQLException e) {
            handleGridRefreshError(e);
        }
    }

    private void onRemove(int mealId) {
        Alert alert = styledAlert(Alert.AlertType.CONFIRMATION, "Confirm Delete", "Do you want to delete this meal?");
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isPresent() && response.get() == ButtonType.OK) {
            try {
                mealPlanService.deleteMeal(mealId);
                refreshMealPlanGrid();
            } catch (SQLException e) {
                handleGridRefreshError(e);
            }
        }
    }

    private void refreshMealPlanGrid() throws SQLException {
        MealCardBuilder builder = new MealCardBuilder(this.user);
        this.mealPlanGrid.getChildren().clear();
        this.weeklyCalories.setText("");
        this.createButton.setVisible(false);
        this.deleteButton.setVisible(false);
        this.createButton.setManaged(false);
        this.deleteButton.setManaged(false);

        this.currentPlan = mealPlanService.findPlanForWeek(user.getId(), currentWeekStart);

        if (currentPlan == null) {
            Label noPlanLabel = new Label("🕸️  No plan for this week  🕸️");
            noPlanLabel.getStyleClass().add("no-plan-label");
            this.mealPlanGrid.getChildren().add(noPlanLabel);
            this.shoppingListController.refresh(new HashMap<>());
            this.createButton.setVisible(true);
            this.createButton.setManaged(true);
            return;
        }

        List<WeeklyMealViewModel> weeklyMeals = this.mealPlanService.getWeeklyMeals(this.user.getId(), this.currentWeekStart);
        List<Integer> mealIds = weeklyMeals.stream().map(WeeklyMealViewModel::mealId).toList();
        Map<Integer, NutritionalEstimate> estimates = this.nutritionService.estimatesForMeals(mealIds);
        Set<Integer> mealsWithNoIngredients = mealIds.stream().filter(id -> !estimates.containsKey(id)).collect(Collectors.toSet());
        Map<LocalDate, Integer> calorieTotals = this.nutritionService.dailyCalorieTotals(weeklyMeals, estimates);

        updateDayLabels();

        Map<LocalDate, Integer> nextRowForDate = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            nextRowForDate.put(currentWeekStart.plusDays(i), 1);
        }
        populateMealCards(builder, weeklyMeals, estimates, nextRowForDate, mealsWithNoIngredients);
        if (this.user.isShowCalories()) {
            appendCalorieTotals(nextRowForDate, calorieTotals);
            int weeklyTotal = calorieTotals.values().stream().reduce(0, Integer::sum);
            this.weeklyCalories.setText("🕯️  Weekly calories: " + weeklyTotal + "  🕯️");
        }

        // + meal buttons
        // the buttons are placed in the max row + 1 so that they will be aligned in the UI
        int row = Collections.max(nextRowForDate.values()) + 1;
        for (Map.Entry<LocalDate, Integer> entry : nextRowForDate.entrySet()) {
            int column = columnFor(entry.getKey());
            Button button = new Button("+");
            button.getStyleClass().add("button-primary");
            button.setOnAction(e ->
                    showAddMealDialog(entry.getKey()));
            mealPlanGrid.add(button, column, row);
        }

        // Buttons!
        this.deleteButton.setVisible(true);
        this.deleteButton.setManaged(true);

        // Shopping List
        Map<String, List<ShoppingItem>> shoppingList = this.shoppingListService.buildList(currentPlan.id());
        this.shoppingListController.refresh(shoppingList);
    }


    private void handleGridRefreshError(SQLException e) {
        log.log(Level.SEVERE, "Failed to refresh meal plan grid", e);
        Alert alert = styledAlert(Alert.AlertType.ERROR, "Refresh Error", "The grid could not be refreshed.\n\n" + e.getMessage());
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
        Alert alert = styledAlert(Alert.AlertType.CONFIRMATION, "Confirm Delete", "Dispel this plan?");
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
