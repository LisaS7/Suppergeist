package com.example.suppergeist.database;

public final class Schema {
    private Schema() {
    }

    public static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "dietary_constraints TEXT NOT NULL DEFAULT '', " +
                    "avoid_food_codes TEXT NOT NULL DEFAULT '', " +
                    "servings_per_meal INTEGER NOT NULL DEFAULT 2 CHECK (servings_per_meal >= 1), " +
                    "show_calories BOOLEAN DEFAULT true, " +
                    "show_nutritional_info BOOLEAN DEFAULT true " +
                    ");";

    public static final String CREATE_MEALS =
            "CREATE TABLE IF NOT EXISTS meals (" +
                    "id           INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "meal_plan_id INTEGER NOT NULL REFERENCES meal_plans(id) ON DELETE CASCADE, " +
                    "day_offset   INTEGER NOT NULL CHECK (day_offset BETWEEN 0 AND 6), " +
                    "meal_type    TEXT    NOT NULL, " +
                    "name         TEXT    NOT NULL, " +
                    "UNIQUE (meal_plan_id, day_offset, meal_type)" +
                    ");";

    public static final String CREATE_INDEX_MEALS_MEAL_PLAN_ID =
            "CREATE INDEX IF NOT EXISTS idx_meals_meal_plan_id " +
                    "ON meals (meal_plan_id);";

    public static final String CREATE_INGREDIENTS =
            "CREATE TABLE IF NOT EXISTS ingredients (" +
                    "    id              INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "    name            TEXT    NOT NULL UNIQUE, " +
                    "    food_code       TEXT, " +
                    "    energy_kcal     REAL, " +
                    "    protein_g       REAL, " +
                    "    fat_g           REAL, " +
                    "    carbohydrate_g  REAL, " +
                    "    total_sugars_g  REAL, " +
                    "    fibre_g         REAL, " +
                    "    vitamin_a_µg    REAL, " +
                    "    vitamin_c_mg    REAL, " +
                    "    vitamin_d_µg    REAL, " +
                    "    vitamin_e_mg    REAL, " +
                    "    vitamin_b12_µg  REAL, " +
                    "    folate_µg       REAL " +
                    ");";

    public static final String CREATE_INDEX_INGREDIENTS =
            "CREATE INDEX IF NOT EXISTS idx_ingredients_food_code " +
                    "    ON ingredients (food_code);";

    public static final String CREATE_MEAL_INGREDIENTS =
            "CREATE TABLE IF NOT EXISTS meal_ingredients (" +
                    "    id            INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "    meal_id       INTEGER NOT NULL REFERENCES meals(id) ON DELETE CASCADE, " +
                    "    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE, " +
                    "    quantity      REAL    NOT NULL, " +
                    "    unit          TEXT " +
                    ");";

    public static final String CREATE_INDEX_MEAL_INGREDIENTS =
            "CREATE INDEX IF NOT EXISTS idx_meal_ingredients_meal_id " +
                    "    ON meal_ingredients (meal_id);";

    public static final String CREATE_MEAL_PLANS =
            "CREATE TABLE IF NOT EXISTS meal_plans (" +
                    "    id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "    start_date TEXT    NOT NULL " +
                    ");";

    public static final String CREATE_INDEX_MEAL_PLANS_USER_WEEK =
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_meal_plans_user_week " +
                    "    ON meal_plans (user_id, start_date);";

}

