PRAGMA foreign_keys = ON;

-- ─────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL
);

-- ─────────────────────────────────────────────
-- Ingredients (canonical catalog)
-- food_code is the CoFID 2021 primary key (e.g. "11-002").
-- Null when no nutritional match has been made yet.
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ingredients (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL UNIQUE,
    food_code       TEXT,
    energy_kcal     REAL,
    protein_g       REAL,
    fat_g           REAL,
    carbohydrate_g  REAL
);

CREATE INDEX IF NOT EXISTS idx_ingredients_food_code
    ON ingredients (food_code);

-- ─────────────────────────────────────────────
-- Meals
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meals (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL
);

-- ─────────────────────────────────────────────
-- Meal ingredients
-- One row per ingredient line in a meal.
-- quantity is a numeric amount; unit is the measure (e.g. "g", "ml", "tbsp").
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meal_ingredients (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    meal_id       INTEGER NOT NULL REFERENCES meals(id),
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
    quantity      REAL    NOT NULL,
    unit          TEXT
);

CREATE INDEX IF NOT EXISTS idx_meal_ingredients_meal_id
    ON meal_ingredients (meal_id);

-- ─────────────────────────────────────────────
-- Meal plans
-- start_date is an ISO-8601 date string for the Monday of the planned week
-- (e.g. "2026-04-06"). One plan per user per week.
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meal_plans (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id),
    start_date TEXT    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_meal_plans_user_week
    ON meal_plans (user_id, start_date);

CREATE INDEX IF NOT EXISTS idx_meal_plans_user_id
    ON meal_plans (user_id);

-- ─────────────────────────────────────────────
-- Meal plan entries
-- Assigns a meal to a day within a plan.
-- day_offset: 0 = Monday … 6 = Sunday.
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meal_plan_entries (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    meal_plan_id INTEGER NOT NULL REFERENCES meal_plans(id),
    meal_id      INTEGER NOT NULL REFERENCES meals(id),
    day_offset   INTEGER NOT NULL CHECK (day_offset BETWEEN 0 AND 6),
    meal_type    TEXT    NOT NULL,

    UNIQUE (meal_plan_id, day_offset, meal_type)
);

CREATE INDEX IF NOT EXISTS idx_meal_plan_entries_plan_id
    ON meal_plan_entries (meal_plan_id);
