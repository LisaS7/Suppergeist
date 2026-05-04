package com.example.suppergeist.ui;

import com.example.suppergeist.model.NutritionalEstimate;
import com.example.suppergeist.model.User;
import com.example.suppergeist.service.WeeklyMealViewModel;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class MealCardBuilder {
    private final User user;

    public MealCardBuilder(User user) {
        this.user = user;
    }

    public StackPane buildMealCard(WeeklyMealViewModel meal, NutritionalEstimate estimate, String toolTipText, Runnable onEditMeal, Runnable onEditIngredient, Runnable onRemove) {
        // FRONT
        VBox front = new VBox();
        front.getStyleClass().add("meal-card");
        front.getStyleClass().add("meal-card-" + meal.mealType().toLowerCase());
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
            Label tooltipLabel = new Label("☁");
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
        DropShadow normalShadow = new DropShadow(8, 2, 2, Color.web("#1d3a49", 0.15));
        DropShadow pressedShadow = new DropShadow(3, 1, 1, Color.web("#1d3a49", 0.08));
        front.setEffect(normalShadow);
        front.setOnMousePressed(e -> { front.setEffect(pressedShadow); front.setTranslateX(1); front.setTranslateY(1); });
        front.setOnMouseReleased(e -> { front.setEffect(normalShadow); front.setTranslateX(0); front.setTranslateY(0); });
        SimpleBooleanProperty flipped = new SimpleBooleanProperty(false);
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) flipCard(card, front, back, flipped);
        });

        // Right Click Menu
        MenuItem editMealMenu = new MenuItem("Rename");
        MenuItem editIngredientMenu = new MenuItem("Alter Ingredients");
        MenuItem removeMenu = new MenuItem("Banish");
        editMealMenu.setOnAction(e -> onEditMeal.run());
        editIngredientMenu.setOnAction(e -> onEditIngredient.run());
        removeMenu.setOnAction(e -> onRemove.run());
        ContextMenu menu = new ContextMenu(editMealMenu, editIngredientMenu, removeMenu);
        card.setOnContextMenuRequested(e -> menu.show(card, e.getScreenX(), e.getScreenY()));
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
}
