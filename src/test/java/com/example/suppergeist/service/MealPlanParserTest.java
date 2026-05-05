package com.example.suppergeist.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealPlanParserTest {

    private final MealPlanParser parser = new MealPlanParser();

    @Test
    void parse_returnsMealsAndIngredientsFromValidJson() throws MealPlanParseException {
        String json = jsonWithMeals(
                """
                        {
                          "name": "Lentil Stew",
                          "mealType": "Dinner",
                          "ingredients": [
                            {
                              "foodCode": "lentils",
                              "name": "Lentils",
                              "quantity": 200,
                              "unit": "g"
                            },
                            {
                              "foodCode": "tomato",
                              "name": "Tomato",
                              "quantity": 1.5,
                              "unit": "cups"
                            }
                          ]
                        }
                        """,
                validMeal("Tuesday Meal", "Dinner"),
                validMeal("Wednesday Meal", "Dinner"),
                validMeal("Thursday Meal", "Dinner"),
                validMeal("Friday Meal", "Dinner"),
                validMeal("Saturday Meal", "Dinner"),
                validMeal("Sunday Meal", "Dinner")
        );

        List<MealPlanParser.ParsedMeal> meals = parser.parse(json);

        assertEquals(7, meals.size());
        MealPlanParser.ParsedMeal meal = meals.getFirst();
        assertEquals("Lentil Stew", meal.name());
        assertEquals("Dinner", meal.mealType());
        assertEquals(2, meal.ingredients().size());

        MealPlanParser.ParsedIngredient firstIngredient = meal.ingredients().getFirst();
        assertEquals("lentils", firstIngredient.foodCode());
        assertEquals("Lentils", firstIngredient.name());
        assertEquals(200.0, firstIngredient.quantity());
        assertEquals("g", firstIngredient.unit());

        MealPlanParser.ParsedIngredient secondIngredient = meal.ingredients().get(1);
        assertEquals("tomato", secondIngredient.foodCode());
        assertEquals("Tomato", secondIngredient.name());
        assertEquals(1.5, secondIngredient.quantity());
        assertEquals("cups", secondIngredient.unit());
    }

    @Test
    void parse_returnsAllMealsInResponseOrder() throws MealPlanParseException {
        String json = jsonWithMeals(
                validMeal("Monday Meal", "Breakfast"),
                validMeal("Tuesday Meal", "Lunch"),
                validMeal("Wednesday Meal", "Dinner"),
                validMeal("Thursday Meal", "Dinner"),
                validMeal("Friday Meal", "Dinner"),
                validMeal("Saturday Meal", "Dinner"),
                validMeal("Sunday Meal", "Dinner")
        );

        List<MealPlanParser.ParsedMeal> meals = parser.parse(json);

        assertEquals(7, meals.size());
        assertEquals("Monday Meal", meals.getFirst().name());
        assertEquals("Breakfast", meals.getFirst().mealType());
        assertEquals("Tuesday Meal", meals.get(1).name());
        assertEquals("Lunch", meals.get(1).mealType());
        assertEquals("Sunday Meal", meals.get(6).name());
    }

    @Test
    void parse_throwsWhenJsonIsMalformed() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse("{ \"meals\": [")
        );

        assertTrue(exception.getMessage().startsWith("Invalid JSON: "));
    }

    @Test
    void parse_throwsWhenMealsFieldIsMissing() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse("{ \"plan\": [] }")
        );

        assertEquals("Failed to parse meal plan response", exception.getMessage());
    }

    @Test
    void parse_throwsWhenMealsListIsEmpty() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse("{ \"meals\": [] }")
        );

        assertEquals("Expected exactly 7 meals but got 0", exception.getMessage());
    }

    @Test
    void parse_throwsWhenInputIsNull() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(null)
        );

        assertEquals("Failed to parse meal plan response", exception.getMessage());
    }

    @Test
    void parse_throwsWhenMealCountIsNotSeven() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(validMeal("Only Meal", "Dinner")))
        );

        assertEquals("Expected exactly 7 meals but got 1", exception.getMessage());
    }

    @Test
    void parse_throwsWhenMealNameIsBlank() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(
                        validMeal("Monday Meal", "Dinner"),
                        validMeal("Tuesday Meal", "Dinner"),
                        validMeal("   ", "Dinner"),
                        validMeal("Thursday Meal", "Dinner"),
                        validMeal("Friday Meal", "Dinner"),
                        validMeal("Saturday Meal", "Dinner"),
                        validMeal("Sunday Meal", "Dinner")
                ))
        );

        assertEquals("Meal 3 is missing a name", exception.getMessage());
    }

    @Test
    void parse_throwsWhenMealTypeIsMissing() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(
                        validMeal("Monday Meal", "Dinner"),
                        """
                                {
                                  "name": "Tuesday Meal",
                                  "ingredients": [
                                    {
                                      "foodCode": "rice",
                                      "name": "Rice",
                                      "quantity": 100,
                                      "unit": "g"
                                    }
                                  ]
                                }
                                """,
                        validMeal("Wednesday Meal", "Dinner"),
                        validMeal("Thursday Meal", "Dinner"),
                        validMeal("Friday Meal", "Dinner"),
                        validMeal("Saturday Meal", "Dinner"),
                        validMeal("Sunday Meal", "Dinner")
                ))
        );

        assertEquals("Meal 2 is missing a mealType", exception.getMessage());
    }

    @Test
    void parse_throwsWhenIngredientsFieldIsMissing() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(
                        validMeal("Monday Meal", "Dinner"),
                        validMeal("Tuesday Meal", "Dinner"),
                        """
                                {
                                  "name": "Wednesday Meal",
                                  "mealType": "Dinner"
                                }
                                """,
                        validMeal("Thursday Meal", "Dinner"),
                        validMeal("Friday Meal", "Dinner"),
                        validMeal("Saturday Meal", "Dinner"),
                        validMeal("Sunday Meal", "Dinner")
                ))
        );

        assertEquals("Meal 3 is missing ingredients", exception.getMessage());
    }

    @Test
    void parse_throwsWhenIngredientNameIsBlank() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(
                        validMeal("Monday Meal", "Dinner"),
                        """
                                {
                                  "name": "Tuesday Meal",
                                  "mealType": "Dinner",
                                  "ingredients": [
                                    {
                                      "foodCode": "rice",
                                      "name": "Rice",
                                      "quantity": 100,
                                      "unit": "g"
                                    },
                                    {
                                      "foodCode": "blank",
                                      "name": "",
                                      "quantity": 50,
                                      "unit": "g"
                                    }
                                  ]
                                }
                                """,
                        validMeal("Wednesday Meal", "Dinner"),
                        validMeal("Thursday Meal", "Dinner"),
                        validMeal("Friday Meal", "Dinner"),
                        validMeal("Saturday Meal", "Dinner"),
                        validMeal("Sunday Meal", "Dinner")
                ))
        );

        assertEquals("Meal 2 ingredient 2 is missing a name", exception.getMessage());
    }

    @Test
    void parse_throwsWhenIngredientFoodCodeIsMissing() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(
                        validMeal("Monday Meal", "Dinner"),
                        """
                                {
                                  "name": "Tuesday Meal",
                                  "mealType": "Dinner",
                                  "ingredients": [
                                    {
                                      "name": "Rice",
                                      "quantity": 100,
                                      "unit": "g"
                                    }
                                  ]
                                }
                                """,
                        validMeal("Wednesday Meal", "Dinner"),
                        validMeal("Thursday Meal", "Dinner"),
                        validMeal("Friday Meal", "Dinner"),
                        validMeal("Saturday Meal", "Dinner"),
                        validMeal("Sunday Meal", "Dinner")
                ))
        );

        assertEquals("Meal 2 ingredient 1 is missing a foodCode", exception.getMessage());
    }

    @Test
    void parse_throwsWhenIngredientQuantityIsZeroOrMissing() {
        MealPlanParseException exception = assertThrows(
                MealPlanParseException.class,
                () -> parser.parse(jsonWithMeals(
                        validMeal("Monday Meal", "Dinner"),
                        validMeal("Tuesday Meal", "Dinner"),
                        validMeal("Wednesday Meal", "Dinner"),
                        """
                                {
                                  "name": "Thursday Meal",
                                  "mealType": "Dinner",
                                  "ingredients": [
                                    {
                                      "foodCode": "rice",
                                      "name": "Rice",
                                      "unit": "g"
                                    }
                                  ]
                                }
                                """,
                        validMeal("Friday Meal", "Dinner"),
                        validMeal("Saturday Meal", "Dinner"),
                        validMeal("Sunday Meal", "Dinner")
                ))
        );

        assertEquals("Meal 4 ingredient 1 must have a positive quantity", exception.getMessage());
    }

    @Test
    void parse_allowsNullOrBlankIngredientUnits() throws MealPlanParseException {
        String json = jsonWithMeals(
                """
                        {
                          "name": "Monday Meal",
                          "mealType": "Dinner",
                          "ingredients": [
                            {
                              "foodCode": "egg",
                              "name": "Egg",
                              "quantity": 2,
                              "unit": null
                            },
                            {
                              "foodCode": "salt",
                              "name": "Salt",
                              "quantity": 1,
                              "unit": ""
                            }
                          ]
                        }
                        """,
                validMeal("Tuesday Meal", "Dinner"),
                validMeal("Wednesday Meal", "Dinner"),
                validMeal("Thursday Meal", "Dinner"),
                validMeal("Friday Meal", "Dinner"),
                validMeal("Saturday Meal", "Dinner"),
                validMeal("Sunday Meal", "Dinner")
        );

        List<MealPlanParser.ParsedMeal> meals = parser.parse(json);

        assertEquals(7, meals.size());
        assertNull(meals.getFirst().ingredients().getFirst().unit());
        assertEquals("", meals.getFirst().ingredients().get(1).unit());
    }

    private String jsonWithMeals(String... meals) {
        return """
                {
                  "meals": [
                """ +
                String.join(",\n", meals) +
                """
                
                  ]
                }
                """;
    }

    private String validMeal(String name, String mealType) {
        return """
                {
                  "name": "%s",
                  "mealType": "%s",
                  "ingredients": [
                    {
                      "foodCode": "rice",
                      "name": "Rice",
                      "quantity": 100,
                      "unit": "g"
                    }
                  ]
                }
                """.formatted(name, mealType);
    }
}
