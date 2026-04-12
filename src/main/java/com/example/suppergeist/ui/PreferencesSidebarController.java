package com.example.suppergeist.ui;

import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PreferencesSidebarController {

    private UserRepository userRepository;
    private User user;
    private static final Logger log = Logger.getLogger(PreferencesSidebarController.class.getName());

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

    public void loadUser(int userId) throws SQLException {
        this.user = this.userRepository.getUser(userId);

        this.servingsPerMealSpinner.getValueFactory().setValue(this.user.getServingsPerMeal());

        this.showCaloriesCheckbox.setSelected(this.user.isShowCalories());
        this.showNutritionalInfoCheckbox.setSelected(this.user.isShowNutritionalInfo());

        this.weekStartDayChoiceBox.setValue(DayOfWeek.of(this.user.getWeekStartDay()));
    }

    public void savePreferences() {
        this.user = new User(
                this.user.getId(),
                this.user.getName(),
                this.user.getDietaryConstraints(),
                this.user.getAvoidFoodCodes(),
                this.servingsPerMealSpinner.getValue(),
                this.showCaloriesCheckbox.isSelected(),
                this.showNutritionalInfoCheckbox.isSelected(),
                this.weekStartDayChoiceBox.getValue().getValue()
        );
        
        try {
            this.userRepository.savePreferences(this.user);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save user preferences", e);
            new Alert(Alert.AlertType.ERROR, "Failed to save preferences").showAndWait();
        }
    }
}
