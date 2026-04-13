package com.example.suppergeist.repository;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private Path tempDb;
    private DatabaseManager dbManager;
    private UserRepository repository;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDb = Files.createTempFile("suppergeist-test-", ".db");
        dbManager = new DatabaseManager(tempDb);
        dbManager.init();
        repository = new UserRepository(dbManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    @Test
    void ensureDefaultUserExists_createsUserWhenMissing() throws SQLException {
        repository.ensureDefaultUserExists();
        User user = repository.getUser(1);
        assertEquals("Default User", user.getName());
    }

    @Test
    void ensureDefaultUserExists_isIdempotent() throws SQLException {
        repository.ensureDefaultUserExists();
        assertDoesNotThrow(() -> repository.ensureDefaultUserExists());
    }

    @Test
    void getUser_returnsUserFromDatabase() throws SQLException {
        repository.ensureDefaultUserExists();
        User user = repository.getUser(1);
        assertEquals(1, user.getId());
        assertEquals("Default User", user.getName());
        assertEquals(2, user.getServingsPerMeal());
    }

    @Test
    void getUser_returnsSyntheticDefaultWhenNotFound() throws SQLException {
        User user = repository.getUser(999);
        assertEquals(999, user.getId());
        assertEquals("Default User", user.getName());
    }

    @Test
    void getUser_parsesDietaryConstraints() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of("vegetarian", "gluten-free"), Set.of(), 2, true, true, 1));

        User loaded = repository.getUser(1);
        assertEquals(Set.of("vegetarian", "gluten-free"), loaded.getDietaryConstraints());
    }

    @Test
    void getUser_parsesAvoidFoodCodes() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of(), Set.of("A001", "B002"), 2, true, true, 1));

        User loaded = repository.getUser(1);
        assertEquals(Set.of("A001", "B002"), loaded.getAvoidFoodCodes());
    }

    @Test
    void getUser_handlesBlankConstraints() throws SQLException {
        repository.ensureDefaultUserExists();
        User loaded = repository.getUser(1);
        assertTrue(loaded.getDietaryConstraints().isEmpty());
        assertTrue(loaded.getAvoidFoodCodes().isEmpty());
    }

    @Test
    void savePreferences_persistsAllFields() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of("vegan"), Set.of("A001"), 4, false, false, 7));

        User loaded = repository.getUser(1);
        assertEquals(Set.of("vegan"), loaded.getDietaryConstraints());
        assertEquals(Set.of("A001"), loaded.getAvoidFoodCodes());
        assertEquals(4, loaded.getServingsPerMeal());
        assertFalse(loaded.isShowCalories());
        assertFalse(loaded.isShowNutritionalInfo());
        assertEquals(7, loaded.getWeekStartDay());
    }

    @Test
    void getUser_parsesAllCheckboxDietaryConstraints() throws SQLException {
        repository.ensureDefaultUserExists();
        Set<String> all = Set.of("vegetarian", "vegan", "gluten-free", "dairy-free");
        repository.savePreferences(new User(1, "Default User", all, Set.of(), 2, true, true, 1));

        User loaded = repository.getUser(1);
        assertEquals(all, loaded.getDietaryConstraints());
    }

    @Test
    void savePreferences_clearsDietaryConstraintsWhenSetIsEmpty() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of("vegetarian", "vegan"), Set.of(), 2, true, true, 1));
        repository.savePreferences(new User(1, "Default User", Set.of(), Set.of(), 2, true, true, 1));

        User loaded = repository.getUser(1);
        assertTrue(loaded.getDietaryConstraints().isEmpty());
    }

    @Test
    void savePreferences_clearsAvoidFoodCodes_whenSetIsEmpty() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of(), Set.of("A001", "B002"), 2, true, true, 1));
        repository.savePreferences(new User(1, "Default User", Set.of(), Set.of(), 2, true, true, 1));

        User loaded = repository.getUser(1);
        assertTrue(loaded.getAvoidFoodCodes().isEmpty());
    }

    @Test
    void savePreferences_overwritesPreviousAvoidFoodCodes() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of(), Set.of("A001", "B002"), 2, true, true, 1));
        repository.savePreferences(new User(1, "Default User", Set.of(), Set.of("C003"), 2, true, true, 1));

        User loaded = repository.getUser(1);
        assertEquals(Set.of("C003"), loaded.getAvoidFoodCodes());
    }

    @Test
    void savePreferences_doesNotThrow_whenUserDoesNotExist() {
        assertDoesNotThrow(() -> repository.savePreferences(
                new User(999, "Ghost", Set.of(), Set.of(), 2, true, true, 1)));
    }

    @Test
    void savePreferences_overwritesPreviousPreferences() throws SQLException {
        repository.ensureDefaultUserExists();
        repository.savePreferences(new User(1, "Default User", Set.of("vegetarian"), Set.of(), 2, true, true, 1));
        repository.savePreferences(new User(1, "Default User", Set.of("vegan"), Set.of(), 3, false, true, 2));

        User loaded = repository.getUser(1);
        assertEquals(Set.of("vegan"), loaded.getDietaryConstraints());
        assertEquals(3, loaded.getServingsPerMeal());
        assertFalse(loaded.isShowCalories());
        assertEquals(2, loaded.getWeekStartDay());
    }
}
