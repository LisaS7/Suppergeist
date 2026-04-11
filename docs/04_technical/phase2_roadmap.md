# Phase 2 Roadmap — Application Layer

Goal: fully wired application layer — DB self-initialises on first run, all repositories active, user preferences
persisted and applied, shopping list generated from the active plan, and the UI covers all three feature areas.

Status markers: ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## Completed during Phase 1 development

These tasks belong to Phase 2 but were built ahead of schedule:

| Task                    | What was built                                                                                               |
|-------------------------|--------------------------------------------------------------------------------------------------------------|
| `WeeklyMealViewModel`   | Record in `com.example.suppergeist.service` — `date`, `dayLabel`, `mealType`, `mealName`                     |
| `MealPlanService`       | Coordinates three repositories; week-start via `TemporalAdjusters`; `dayLabel` formatted as `"Monday 6 Apr"` |
| `MainController` wiring | Constructs repositories + service in `initialize()`; calls `getWeeklyMeals(1, 2026-04-06, MONDAY)`           |
| Data-driven weekly grid | Day labels and meal cards added dynamically; day label deduplication via `Set<LocalDate>`                    |

Remaining gap from this work: calorie labels show `"-- kcal"` placeholder — pending `NutritionService` (Phase 3).

---

## Task 1 — Strict `module-info.java` cleanup ✅

`requires org.xerial.sqlitejdbc` added; redundant `opens com.example.suppergeist` replaced with
`opens com.example.suppergeist.ui to javafx.fxml`; exports added for `model`, `service`, `database`, `repository`.

> As new packages are introduced, add an explicit `exports` declaration — missing declarations surface as confusing
> compile-time reflection errors rather than a clear "package not opened" message.

---

## Task 2 — Complete `DatabaseManager` ✅

`DatabaseManager` has `getConnection()` and `init()`. `init()` applies all DDL constants from `Schema.java` in order.
DB path is anchored to `~/.suppergeist/app.db` via `Path.of(System.getProperty("user.home"), ".suppergeist", "app.db")`.

> **`nutrients.db` is not opened at runtime.** The runtime app only talks to `app.db`. Nutrition data was pre-joined
> into `app.db.ingredients` by `seed_ingredients.py` during data prep — one canonical match per ingredient, decided
> upstream. Having the app query `nutrients.db` directly at runtime would require matching and disambiguation logic
> that belongs in the data pipeline, not the service layer. `DatabaseManager` stays focused on `app.db` only.

---

## Task 3 — Define `app.db` schema as `Schema.java` constants ✅

All DDL lives in `com.example.suppergeist.database.Schema` as string constants — one per table and index.
`DatabaseManager.init()` applies them in dependency order. No SQL files on disk.

---

## Task 4 — Wire `DatabaseManager.init()` into app startup ✅

*(Depends on Tasks 2 and 3)*

Call `DatabaseManager.init()` from the `init()` lifecycle method of `SuppergeistApplication`. Errors must be surfaced on
the JavaFX thread — the `start()` method shows an error alert if init failed:

```java
private SQLException initError;

@Override
public void init() throws Exception {
    try {
        DatabaseManager.init();
    } catch (SQLException e) {
        initError = e;
    }
}

@Override
public void start(Stage stage) {
    if (initError != null) {
        new Alert(Alert.AlertType.ERROR, "Database init failed: " + initError.getMessage()).showAndWait();
        Platform.exit();
        return;
    }
    // ... normal startup
}
```

**Done when:** `./gradlew run` starts cleanly, `app.db` is created if absent, and all tables exist.

---

## Task 5 — `AppSeedService` ✅

*(Depends on Task 4)*

A freshly initialized `app.db` has the correct schema but no data. `AppSeedService` runs immediately after `DatabaseManager.init()` and handles two bootstrap concerns: seeding the ingredient catalogue and ensuring a default user exists. Both checks are idempotent — they do nothing on subsequent launches.

This removes the need for any manual data script during normal app startup. The Python scripts remain useful for developer workflows (re-seeding after a schema reset, re-enriching the CSV from CoFID), but are no longer required to get a working app.

**Bootstrap sequence (as implemented):**

1. `DatabaseManager.init()` creates the schema (if absent)
2. `AppSeedService.seedIfEmpty(conn)` — if `ingredients` is empty, reads `ingredient_mapping.csv` from the classpath and inserts all rows in a single `executeBatch()`; otherwise skips and logs
3. `UserRepository.ensureDefaultUserExists()` — if no row with `id = 1` exists in `users`, inserts one; otherwise skips and logs

Both are called from `SuppergeistApplication.init()`. The default user step lives in `UserRepository` rather than `AppSeedService` but the behaviour is identical to the original spec.

**Bundling:** `ingredient_mapping.csv` lives at `src/main/resources/data/ingredient_mapping.csv`. This path is picked up by Gradle's standard resource processing and included in the built JAR. `AppSeedService` reads it via `getResourceAsStream("/data/ingredient_mapping.csv")`.

---

## Task 6 — `UserRepository` ✅

Read/write user data (including preferences) from the `users` table in `app.db`. Depends on Task 3 (table must exist).

All three methods implemented:

```java
public class UserRepository {
    public void ensureDefaultUserExists() throws SQLException { ... }
    public User getUser(int userId) throws SQLException { ... }
    public void savePreferences(User user) throws SQLException { ... }
}
```

Storage: `dietary_constraints` and `avoid_ingredients` serialised as comma-separated strings. `getUser` returns a
synthetic default `User` with empty preferences if the row doesn't exist.

Round-trip tests in `UserRepositoryTest` cover: default creation, idempotency, preference persistence, overwrite,
blank/null handling, and synthetic default for unknown IDs.

---

## Task 7 — Wire preferences sidebar ⬜

*(Depends on Task 6)*

The preferences sidebar (`prefsSidebar` VBox) exists in the FXML and toggles visibility, but its fields are unbound.

### UI changes

- Text fields or checkboxes for common `dietaryConstraints` (e.g. vegetarian, gluten-free)
- Numeric field for `servingsPerMeal`
- Save button that calls `UserRepository.savePreferences(...)`
- On startup, load preferences and populate fields

**Avoid ingredients — searchable multi-select (replaces free-text input):**

Free-text entry requires users to guess exact ingredient names and leads to fragile matching (e.g. `"tomato"` vs `"chopped tomatoes"`). Replace with a two-part control:

1. Search input field — filters the ingredient list only; not stored
2. Multi-select list — populated via `WHERE name LIKE '%<query>%'`; selected items are persisted

### Schema changes

Add a new join table to `Schema.java` and apply it in `DatabaseManager.init()`:

```sql
CREATE TABLE IF NOT EXISTS user_avoid_ingredients (
    user_id      INTEGER NOT NULL,
    ingredient_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, ingredient_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
)
```

Remove the `avoid_ingredients` column from `users` (or stop using it if already present).

### Model changes

Update `UserPreferences`:

- Change `avoidIngredients` from `List<String>` → `List<Integer>` (ingredient IDs)

### Repository changes

Update `UserRepository`:

- `savePreferences(...)` — delete existing rows for the user in `user_avoid_ingredients`, then insert selected IDs
- `getUser(...)` — load ingredient IDs from `user_avoid_ingredients` and populate `UserPreferences.avoidIngredients`

Add a query method (can live in `IngredientRepository` or inline in the preferences controller) for the search filter:

```sql
SELECT id, name FROM ingredients WHERE name LIKE ? ORDER BY name LIMIT 50
```

### Call-site change in `MainController`

Replace the hardcoded `DayOfWeek.MONDAY` and user ID `1` with values loaded from `UserRepository`:

```java
User user = userRepository.getUser(userId);
weeklyMeals = mealPlanService.getWeeklyMeals(userId, LocalDate.now(), user.weekStartDay());
```

> `UserPreferences` will need a `weekStartDay` field (default `MONDAY`) added as part of this task.

### Non-goals (this phase)

- No fuzzy matching beyond `LIKE '%query%'`
- No category or tag system
- No natural language parsing

**Done when:** selected avoid-ingredients survive a restart and are reloaded correctly; `MealPlanService` uses the loaded `weekStartDay`.

---

## Task 8 — `MealIngredientRepository` ⬜

Loads ingredient lines for a meal from the `meal_ingredients` table. `ShoppingListService` (Task 9) depends on this.

```java
public class MealIngredientRepository {
    public List<MealIngredient> getIngredientsForMeal(int mealId) throws SQLException { ...}
}
```

The basic query returns `MealIngredient` rows as-is. For callers that also need the ingredient name (e.g.
`ShoppingListService`), add a joined variant that returns a lightweight projection:

```java
public record MealIngredientRow(int ingredientId, String ingredientName, double quantity, String unit) {
}

public List<MealIngredientRow> getIngredientsWithNameForMeal(int mealId) throws SQLException { ...}
```

SQL for the joined variant:

```sql
SELECT mi.ingredient_id, i.name, mi.quantity, mi.unit
FROM meal_ingredients mi
         JOIN ingredients i ON mi.ingredient_id = i.id
WHERE mi.meal_id = ?
ORDER BY i.name
```

**Testing:** verify against the seeded `app.db` — the seed includes meals with ingredients. Check that:

- `getIngredientsForMeal` returns the correct row count for a known meal ID
- `getIngredientsWithNameForMeal` returns the ingredient name correctly joined
- an unknown `mealId` returns an empty list (not an exception)

**Done when:** both query methods work against the seeded DB; tests pass.

---

## Task 9 — `ShoppingListService` ⬜

*(Depends on Task 8)*

Derives an aggregated ingredient list from a `MealPlan`. Lives in `com.example.suppergeist.service`.

```java
public class ShoppingListService {
    public List<ShoppingItem> buildList(int mealPlanId) throws SQLException { ...}
}
```

`ShoppingItem` is a simple record: `name`, `totalQuantity` (String — free-text quantities are combined naively for MVP),
`category` (String — derive from `Ingredient.foodCode` prefix for MVP, or leave as `"General"` if unclassified).

**Algorithm (MVP):**

1. Load all `MealPlanEntry` rows for the plan
2. For each entry, load the meal's `MealIngredient` rows
3. Aggregate by `Ingredient.name` — concatenate quantities if units differ, sum if units match
4. Sort alphabetically; group by category

**Done when:** `ShoppingListService.buildList()` returns a non-empty list for the seeded plan; duplicate ingredients
are consolidated.

---

## Task 10 — Shopping list UI panel ⬜

*(Depends on Task 9)*

Add a shopping list panel to the main view. It can be a side panel or a tab — to be decided when the layout is
reviewed.

**Minimum viable contents:**

- Scrollable list of `ShoppingItem` entries with a checkbox per item (state held in-memory only)
- Item label shows `name` + `totalQuantity`
- "Copy to clipboard" button — plain text, one item per line
- Refresh triggered when a new plan is loaded

**Done when:** shopping list panel renders for the seeded plan; copy-to-clipboard produces readable plain text.

---

## Future Considerations

### SQL `ON DELETE` cascade behaviour

Foreign key relationships are defined in `schema.sql` but no `ON DELETE` behaviour has been specified (SQLite defaults
to `NO ACTION`). Decide cascade rules before implementing any delete operations:

| Parent deleted | Child rows in       | Expected behaviour                                                  |
|----------------|---------------------|---------------------------------------------------------------------|
| `meal_plans`   | `meal_plan_entries` | CASCADE — entries are meaningless without their plan                |
| `meals`        | `meal_ingredients`  | CASCADE — ingredients list is part of the meal                      |
| `meals`        | `meal_plan_entries` | RESTRICT or SET NULL                                                |
| `ingredients`  | `meal_ingredients`  | RESTRICT — don't silently delete ingredient lines                   |
| `users`        | `meal_plans`        | CASCADE or RESTRICT — depends on whether user deletion is supported |

Update `Schema.java` once decided.

### Upgrade to SLF4J + Logback (Phase 2+)

Phase 1 uses `java.util.logging`. SLF4J + Logback is the right long-term choice: structured output, configurable
levels per package, and no platform-specific quirks. Add when the service layer grows enough that log verbosity needs
per-package control. Requires JPMS wiring: `requires org.slf4j` in `module-info.java`.

### Jackson dependency (Phase 3+)

Add `com.fasterxml.jackson.core:jackson-databind` to `build.gradle.kts` and `requires com.fasterxml.jackson.databind`
to `module-info.java` when `MealPlanParser` is implemented. Not needed before then. (Phase 2 uses naive
`String.join`/`split` for preferences serialisation.)

### NutritionService (Phase 3)

`NutritionService` reads the inline nutrition columns already on `app.db.ingredients` — it does not query
`nutrients.db` at runtime. The data pipeline (`seed_ingredients.py`) made the food-identity decision once, upstream;
the app inherits clean, unambiguous data. There is no runtime `NutrientRepository` against the raw CoFID dataset.

---

## Phase 2 Completion Criteria

- [x] `WeeklyMealViewModel`, `MealPlanService`, and `MainController` wiring in place
- [x] Data-driven weekly grid with correct day labels
- [x] `module-info.java` declares all active packages; no redundant `opens`
- [x] `DatabaseManager.init()` creates `app.db` on first run with all tables
- [x] `app.db` path anchored to a stable location (not CWD-relative)
- [x] `users` table includes preference columns in schema and `Schema.java`
- [x] `AppSeedService` seeds `ingredients` and ensures default user on first run; skips on subsequent launches
- [x] `UserRepository` round-trips user preferences correctly
- [ ] Preferences sidebar reads/writes preferences; survives restart
- [ ] `MealPlanService` uses `weekStartDay` from loaded preferences
- [ ] `MealIngredientRepository` returns correct rows and joined names for seeded data
- [ ] `ShoppingListService` produces a consolidated ingredient list
- [ ] Shopping list panel renders and copy-to-clipboard works
