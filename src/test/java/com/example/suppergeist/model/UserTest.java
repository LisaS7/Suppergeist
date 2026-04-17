package com.example.suppergeist.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void constructor_throwsOnNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(1, null, Set.of(), Set.of(), 2, true, true));
    }

    @Test
    void constructor_throwsOnBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(1, "   ", Set.of(), Set.of(), 2, true, true));
    }

    @Test
    void constructor_throwsWhenServingsIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(1, "Alice", Set.of(), Set.of(), 0, true, true));
    }

    @Test
    void constructor_throwsWhenServingsIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(1, "Alice", Set.of(), Set.of(), -1, true, true));
    }

    @Test
    void constructor_treatsNullDietaryConstraintsAsEmpty() {
        User user = new User(1, "Alice", null, null, 2, true, true);
        assertTrue(user.getDietaryConstraints().isEmpty());
        assertTrue(user.getAvoidFoodCodes().isEmpty());
    }

    @Test
    void constructor_makesDefensiveCopyOfConstraints() {
        Set<String> constraints = new HashSet<>();
        constraints.add("vegetarian");
        User user = new User(1, "Alice", constraints, Set.of(), 2, true, true);
        constraints.add("vegan");
        assertFalse(user.getDietaryConstraints().contains("vegan"));
    }

    @Test
    void constructor_storesConstraintsCorrectly() {
        User user = new User(1, "Alice", Set.of("gluten-free"), Set.of("A001"), 3, true, true);
        assertEquals(Set.of("gluten-free"), user.getDietaryConstraints());
        assertEquals(Set.of("A001"), user.getAvoidFoodCodes());
        assertEquals(3, user.getServingsPerMeal());
    }

    @Test
    void constructor_storesShowCaloriesAndNutritionalInfo() {
        User user = new User(1, "Alice", Set.of(), Set.of(), 2, false, false);
        assertFalse(user.isShowCalories());
        assertFalse(user.isShowNutritionalInfo());
    }

    @Test
    void nameOnlyConstructor_usesDefaultsAndNullId() {
        User user = new User("Default User");
        assertNull(user.getId());
        assertEquals("Default User", user.getName());
        assertEquals(2, user.getServingsPerMeal());
        assertTrue(user.isShowCalories());
        assertTrue(user.isShowNutritionalInfo());
        assertTrue(user.getDietaryConstraints().isEmpty());
        assertTrue(user.getAvoidFoodCodes().isEmpty());
    }
}
