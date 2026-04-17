package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class AppSeedService {
    private final DatabaseManager dbManager;
    private static final Logger log = Logger.getLogger(AppSeedService.class.getName());
    private static final String CSV_RESOURCE = "/data/ingredient_mapping.csv";

    public AppSeedService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void seedMealPlansIfEmpty() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM meal_plans LIMIT 1");
                 ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    log.info("Meal plans already present, skipping seed.");
                    return;
                }
            }

            // Each entry: meal name → ingredients as [name, quantity, unit]
            List<Map.Entry<String, List<Object[]>>> meals = List.of(
                    Map.entry("Spaghetti Bolognese", List.of(
                            new Object[]{"Spaghetti", 200.0, "g"},
                            new Object[]{"Minced beef", 300.0, "g"},
                            new Object[]{"Tinned tomatoes", 400.0, "g"},
                            new Object[]{"Onions", 100.0, "g"},
                            new Object[]{"Garlic", 10.0, "g"}
                    )),
                    Map.entry("Grilled Salmon with Roasted Vegetables", List.of(
                            new Object[]{"Salmon", 200.0, "g"},
                            new Object[]{"Broccoli", 150.0, "g"},
                            new Object[]{"Olive oil", 15.0, "ml"},
                            new Object[]{"Lemon juice", 20.0, "ml"}
                    )),
                    Map.entry("Chicken Tikka Masala", List.of(
                            new Object[]{"Chicken breast", 300.0, "g"},
                            new Object[]{"Yogurt", 100.0, "g"},
                            new Object[]{"Tinned tomatoes", 400.0, "g"},
                            new Object[]{"Onions", 100.0, "g"},
                            new Object[]{"Garam masala", 10.0, "g"}
                    )),
                    Map.entry("Mushroom Risotto", List.of(
                            new Object[]{"Risotto rice", 200.0, "g"},
                            new Object[]{"Mushrooms", 250.0, "g"},
                            new Object[]{"Parmesan cheese", 50.0, "g"},
                            new Object[]{"Vegetable stock", 500.0, "ml"},
                            new Object[]{"Onions", 100.0, "g"}
                    )),
                    Map.entry("Fish and Chips", List.of(
                            new Object[]{"Cod fillet", 200.0, "g"},
                            new Object[]{"Potatoes", 400.0, "g"},
                            new Object[]{"Plain flour", 100.0, "g"},
                            new Object[]{"Sunflower oil", 50.0, "ml"}
                    )),
                    Map.entry("Beef Tacos", List.of(
                            new Object[]{"Minced beef", 300.0, "g"},
                            new Object[]{"Flour tortillas", 4.0, "unit"},
                            new Object[]{"Cheddar cheese", 80.0, "g"},
                            new Object[]{"Tomatoes", 150.0, "g"},
                            new Object[]{"Soured cream", 60.0, "g"}
                    )),
                    Map.entry("Vegetable Stir-Fry", List.of(
                            new Object[]{"Egg noodles", 200.0, "g"},
                            new Object[]{"Broccoli", 150.0, "g"},
                            new Object[]{"Soy sauce", 30.0, "ml"},
                            new Object[]{"Sesame oil", 15.0, "ml"},
                            new Object[]{"Garlic", 10.0, "g"}
                    ))
            );

            List<Integer> mealIds = new ArrayList<>();
            try (PreparedStatement mealInsert = conn.prepareStatement(
                    "INSERT INTO meals (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement ingredientUpsert = conn.prepareStatement(
                    "INSERT OR IGNORE INTO ingredients (name) VALUES (?)");
                 PreparedStatement ingredientLookup = conn.prepareStatement(
                    "SELECT id FROM ingredients WHERE name = ?");
                 PreparedStatement linkInsert = conn.prepareStatement(
                    "INSERT INTO meal_ingredients (meal_id, ingredient_id, quantity, unit) VALUES (?, ?, ?, ?)")) {

                for (Map.Entry<String, List<Object[]>> meal : meals) {
                    mealInsert.setString(1, meal.getKey());
                    mealInsert.executeUpdate();
                    int mealId;
                    try (ResultSet keys = mealInsert.getGeneratedKeys()) {
                        keys.next();
                        mealId = keys.getInt(1);
                        mealIds.add(mealId);
                    }

                    for (Object[] ing : meal.getValue()) {
                        String ingName = (String) ing[0];
                        double qty = (double) ing[1];
                        String unit = (String) ing[2];

                        ingredientUpsert.setString(1, ingName);
                        ingredientUpsert.executeUpdate();

                        ingredientLookup.setString(1, ingName);
                        int ingredientId;
                        try (ResultSet rs2 = ingredientLookup.executeQuery()) {
                            rs2.next();
                            ingredientId = rs2.getInt(1);
                        }

                        linkInsert.setInt(1, mealId);
                        linkInsert.setInt(2, ingredientId);
                        linkInsert.setDouble(3, qty);
                        linkInsert.setString(4, unit);
                        linkInsert.addBatch();
                    }
                    linkInsert.executeBatch();
                }
            }

            LocalDate thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            for (int weekOffset = 0; weekOffset < 3; weekOffset++) {
                LocalDate weekStart = thisWeekStart.plusWeeks(weekOffset);

                int planId;
                try (PreparedStatement planInsert = conn.prepareStatement(
                        "INSERT INTO meal_plans (user_id, start_date) VALUES (1, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    planInsert.setString(1, weekStart.toString());
                    planInsert.executeUpdate();
                    try (ResultSet keys = planInsert.getGeneratedKeys()) {
                        keys.next();
                        planId = keys.getInt(1);
                    }
                }

                try (PreparedStatement entryInsert = conn.prepareStatement(
                        "INSERT INTO meal_plan_entries (meal_plan_id, meal_id, day_offset, meal_type) VALUES (?, ?, ?, 'dinner')")) {
                    for (int day = 0; day < 7; day++) {
                        entryInsert.setInt(1, planId);
                        entryInsert.setInt(2, mealIds.get(day));
                        entryInsert.setInt(3, day);
                        entryInsert.addBatch();
                    }
                    entryInsert.executeBatch();
                }
            }

            log.info("Seeded 3 weeks of meal plans (starting " + thisWeekStart + ").");
        }
    }

    public void seedIfEmpty() throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM ingredients LIMIT 1")) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.info("Ingredients already present, skipping seed.");
                    return;
                }
            }
            
            try (InputStream inputStream = AppSeedService.class.getResourceAsStream(CSV_RESOURCE)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Could not find resource: " + CSV_RESOURCE);
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                     CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);) {

                    List<String> headers = csvParser.getHeaderNames();
                    if (headers.isEmpty()) {
                        throw new IllegalStateException("CSV is empty: " + CSV_RESOURCE);
                    }

                    log.info("Seeding ingredients from CSV...");
                    String columnList = String.join(", ", headers);
                    String placeholders = String.join(", ", Collections.nCopies(headers.size(), "?"));

                    String sql = "INSERT INTO ingredients (" + columnList + ") VALUES (" + placeholders + ")";
                    try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                        for (CSVRecord record : csvParser) {
                            if (record.size() != headers.size()) {
                                throw new IllegalStateException("CSV row does not match header length. Expected " + headers.size() + " but got " + record.size() + ". Row: " + record);
                            }

                            for (int i = 0; i < headers.size(); i++) {
                                String value = record.get(i).trim();
                                insertStmt.setString(i + 1, value.isEmpty() ? null : value);
                            }
                            insertStmt.addBatch();
                        }
                        int[] results = insertStmt.executeBatch();
                        log.info("Inserted " + results.length + " ingredients.");
                    }
                }
            }
        }
    }
}
