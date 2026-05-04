package com.example.suppergeist.service;

/**
 * Service for loading and preparing user preferences.
 * Currently delegates to repositories directly, but will contain logic to assemble
 * prompt-ready preference data for Ollama integration — applying dietary constraints,
 * building ingredient exclusion lists, and shaping the User record into a form
 * PromptBuilder can consume without touching repositories itself.
 */

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class UserPreferencesService {
    private static final Logger log = Logger.getLogger(UserPreferencesService.class.getName());

    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;

    public UserPreferencesService(UserRepository userRepository, IngredientRepository ingredientRepository) {
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public User loadUser(int userId) throws SQLException {
        User user = userRepository.getUser(userId);
        log.info(() -> "Loaded user preferences for user " + userId);
        return user;
    }

    public void savePreferences(User user) throws SQLException {
        this.userRepository.savePreferences(user);
        log.info(() -> "Saved preferences for user " + user.getId()
                + " with " + user.getDietaryConstraints().size() + " dietary constraints and "
                + user.getAvoidFoodCodes().size() + " avoided food codes");
    }

    public List<Ingredient> getAllIngredients() throws SQLException {
        return ingredientRepository.getAllIngredients();
    }
}
