package com.example.suppergeist.ui;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import com.example.suppergeist.service.UserPreferencesService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Setter;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PreferencesSidebarController {
    @Setter private UserPreferencesService userPreferencesService;
    private User user;
    private static final Logger log = Logger.getLogger(PreferencesSidebarController.class.getName());
    @Setter private Consumer<User> onPreferencesSaved;
    private FilteredList<Ingredient> filteredIngredients;

    // UI Elements
    @FXML private VBox root;
    @FXML private CheckBox vegetarianCheckbox;
    @FXML private CheckBox veganCheckbox;
    @FXML private CheckBox glutenFreeCheckbox;
    @FXML private CheckBox dairyFreeCheckbox;
    @FXML private TextField avoidFoodCodesSearch;
    @FXML private ListView<Ingredient> avoidFoodCodesListView;
    @FXML private Spinner<Integer> servingsPerMealSpinner;
    @FXML private CheckBox showCaloriesCheckbox;
    @FXML private CheckBox showNutritionalInfoCheckbox;
    @FXML private ChoiceBox<DayOfWeek> weekStartDayChoiceBox;

    @FXML
    public void initialize() {
        // Avoid Food Codes Search Listener
        avoidFoodCodesSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredIngredients.setPredicate(ingredient ->
                    newValue == null || newValue.isEmpty() || ingredient.getName().toLowerCase().contains(newValue.toLowerCase())
            );
        });

        // Spinner: servings per meal
        this.servingsPerMealSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2)
        );

        // ChoiceBox: week start day
        this.weekStartDayChoiceBox.getItems().addAll(DayOfWeek.values());
        weekStartDayChoiceBox.setConverter(new StringConverter<DayOfWeek>() {
            @Override
            public String toString(DayOfWeek day) {
                return day == null ? "" : day.getDisplayName(TextStyle.FULL, Locale.getDefault());
            }

            @Override
            public DayOfWeek fromString(String s) {
                return null;
            }
        });
    }

    public void toggleVisibility() {
        root.setVisible(!root.isVisible());
        root.setManaged(root.isVisible());
    }

    public void setFormValues(User user) throws SQLException {
        this.user = user;

        // ---------- Dietary Constraints ----------
        vegetarianCheckbox.setSelected(user.getDietaryConstraints().contains("vegetarian"));
        veganCheckbox.setSelected(user.getDietaryConstraints().contains("vegan"));
        glutenFreeCheckbox.setSelected(user.getDietaryConstraints().contains("gluten-free"));
        dairyFreeCheckbox.setSelected(user.getDietaryConstraints().contains("dairy-free"));

        // ---------- Avoid Food Codes ----------
        // Load all ingredients into an ObservableList once. The ListView doesn't use this directly —
        // instead a FilteredList wraps it and acts as a live window, showing only items that match
        // the current search text. The predicate starts as always-true (show everything) and is
        // swapped on each search keystroke. The ObservableList itself never changes.
        List<Ingredient> allIngredients = userPreferencesService.getAllIngredients();
        ObservableList<Ingredient> avoidFoodCodes = FXCollections.observableArrayList();
        avoidFoodCodes.addAll(allIngredients);
        this.filteredIngredients = new FilteredList<>(avoidFoodCodes, ingredient -> true);


        avoidFoodCodesListView.setItems(filteredIngredients);
        avoidFoodCodesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        for (Ingredient ingredient : allIngredients) {
            if (user.getAvoidFoodCodes().contains(ingredient.getFoodCode())) {
                avoidFoodCodesListView.getSelectionModel().select(ingredient);
            }
        }
        // ------------------------------------------

        this.servingsPerMealSpinner.getValueFactory().setValue(user.getServingsPerMeal());

        this.showCaloriesCheckbox.setSelected(user.isShowCalories());
        this.showNutritionalInfoCheckbox.setSelected(user.isShowNutritionalInfo());

        this.weekStartDayChoiceBox.setValue(DayOfWeek.of(user.getWeekStartDay()));
    }

    public void savePreferences() {

        // ---------- Dietary Constraints ----------
        Set<String> dietaryConstraints = new HashSet<>();

        if (vegetarianCheckbox.isSelected()) {
            dietaryConstraints.add("vegetarian");
        }

        if (veganCheckbox.isSelected()) {
            dietaryConstraints.add("vegan");
        }

        if (glutenFreeCheckbox.isSelected()) {
            dietaryConstraints.add("gluten-free");
        }

        if (dairyFreeCheckbox.isSelected()) {
            dietaryConstraints.add("dairy-free");
        }

        // ---------- Avoid Food Codes ----------
        List<Ingredient> selectedIngredients = avoidFoodCodesListView.getSelectionModel().getSelectedItems();
        Set<String> selectedFoodCodes = new HashSet<>();
        for (Ingredient ingredient : selectedIngredients) {
            selectedFoodCodes.add(ingredient.getFoodCode());
        }

        this.user = new User(
                this.user.getId(),
                this.user.getName(),
                dietaryConstraints,
                selectedFoodCodes,
                this.servingsPerMealSpinner.getValue(),
                this.showCaloriesCheckbox.isSelected(),
                this.showNutritionalInfoCheckbox.isSelected(),
                this.weekStartDayChoiceBox.getValue().getValue()
        );

        try {
            this.userPreferencesService.savePreferences(this.user);
            if (this.onPreferencesSaved != null) {
                this.onPreferencesSaved.accept(this.user);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save user preferences", e);
            new Alert(Alert.AlertType.ERROR, "Failed to save preferences").showAndWait();
        }
    }
}
