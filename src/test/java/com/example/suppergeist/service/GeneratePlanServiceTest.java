package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.MealPlan;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratePlanServiceTest {

    private final User user = new User(42, "Test User", Set.of("vegetarian"), Set.of("avoid-rice"), 2, true, true);
    private final LocalDate weekStart = LocalDate.of(2026, 5, 4);
    private final List<Ingredient> ingredients = List.of(
            new Ingredient(10, "Lentils", "lentils"),
            new Ingredient(11, "Tomato", "tomato"),
            new Ingredient(12, "Rice", "avoid-rice")
    );

    @Test
    void generateAndSave_deletesExistingPlanAndPersistsGeneratedMealsAndMatchingIngredients() throws Exception {
        FakeIngredientRepository ingredientRepository = new FakeIngredientRepository(ingredients);
        FakeOllamaClient client = new FakeOllamaClient(validResponse());
        FakeMealPlanService mealPlanService = new FakeMealPlanService(new MealPlan(99, user.getId(), weekStart));
        FakeMealIngredientService mealIngredientService = new FakeMealIngredientService();
        GeneratePlanService service = new GeneratePlanService(
                ingredientRepository,
                client,
                mealPlanService,
                mealIngredientService
        );

        MealPlan result = service.generateAndSave(user, weekStart);

        assertSame(mealPlanService.createdPlan, result);
        assertTrue(ingredientRepository.getAllIngredientsCalled);
        assertTrue(client.prompt.contains("Dietary constraints: [vegetarian]"));
        assertTrue(client.prompt.contains("Avoid foods: Rice"));
        assertTrue(client.prompt.contains("{\"foodCode\":\"lentils\",\"name\":\"Lentils\"}"));
        assertTrue(client.prompt.contains("{\"foodCode\":\"tomato\",\"name\":\"Tomato\"}"));
        assertFalse(client.prompt.contains("{\"foodCode\":\"avoid-rice\",\"name\":\"Rice\"}"));

        assertEquals(List.of(99), mealPlanService.deletedPlanIds);
        assertEquals(user.getId(), mealPlanService.createdUserId);
        assertEquals(weekStart, mealPlanService.createdWeekStart);

        assertEquals(7, mealPlanService.addedMeals.size());
        assertEquals(new AddedMeal("Monday Stew", "Dinner", mealPlanService.createdPlan.id(), 0), mealPlanService.addedMeals.getFirst());
        assertEquals(new AddedMeal("Sunday Stew", "Dinner", mealPlanService.createdPlan.id(), 6), mealPlanService.addedMeals.get(6));

        assertEquals(8, mealIngredientService.addedIngredients.size());
        assertTrue(mealIngredientService.addedIngredients.contains(new AddedIngredient(1000, 10, 200.0, "g")));
        assertTrue(mealIngredientService.addedIngredients.contains(new AddedIngredient(1000, 11, 1.0, "cup")));
        assertFalse(mealIngredientService.addedIngredients.stream()
                .anyMatch(ingredient -> ingredient.ingredientId() == 12));
    }

    @Test
    void generateAndSave_doesNotDeletePlan_whenNoExistingPlanExists() throws Exception {
        FakeMealPlanService mealPlanService = new FakeMealPlanService(null);
        GeneratePlanService service = new GeneratePlanService(
                new FakeIngredientRepository(ingredients),
                new FakeOllamaClient(validResponse()),
                mealPlanService,
                new FakeMealIngredientService()
        );

        service.generateAndSave(user, weekStart);

        assertTrue(mealPlanService.deletedPlanIds.isEmpty());
        assertEquals(7, mealPlanService.addedMeals.size());
    }

    @Test
    void generateAndSave_throwsParseExceptionAndDoesNotReplacePlan_whenGeneratedResponseIsInvalid() {
        FakeMealPlanService mealPlanService = new FakeMealPlanService(new MealPlan(99, user.getId(), weekStart));
        GeneratePlanService service = new GeneratePlanService(
                new FakeIngredientRepository(ingredients),
                new FakeOllamaClient("{ \"meals\": [] }"),
                mealPlanService,
                new FakeMealIngredientService()
        );

        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> service.generateAndSave(user, weekStart)
        );

        assertEquals("Expected exactly 7 meals but got 0", exception.getMessage());
        assertTrue(mealPlanService.deletedPlanIds.isEmpty());
        assertFalse(mealPlanService.createEmptyPlanCalled);
        assertTrue(mealPlanService.addedMeals.isEmpty());
    }

    @Test
    void generateAndSave_propagatesClientIOExceptionAndDoesNotReplacePlan() {
        FakeMealPlanService mealPlanService = new FakeMealPlanService(new MealPlan(99, user.getId(), weekStart));
        FakeOllamaClient client = new FakeOllamaClient(new IOException("ollama unavailable"));
        GeneratePlanService service = new GeneratePlanService(
                new FakeIngredientRepository(ingredients),
                client,
                mealPlanService,
                new FakeMealIngredientService()
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> service.generateAndSave(user, weekStart)
        );

        assertEquals("ollama unavailable", exception.getMessage());
        assertTrue(mealPlanService.deletedPlanIds.isEmpty());
        assertFalse(mealPlanService.createEmptyPlanCalled);
        assertTrue(mealPlanService.addedMeals.isEmpty());
    }

    private static String validResponse() {
        return """
                {
                  "meals": [
                    {
                      "name": "Monday Stew",
                      "mealType": "Dinner",
                      "ingredients": [
                        { "foodCode": "lentils", "name": "Different Display Name", "quantity": 200, "unit": "g" },
                        { "foodCode": "tomato", "name": "Tomatoes", "quantity": 1, "unit": "cup" },
                        { "foodCode": "unknown-code", "name": "Ingredient Not In Repository", "quantity": 1, "unit": "piece" },
                        { "foodCode": "avoid-rice", "name": "Rice", "quantity": 100, "unit": "g" }
                      ]
                    },
                    { "name": "Tuesday Stew", "mealType": "Dinner", "ingredients": [{ "foodCode": "lentils", "name": "Lentils", "quantity": 150, "unit": "g" }] },
                    { "name": "Wednesday Stew", "mealType": "Dinner", "ingredients": [{ "foodCode": "lentils", "name": "Lentils", "quantity": 150, "unit": "g" }] },
                    { "name": "Thursday Stew", "mealType": "Dinner", "ingredients": [{ "foodCode": "lentils", "name": "Lentils", "quantity": 150, "unit": "g" }] },
                    { "name": "Friday Stew", "mealType": "Dinner", "ingredients": [{ "foodCode": "lentils", "name": "Lentils", "quantity": 150, "unit": "g" }] },
                    { "name": "Saturday Stew", "mealType": "Dinner", "ingredients": [{ "foodCode": "lentils", "name": "Lentils", "quantity": 150, "unit": "g" }] },
                    { "name": "Sunday Stew", "mealType": "Dinner", "ingredients": [{ "foodCode": "lentils", "name": "Lentils", "quantity": 150, "unit": "g" }] }
                  ]
                }
                """;
    }

    private static final class FakeIngredientRepository extends IngredientRepository {
        private final List<Ingredient> ingredients;
        private boolean getAllIngredientsCalled;

        private FakeIngredientRepository(List<Ingredient> ingredients) {
            super(null);
            this.ingredients = ingredients;
        }

        @Override
        public List<Ingredient> getAllIngredients() {
            getAllIngredientsCalled = true;
            return ingredients;
        }
    }

    private static final class FakeOllamaClient extends OllamaClient {
        private final String response;
        private final IOException exception;
        private String prompt;

        private FakeOllamaClient(String response) {
            super("test-model");
            this.response = response;
            this.exception = null;
        }

        private FakeOllamaClient(IOException exception) {
            super("test-model");
            this.response = null;
            this.exception = exception;
        }

        @Override
        public String generate(String prompt) throws IOException {
            this.prompt = prompt;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }

    private static final class FakeMealPlanService extends MealPlanService {
        private final MealPlan existingPlan;
        private final MealPlan createdPlan = new MealPlan(200, 42, LocalDate.of(2026, 5, 4));
        private final List<Integer> deletedPlanIds = new ArrayList<>();
        private final List<AddedMeal> addedMeals = new ArrayList<>();
        private boolean createEmptyPlanCalled;
        private int createdUserId;
        private LocalDate createdWeekStart;

        private FakeMealPlanService(MealPlan existingPlan) {
            super(null, null);
            this.existingPlan = existingPlan;
        }

        @Override
        public MealPlan findPlanForWeek(int userId, LocalDate startDate) {
            return existingPlan;
        }

        @Override
        public void deletePlan(int id) {
            deletedPlanIds.add(id);
        }

        @Override
        public MealPlan createEmptyPlan(int userId, LocalDate startDate) {
            createEmptyPlanCalled = true;
            createdUserId = userId;
            createdWeekStart = startDate;
            return createdPlan;
        }

        @Override
        public int addMealToSlot(String mealName, String mealType, int mealPlanId, int dayOffset) {
            addedMeals.add(new AddedMeal(mealName, mealType, mealPlanId, dayOffset));
            return 1000 + dayOffset;
        }
    }

    private static final class FakeMealIngredientService extends MealIngredientService {
        private final List<AddedIngredient> addedIngredients = new ArrayList<>();

        private FakeMealIngredientService() {
            super(null, null);
        }

        @Override
        public int addIngredientToMeal(int mealId, int ingredientId, double quantity, String unit) throws SQLException {
            addedIngredients.add(new AddedIngredient(mealId, ingredientId, quantity, unit));
            return addedIngredients.size();
        }
    }

    private record AddedMeal(String name, String mealType, int mealPlanId, int dayOffset) {
    }

    private record AddedIngredient(int mealId, int ingredientId, double quantity, String unit) {
    }
}
