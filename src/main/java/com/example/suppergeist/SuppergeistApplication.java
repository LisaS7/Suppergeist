package com.example.suppergeist;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.repository.IngredientRepository;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class SuppergeistApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(SuppergeistApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 900);

        var css = SuppergeistApplication.class.getResource("style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        
        stage.setTitle("Suppergeist");
        stage.setScene(scene);
        stage.show();
    }
}
