package com.example.suppergeist.ui;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.UserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PreferencesSidebarController {

    @Setter private UserRepository userRepository;
    private User user;
    @Setter private IngredientRepository ingredientRepository;
    private static final Logger log = Logger.getLogger(PreferencesSidebarController.class.getName());
    @Setter private Consumer<User> onPreferencesSaved;
    private FilteredList<Ingredient> filteredIngredients;

    // UI Elements
    @FXML private VBox root;
    @FXML private VBox dietaryConstraintsBox;
    @FXML private TextField avoidFoodCodesSearch;
    @FXML private ListView<Ingredient> avoidFoodCodesListView;
    @FXML private Spinner<Integer> servingsPerMealSpinner;
    @FXML private CheckBox showCaloriesCheckbox;
    @FXML private CheckBox showNutritionalInfoCheckbox;
    @FXML private ChoiceBox<DayOfWeek> weekStartDayChoiceBox;

    @FXML
    public void initialize() {
        // Spinner: servings per meal
        this.servingsPerMealSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2)
        );

        // ChoiceBox: week start day
        this.weekStartDayChoiceBox.getItems().addAll(DayOfWeek.values());
    }

    public void toggleVisibility() {
        root.setVisible(!root.isVisible());
        root.setManaged(root.isVisible());
    }

    public void setFormValues(User user) throws SQLException {
        this.user = user;

        // ---------- Dietary Constraints ----------
        ObservableList<Node> dietaryConstraintBoxes = dietaryConstraintsBox.getChildren();
        for (Node node : dietaryConstraintBoxes) {
            CheckBox box = (CheckBox) node;
            String boxString = box.getText().toLowerCase();
            box.setSelected(user.getDietaryConstraints().contains(boxString));
        }

        // ---------- Avoid Food Codes ----------
        // Load all ingredients into an ObservableList once. The ListView doesn't use this directly —
        // instead a FilteredList wraps it and acts as a live window, showing only items that match
        // the current search text. The predicate starts as always-true (show everything) and is
        // swapped on each search keystroke. The ObservableList itself never changes.
        List<Ingredient> allIngredients = ingredientRepository.getAllIngredients();
        ObservableList<Ingredient> avoidFoodCodes = FXCollections.observableArrayList();
        avoidFoodCodes.addAll(allIngredients);
        this.filteredIngredients = new FilteredList<>(avoidFoodCodes, ingredient -> true);
        
        avoidFoodCodesSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredIngredients.setPredicate(ingredient ->
                    newValue == null || newValue.isEmpty() || ingredient.getName().toLowerCase().contains(newValue.toLowerCase())
            );
        });

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
        ObservableList<Node> dietaryConstraintBoxes = dietaryConstraintsBox.getChildren();
        Set<String> dietaryConstraints = new HashSet<>();
        for (Node node : dietaryConstraintBoxes) {
            CheckBox box = (CheckBox) node;
            if (box.isSelected()) {
                dietaryConstraints.add(box.getText().toLowerCase());
            }
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
            this.userRepository.savePreferences(this.user);
            if (this.onPreferencesSaved != null) {
                this.onPreferencesSaved.accept(this.user);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save user preferences", e);
            new Alert(Alert.AlertType.ERROR, "Failed to save preferences").showAndWait();
        }
    }
}
