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
import lombok.Setter;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
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

    @FXML
    public void initialize() {
        // Avoid Food Codes Search Listener
        this.avoidFoodCodesSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            this.filteredIngredients.setPredicate(ingredient ->
                    newValue == null || newValue.isEmpty() || ingredient.getName().toLowerCase().contains(newValue.toLowerCase())
            );
        });

        // Spinner: servings per meal
        this.servingsPerMealSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2)
        );

    }

    public void toggleVisibility() {
        this.root.setVisible(!this.root.isVisible());
        this.root.setManaged(this.root.isVisible());
    }

    public void setFormValues(User user) throws SQLException {
        this.user = user;

        // ---------- Dietary Constraints ----------
        this.vegetarianCheckbox.setSelected(user.getDietaryConstraints().contains("vegetarian"));
        this.veganCheckbox.setSelected(user.getDietaryConstraints().contains("vegan"));
        this.glutenFreeCheckbox.setSelected(user.getDietaryConstraints().contains("gluten-free"));
        this.dairyFreeCheckbox.setSelected(user.getDietaryConstraints().contains("dairy-free"));

        // ---------- Avoid Food Codes ----------
        // Load all ingredients into an ObservableList once. The ListView doesn't use this directly —
        // instead a FilteredList wraps it and acts as a live window, showing only items that match
        // the current search text. The predicate starts as always-true (show everything) and is
        // swapped on each search keystroke. The ObservableList itself never changes.
        List<Ingredient> allIngredients = this.userPreferencesService.getAllIngredients();
        ObservableList<Ingredient> avoidFoodCodes = FXCollections.observableArrayList();
        avoidFoodCodes.addAll(allIngredients);
        this.filteredIngredients = new FilteredList<>(avoidFoodCodes, ingredient -> true);


        this.avoidFoodCodesListView.setItems(this.filteredIngredients);
        this.avoidFoodCodesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        for (Ingredient ingredient : allIngredients) {
            if (ingredient.getFoodCode() != null && user.getAvoidFoodCodes().contains(ingredient.getFoodCode())) {
                this.avoidFoodCodesListView.getSelectionModel().select(ingredient);
            }
        }
        // ------------------------------------------

        this.servingsPerMealSpinner.getValueFactory().setValue(user.getServingsPerMeal());

        this.showCaloriesCheckbox.setSelected(user.isShowCalories());
        this.showNutritionalInfoCheckbox.setSelected(user.isShowNutritionalInfo());
    }

    public void savePreferences() {

        // ---------- Dietary Constraints ----------
        Set<String> dietaryConstraints = new HashSet<>();

        if (this.vegetarianCheckbox.isSelected()) {
            dietaryConstraints.add("vegetarian");
        }

        if (this.veganCheckbox.isSelected()) {
            dietaryConstraints.add("vegan");
        }

        if (this.glutenFreeCheckbox.isSelected()) {
            dietaryConstraints.add("gluten-free");
        }

        if (this.dairyFreeCheckbox.isSelected()) {
            dietaryConstraints.add("dairy-free");
        }

        // ---------- Avoid Food Codes ----------
        List<Ingredient> selectedIngredients = this.avoidFoodCodesListView.getSelectionModel().getSelectedItems();
        Set<String> selectedFoodCodes = new HashSet<>();
        for (Ingredient ingredient : selectedIngredients) {
            selectedFoodCodes.add(ingredient.getFoodCode());
        }

        try {
            this.user = new User(
                    this.user.getId(),
                    this.user.getName(),
                    dietaryConstraints,
                    selectedFoodCodes,
                    this.servingsPerMealSpinner.getValue(),
                    this.showCaloriesCheckbox.isSelected(),
                    this.showNutritionalInfoCheckbox.isSelected()
            );
            this.userPreferencesService.savePreferences(this.user);
            if (this.onPreferencesSaved != null) {
                this.onPreferencesSaved.accept(this.user);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save user preferences", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save preferences");
            alert.setContentText(e.getMessage());
            alert.setGraphic(null);
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/com/example/suppergeist/style.css").toExternalForm()
            );
            alert.showAndWait();
        }
    }
}
