package com.example.suppergeist;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.UserRepository;
import com.example.suppergeist.service.AppSeedService;
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

            AppSeedService appSeedService = new AppSeedService(dbManager);
            appSeedService.seedIfEmpty();

            UserRepository userRepository = new UserRepository(dbManager);
            IngredientRepository ingredientRepository = new IngredientRepository(dbManager);
            userRepository.ensureDefaultUserExists();

            this.userPreferencesService = new UserPreferencesService(userRepository, ingredientRepository);
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
