package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.repository.IngredientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class AppSeedServiceTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private AppSeedService seedService;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
        dbManager.init();
        seedService = new AppSeedService(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    @Test
    void seedIfEmpty_insertsIngredientsFromCsv() throws SQLException, IOException {
        seedService.seedIfEmpty();

        IngredientRepository repo = new IngredientRepository(dbManager);
        assertFalse(repo.getAllIngredients().isEmpty());
    }

    @Test
    void seedIfEmpty_isIdempotent() throws SQLException, IOException {
        seedService.seedIfEmpty();
        int countAfterFirst = new IngredientRepository(dbManager).getAllIngredients().size();

        seedService.seedIfEmpty();
        int countAfterSecond = new IngredientRepository(dbManager).getAllIngredients().size();

        assertEquals(countAfterFirst, countAfterSecond);
    }

    @Test
    void seedIfEmpty_skipsWhenIngredientsAlreadyPresent() throws SQLException, IOException {
        seedService.seedIfEmpty();
        int initialCount = new IngredientRepository(dbManager).getAllIngredients().size();

        // A second call must not throw and must not alter the count
        assertDoesNotThrow(() -> seedService.seedIfEmpty());
        assertEquals(initialCount, new IngredientRepository(dbManager).getAllIngredients().size());
    }
}
