package com.example.suppergeist;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.UserRepository;
import com.example.suppergeist.service.AppSeedService;
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
    private Exception initError;
    private static final Logger log = Logger.getLogger(SuppergeistApplication.class.getName());

    @Override
    public void init() {

        try {
            DatabaseManager dbManager = new DatabaseManager();
            dbManager.init();

            AppSeedService appSeedService = new AppSeedService(dbManager);
            appSeedService.seedIfEmpty();

            UserRepository userRepository = new UserRepository(dbManager);
            userRepository.ensureDefaultUserExists();
        } catch (SQLException | IOException e) {
            log.log(Level.SEVERE, "Application startup failed", e);
            initError = e;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {

        if (initError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Application startup failed");
            alert.setContentText("Suppergeist could not start.\n\n" + initError.getMessage());
            alert.showAndWait();

            Platform.exit();
            return;
        }
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
