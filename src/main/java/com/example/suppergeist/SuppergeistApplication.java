package com.example.suppergeist;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.*;
import com.example.suppergeist.service.AppSeedService;
import com.example.suppergeist.service.MealPlanService;
import com.example.suppergeist.service.ShoppingListService;
import com.example.suppergeist.service.UserPreferencesService;
import com.example.suppergeist.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SuppergeistApplication extends Application {
    private UserPreferencesService userPreferencesService;
    private MealPlanService mealPlanService;
    private ShoppingListService shoppingListService;
    private Exception initError;
    private static final Logger log = Logger.getLogger(SuppergeistApplication.class.getName());

    private void showFatalError(Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Startup Error");
        alert.setHeaderText("Application startup failed");
        alert.setContentText("Suppergeist could not start.\n\n" + error.getMessage());
        alert.showAndWait();

        Platform.exit();
    }

    @Override
    public void init() {

        try {
            DatabaseManager dbManager = new DatabaseManager();
            dbManager.init();

            UserRepository userRepository = new UserRepository(dbManager);
            userRepository.ensureDefaultUserExists();

            AppSeedService appSeedService = new AppSeedService(dbManager);
            appSeedService.seedIfEmpty();
            appSeedService.seedMealPlansIfEmpty();

            IngredientRepository ingredientRepository = new IngredientRepository(dbManager);
            MealPlanRepository mealPlanRepository = new MealPlanRepository(dbManager);
            MealPlanEntryRepository mealPlanEntryRepository = new MealPlanEntryRepository(dbManager);
            MealIngredientRepository mealIngredientRepository = new MealIngredientRepository(dbManager);

            this.userPreferencesService = new UserPreferencesService(userRepository, ingredientRepository);
            this.mealPlanService = new MealPlanService(mealPlanRepository, mealPlanEntryRepository);
            this.shoppingListService = new ShoppingListService(mealPlanEntryRepository, mealIngredientRepository);
        } catch (SQLException | IOException e) {
            log.log(Level.SEVERE, "Application startup failed", e);
            this.initError = e;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (initError != null) {
            showFatalError(initError);
            return;
        }

        // FXML
        FXMLLoader fxmlLoader = new FXMLLoader(SuppergeistApplication.class.getResource("main.fxml"));
        fxmlLoader.load();

        // Controller
        MainController controller = fxmlLoader.getController();
        controller.setUserPreferencesService(userPreferencesService);
        controller.setMealPlanService(mealPlanService);
        controller.setShoppingListService(shoppingListService);

        try {
            controller.setup();
        } catch (SQLException e) {
            showFatalError(e);
            return;
        }

        Scene scene = new Scene(fxmlLoader.getRoot(), 1200, 900);

        var css = SuppergeistApplication.class.getResource("style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Suppergeist");
        stage.setScene(scene);
        stage.show();
    }
}
