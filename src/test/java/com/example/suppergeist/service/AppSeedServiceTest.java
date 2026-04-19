package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;
import com.example.suppergeist.model.Meal;
import com.example.suppergeist.model.MealIngredientRow;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.MealIngredientRepository;
import com.example.suppergeist.repository.MealPlanRepository;
import com.example.suppergeist.repository.MealRepository;
import com.example.suppergeist.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

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
        new UserRepository(dbManager).ensureDefaultUserExists();
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

    // --- seedMealPlansIfEmpty ---

    @Test
    void seedMealPlansIfEmpty_createsMealsAndPlans() throws SQLException, IOException {
        seedService.seedIfEmpty();
        seedService.seedMealPlansIfEmpty();

        List<Meal> meals = new MealRepository(dbManager).getAllMeals();
        assertEquals(7, meals.size());

        LocalDate thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        MealPlanRepository planRepo = new MealPlanRepository(dbManager);
        assertTrue(planRepo.getMealPlanByUserAndStartDate(1, thisWeekStart).isPresent());
        assertTrue(planRepo.getMealPlanByUserAndStartDate(1, thisWeekStart.plusWeeks(1)).isPresent());
        assertTrue(planRepo.getMealPlanByUserAndStartDate(1, thisWeekStart.plusWeeks(2)).isPresent());
    }

    @Test
    void seedMealPlansIfEmpty_ingredientsLinkedToCoFIDRows() throws SQLException, IOException {
        // The test DB is seeded from the CSV (food_code + name only, no kcal columns).
        // We verify that every meal's ingredients matched existing CoFID rows — i.e. have a
        // food_code — rather than silently creating blank inserts.
        seedService.seedIfEmpty();
        seedService.seedMealPlansIfEmpty();

        MealIngredientRepository mealIngRepo = new MealIngredientRepository(dbManager);
        List<Meal> meals = new MealRepository(dbManager).getAllMeals();

        for (Meal meal : meals) {
            List<MealIngredientRow> rows = mealIngRepo.getIngredientsWithNutritionForMeal(meal.getId());
            assertFalse(rows.isEmpty(), "Meal has no linked ingredients: " + meal.getName());

            boolean allHaveFoodCode = rows.stream()
                    .allMatch(r -> r.ingredient().getFoodCode() != null && !r.ingredient().getFoodCode().isBlank());
            assertTrue(allHaveFoodCode, "Some ingredients lack a food_code (not matched to CoFID) for meal: " + meal.getName());
        }
    }

    @Test
    void seedMealPlansIfEmpty_isIdempotent() throws SQLException, IOException {
        seedService.seedIfEmpty();
        seedService.seedMealPlansIfEmpty();
        int mealCountAfterFirst = new MealRepository(dbManager).getAllMeals().size();

        seedService.seedMealPlansIfEmpty();
        int mealCountAfterSecond = new MealRepository(dbManager).getAllMeals().size();

        assertEquals(mealCountAfterFirst, mealCountAfterSecond);
    }
}
