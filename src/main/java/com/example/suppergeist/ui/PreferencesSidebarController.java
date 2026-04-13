package com.example.suppergeist.ui;

import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.UserRepository;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PreferencesSidebarController {

    private UserRepository userRepository;
    private User user;
    private static final Logger log = Logger.getLogger(PreferencesSidebarController.class.getName());
    private Consumer<User> onPreferencesSaved;

    // UI Elements
    @FXML private VBox root;
    @FXML private VBox dietaryConstraintsBox;
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

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setOnPreferencesSaved(Consumer<User> onPreferencesSaved) {
        this.onPreferencesSaved = onPreferencesSaved;
    }

    public void setFormValues(User user) {
        this.user = user;

        ObservableList<Node> dietaryConstraintBoxes = dietaryConstraintsBox.getChildren();
        for (Node node : dietaryConstraintBoxes) {
            CheckBox box = (CheckBox) node;
            String boxString = box.getText().toLowerCase();
            box.setSelected(user.getDietaryConstraints().contains(boxString));
        }

        this.servingsPerMealSpinner.getValueFactory().setValue(user.getServingsPerMeal());

        this.showCaloriesCheckbox.setSelected(user.isShowCalories());
        this.showNutritionalInfoCheckbox.setSelected(user.isShowNutritionalInfo());

        this.weekStartDayChoiceBox.setValue(DayOfWeek.of(user.getWeekStartDay()));
    }

    public void savePreferences() {
        ObservableList<Node> dietaryConstraintBoxes = dietaryConstraintsBox.getChildren();
        Set<String> dietaryConstraints = new HashSet<>();
        for (Node node : dietaryConstraintBoxes) {
            CheckBox box = (CheckBox) node;
            if (box.isSelected()) {
                dietaryConstraints.add(box.getText().toLowerCase());
            }
        }
        
        this.user = new User(
                this.user.getId(),
                this.user.getName(),
                dietaryConstraints,
                this.user.getAvoidFoodCodes(),
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
