package com.example.suppergeist.service;

import com.example.suppergeist.model.Ingredient;
import com.example.suppergeist.model.User;
import com.example.suppergeist.repository.IngredientRepository;
import com.example.suppergeist.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;

public class UserPreferencesService {
    private UserRepository userRepository;
    private IngredientRepository ingredientRepository;

    public UserPreferencesService(UserRepository userRepository, IngredientRepository ingredientRepository) {
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public User loadUser(int userId) throws SQLException {
        return userRepository.getUser(userId);
    }

    public void savePreferences(User user) throws SQLException {
        this.userRepository.savePreferences(user);
    }

    public List<Ingredient> getAllIngredients() throws SQLException {
        return ingredientRepository.getAllIngredients();
    }
}
