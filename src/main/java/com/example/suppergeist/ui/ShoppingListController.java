package com.example.suppergeist.ui;

import com.example.suppergeist.model.ShoppingItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.*;

public class ShoppingListController {
    private LinkedHashMap<String, List<ShoppingItem>> categorisedList;

    // UI
    @FXML private VBox shoppingListBox;
    @FXML private Button copyListButton;

    @FXML
    private void copyToClipboard() {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> categories = this.categorisedList.keySet().stream().toList();
        for (String category : categories) {
            stringBuilder.append(category).append("\n");
            for (ShoppingItem item : this.categorisedList.get(category)) {
                String itemString = item.name() + " - " + formatQuantity(item.totalQuantity()) + " " + item.unit();
                stringBuilder.append(itemString).append("\n");
            }
            stringBuilder.append("\n");
        }
        String text = stringBuilder.toString();

        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String formatQuantity(double quantity) {
        if (quantity == (int) quantity) {
            return String.valueOf((int) quantity);
        }
        return String.valueOf(quantity);
    }

    public void refresh(LinkedHashMap<String, List<ShoppingItem>> shoppingList) {
        this.categorisedList = shoppingList;
        shoppingListBox.getChildren().clear();

        for (String category : this.categorisedList.keySet()) {
            Label header = new Label(category);
            shoppingListBox.getChildren().add(header);

            List<ShoppingItem> sortedItems = this.categorisedList.get(category).stream().sorted(Comparator.comparing(ShoppingItem::name)).toList();
            for (ShoppingItem item : sortedItems) {
                CheckBox box = new CheckBox(item.name() + " - " + formatQuantity(item.totalQuantity()) + " " + item.unit());
                shoppingListBox.getChildren().add(box);
            }
        }
    }
}
