package com.example.suppergeist;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class MainController {
    @FXML
    private VBox prefsSidebar;

    @FXML
    private void togglePrefs() {
        prefsSidebar.setVisible(!prefsSidebar.isVisible());
        prefsSidebar.setManaged(prefsSidebar.isVisible());
    }
}
