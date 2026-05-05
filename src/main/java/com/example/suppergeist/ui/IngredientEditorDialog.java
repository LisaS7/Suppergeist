package com.example.suppergeist.ui;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.service.MealIngredientService;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IngredientEditorDialog {
    private final MealIngredientService mealIngredientService;
    private final Runnable onChanged;
    private final String stylesheet;

    private static final Logger log = Logger.getLogger(IngredientEditorDialog.class.getName());

    public IngredientEditorDialog(MealIngredientService mealIngredientService, Runnable onChanged, String stylesheet) {
        this.mealIngredientService = mealIngredientService;
        this.onChanged = onChanged;
        this.stylesheet = stylesheet;
    }

    private void handleIngredientEditorError(SQLException e) {
        log.log(Level.SEVERE, "Failed to edit meal ingredients", e);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ingredient Error");
        alert.setContentText("The ingredients could not be updated.\n\n" + e.getMessage());
        alert.setGraphic(null);

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource(stylesheet).toExternalForm()
        );

        alert.showAndWait();
    }

    public void show(int mealId) {
        List<MealIngredientRow> ingredients = null;
        try {
            ingredients = mealIngredientService.getIngredientsForMeal(mealId);
        } catch (SQLException e) {
            handleIngredientEditorError(e);
            return;
        }

        VBox ingredientList = new VBox();
        for (MealIngredientRow ingredient : ingredients) {
            HBox row = buildIngredientRow(ingredientList, ingredient.ingredient().getName(), ingredient.quantity(), ingredient.unit(), () -> {
                try {
                    mealIngredientService.removeIngredientFromMeal(ingredient.id());
                } catch (SQLException ex) {
                    handleIngredientEditorError(ex);
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
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource(stylesheet).toExternalForm()
        );
        dialog.showAndWait();
        onChanged.run();

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
                handleIngredientEditorError(e);
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
                log.warning(() -> "Invalid ingredient quantity entered for meal " + mealId + ": " + quantityField.getText());
                return;
            }

            try {
                log.info(() -> "Adding ingredient " + ingredient.getId() + " to meal " + mealId);
                int mealIngredientId = mealIngredientService.addIngredientToMeal(mealId, ingredient.getId(), quantity, unitField.getText());
                HBox newRow = buildIngredientRow(ingredientList, ingredient.getName(), quantity, unitField.getText(), () -> {
                    try {
                        mealIngredientService.removeIngredientFromMeal(mealIngredientId);
                    } catch (SQLException ex) {
                        handleIngredientEditorError(ex);
                    }
                });
                ingredientList.getChildren().add(newRow);
                ingredientBox.setValue(null);
                ingredientBox.getEditor().clear();
                quantityField.clear();
                unitField.clear();
            } catch (SQLException ex) {
                handleIngredientEditorError(ex);
            }
        });

        HBox addRow = new HBox();
        addRow.getStyleClass().add("ingredient-row");
        addRow.getChildren().addAll(ingredientBox, quantityField, unitField, addButton);
        return addRow;
    }
}
