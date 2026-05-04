package com.example.suppergeist;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.*;
import com.example.suppergeist.service.*;
import com.example.suppergeist.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
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
    private MealIngredientService mealIngredientService;
    private ShoppingListService shoppingListService;
    private NutritionService nutritionService;
    private GeneratePlanService generatePlanService;
    private Exception initError;
    private static final Logger log = Logger.getLogger(SuppergeistApplication.class.getName());
    private static final String LLM_MODEL = "qwen2.5:7b";

    private void showFatalError(Exception error) {
        log.log(Level.SEVERE, "Application startup failed", error);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Startup Error");
        alert.setHeaderText("Application startup failed");
        alert.setContentText("Suppergeist could not start.\n\n" + error.getMessage());
        alert.setGraphic(null);
        alert.getDialogPane().getStylesheets().add(
                SuppergeistApplication.class.getResource("style.css").toExternalForm()
        );
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
            MealRepository mealRepository = new MealRepository(dbManager);
            MealPlanRepository mealPlanRepository = new MealPlanRepository(dbManager);
            MealRepository mealPlanEntryRepository = new MealRepository(dbManager);
            MealIngredientRepository mealIngredientRepository = new MealIngredientRepository(dbManager);

            this.userPreferencesService = new UserPreferencesService(userRepository, ingredientRepository);
            this.mealPlanService = new MealPlanService(mealRepository, mealPlanRepository);
            this.mealIngredientService = new MealIngredientService(mealIngredientRepository, ingredientRepository);
            this.shoppingListService = new ShoppingListService(mealPlanEntryRepository, mealIngredientRepository);
            this.nutritionService = new NutritionService(mealIngredientRepository);
            OllamaClient ollamaClient = new OllamaClient(LLM_MODEL);
            this.generatePlanService = new GeneratePlanService(ingredientRepository, ollamaClient, mealPlanService, mealIngredientService);
        } catch (SQLException | IOException e) {
            log.log(Level.SEVERE, "Application startup failed", e);
            this.initError = e;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Load fonts early so error dialogs also use the app font
        Font.loadFont(SuppergeistApplication.class.getResourceAsStream("fonts/Kings-Regular.ttf"), 22);
        Font.loadFont(SuppergeistApplication.class.getResourceAsStream("fonts/Quintessential-Regular.ttf"), 22);

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
        controller.setMealIngredientService(mealIngredientService);
        controller.setShoppingListService(shoppingListService);
        controller.setNutritionService(nutritionService);
        controller.setGeneratePlanService(generatePlanService);

        try {
            controller.setup();
        } catch (SQLException e) {
            showFatalError(e);
            return;
        }

        Scene scene = new Scene(fxmlLoader.getRoot(), 1800, 1200);

        // CSS
        var css = SuppergeistApplication.class.getResource("style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Suppergeist");
        stage.setScene(scene);
        stage.show();
    }
}
