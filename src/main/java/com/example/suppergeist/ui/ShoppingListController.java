package com.example.suppergeist.ui;

import com.example.suppergeist.model.ShoppingItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ShoppingListController {
    @FXML private VBox shoppingListBox;
    @FXML private Button copyListButton;

    @FXML
    private void copyToClipboard() {
    }


    private String deriveCategory(String foodCode) {
        if (foodCode == null) {
            return "General";
        }

        String prefix = foodCode.split("-")[0];
        return switch (prefix) {
            case "11" -> "Bakery & Grains";
            case "12" -> "Dairy & Eggs";
            case "13" -> "Vegetables & Beans";
            case "14" -> "Fruit & Nuts";
            case "18", "19" -> "Meat";
            case "17", "50" -> "Food Cupboard";
            default -> "General";
        };
    }

    private String formatQuantity(double quantity) {
        if (quantity == (int) quantity) {
            return String.valueOf((int) quantity);
        }
        return String.valueOf(quantity);
    }

    public void refresh(List<ShoppingItem> shoppingList) {
        HashMap<String, ArrayList<ShoppingItem>> groupedItems = new HashMap<>();
        for (ShoppingItem item : shoppingList) {
            String category = deriveCategory(item.foodCode());
            groupedItems.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
        }

        shoppingListBox.getChildren().clear();

        List<String> categories = groupedItems.keySet().stream().sorted().toList();
        for (String category : categories) {
            Label header = new Label(category);
            shoppingListBox.getChildren().add(header);

            List<ShoppingItem> sortedItems = groupedItems.get(category).stream().sorted(Comparator.comparing(ShoppingItem::name)).toList();
            for (ShoppingItem item : sortedItems) {
                CheckBox box = new CheckBox(item.name() + " - " + formatQuantity(item.totalQuantity()) + " " + item.unit());
                shoppingListBox.getChildren().add(box);
            }
        }
    }
}
