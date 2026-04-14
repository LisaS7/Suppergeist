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
| `MealPlanService`       | Coordinates three repositories; `dayLabel` formatted as `"Monday 6 Apr"`; week-start ordering is a display concern handled by `MainController`, not the service |
| `MainController` wiring | Constructs repositories + service in `initialize()`; calls `getWeeklyMeals(1, 2026-04-06)`; uses loaded `weekStartDay` to order grid columns |
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

Storage: `dietary_constraints` and `avoid_food_codes` serialised as comma-separated strings. `getUser` returns a
synthetic default `User` with empty preferences if the row doesn't exist.

Round-trip tests in `UserRepositoryTest` cover: default creation, idempotency, preference persistence, overwrite,
blank/null handling, and synthetic default for unknown IDs.

---

## Task 7 — Wire preferences sidebar ✅

*(Depends on Task 6)*

**What's built:** `PreferencesSidebarController` is fully wired end-to-end. All preference fields are loaded on startup and persisted on save:

- Dietary constraint checkboxes (vegetarian, vegan, gluten-free, dairy-free) — stored/loaded as lowercase strings
- Servings-per-meal spinner
- Show-calories / show-nutritional-info checkboxes
- Week-start-day `ChoiceBox`
- **Avoid ingredients — searchable multi-select:**
  - All ingredients loaded once into an `ObservableList<Ingredient>` via `IngredientRepository.getAllIngredients()`
  - A `FilteredList<Ingredient>` wraps it; the `ListView` is bound to the `FilteredList`
  - A `TextField` (`avoidFoodCodesSearch`) drives the filter — on each keystroke, the `FilteredList` predicate is swapped to a case-insensitive name-contains check
  - On load, ingredients whose food code is in `user.getAvoidFoodCodes()` are pre-selected via `getSelectionModel().select(ingredient)`
  - On save, `getSelectionModel().getSelectedItems()` is read and food codes extracted into a `Set<String>`
- `IngredientRepository` is injected into the sidebar from `MainController.initialize()` alongside `UserRepository`
- `Ingredient.toString()` overridden to return `name` so the `ListView` renders ingredient names correctly

`MainController.initialize()` calls `preferencesSidebarController.setFormValues(user)` on startup. An `onPreferencesSaved` callback triggers `refreshMealPlanGrid()` so the grid reacts immediately when preferences are saved.

For development, `MainController` still uses a fixed reference date (`2026-04-06`) aligned with seeded data. This is intentional and will be replaced once active-week navigation or automatic current-week plan loading exists.

> **Design note:** Filtering is done in-memory rather than per-keystroke DB queries. All ~2000 ingredients are loaded once; the `FilteredList` predicate is swapped on each keystroke. This avoids repeated DB connections and is fast enough for the dataset size.

---

## Task 7a — Consolidate user loading in `MainController` ✅

*(Complete alongside Task 7 — required before Task 7 is considered closed)*

`MainController` holds a single `private User user` field, loaded once in `initialize()` via `userRepository.getUser(1)`. The `onPreferencesSaved` callback is typed as `Consumer<User>` (changed from `Runnable`) so the sidebar passes the updated `User` back on save; the lambda assigns `this.user = updatedUser` before calling `refreshMealPlanGrid()`. `refreshMealPlanGrid()` reads from the field — no DB re-query.

`PreferencesSidebarController.loadUser(int)` was renamed to `setFormValues(User)` — it now accepts a `User` directly, assigns it to `this.user`, and populates the form fields. No DB access. The `throws SQLException` declaration was removed accordingly.

**Done when:** `userRepository.getUser()` is called exactly once per user action; `refreshMealPlanGrid()` uses the cached `User`; the TODO comment is removed.

---

## Task 7b — Preferences sidebar architectural cleanup ⬜

*(Stems from architecture review of Task 7 — address before Phase 3 begins)*

**Structural (address first)**

1. **Introduce `UserPreferencesService`** — `PreferencesSidebarController` and `MainController` both hold repository references directly, violating the "UI never touches the database" rule. A `UserPreferencesService` should own three responsibilities: loading a `User` by ID, saving preferences via `UserRepository`, and fetching the ingredient list for the avoid-foods picker via `IngredientRepository`. Controllers receive the service, not the individual repositories.

2. **Replace DOM-walking for dietary constraint checkboxes** — the save/load path casts every child of `dietaryConstraintsBox` to `CheckBox` and uses `.getText().toLowerCase()` as the persistence key. A `Label` or `Separator` added to that `VBox` would throw a `ClassCastException` at runtime. Give each checkbox an `fx:id` (`vegetarianCheckbox`, `veganCheckbox`, etc.) and wire as `@FXML` fields; the save/load path then references fields explicitly and display text is decoupled from the persistence key.

**Correctness**

3. **Fix listener accumulation in `setFormValues`** — the `avoidFoodCodesSearch` text-change listener is added on every `setFormValues` call. Move listener registration to `initialize()`.

**Minor UI polish** *(can be batched in one pass)*

4. Add `promptText="Search ingredients…"` to `avoidFoodCodesSearch` in the FXML.
5. Add a `StringConverter` to `weekStartDayChoiceBox` so it renders `Monday` rather than `MONDAY`.
6. Tag the hardcoded `getUser(1)` call in `MainController` with a `// TODO: resolve when multi-user support is added`.

**Done when:** no controller holds a direct repository reference; `UserPreferencesService` mediates all user preference and ingredient access; dietary constraint save/load uses `@FXML`-wired fields; the text-search listener is registered once in `initialize()`; minor UI polish applied.

> **Follow-up (not in scope for 7b):** `MainController.initialize()` still constructs its own `DatabaseManager`, `MealRepository`, `MealPlanRepository`, `MealPlanEntryRepository`, and `MealPlanService` directly. These should move to `SuppergeistApplication` and be injected via setter (the same pattern used for `UserPreferencesService`). Deferred until `MealPlanService` gains more responsibilities that make the wiring worth touching.

---

## Task 8 — `MealIngredientRepository` ✅

Loads ingredient lines for a meal from the `meal_ingredients` table. `ShoppingListService` (Task 9) depends on this.

Both methods implemented:

- `getIngredientsForMeal(int mealId)` — returns `List<MealIngredient>` from `meal_ingredients`
- `getIngredientsWithNameForMeal(int mealId)` — returns `List<MealIngredientRow>`, joined with `ingredients` for the name and food code, ordered alphabetically

`MealIngredientRow` is a top-level public record in its own file (`repository/MealIngredientRow.java`): `ingredient` (full `Ingredient` model), `quantity`, `unit`. Food code is accessible as `row.ingredient().getFoodCode()` — no flat `foodCode` field needed.

`MealIngredientRepositoryTest` covers: empty list for unknown meal, field mapping, null unit, isolation between meals, multiple ingredients, joined name, alphabetical ordering.

**Done when:** both query methods work against the seeded DB; tests pass.

---

## Task 9 — `ShoppingListService` 🔄

*(Depends on Task 8)*

Derives an aggregated ingredient list from a `MealPlan`. Lives in `com.example.suppergeist.service`.

```java
public class ShoppingListService {
    public List<ShoppingItem> buildList(int mealPlanId) throws SQLException { ...}
}
```

**What's built:** `buildList` aggregates ingredients across all meals in a plan, deduplicating by ingredient ID and summing quantities. `ShoppingItem` is a package-private record inside `ShoppingListService`: `name`, `totalQuantity` (Double), `unit` (String), `category` (String). Food code is accessed via `MealIngredientRow.ingredient().getFoodCode()` — no flat field needed.

`ShoppingListServiceTest` covers: empty plan, single ingredient, name/unit carry-through, quantity aggregation across two and three meals, distinct ingredients, non-existent plan ID.

**Remaining:**

- Mixed-unit handling: when the same ingredient appears with different units across meals, quantities should be concatenated as `"200 g, 3 tbsp"` rather than summed. Currently quantities are summed regardless of unit, and the unit from the first row is used.
- Category derivation from `foodCode` prefix (currently always `""`):

| Prefix(es) | Category            |
|-----------|---------------------|
| `11`      | Bakery & Grains     |
| `12`      | Dairy & Eggs        |
| `13`      | Vegetables & Beans  |
| `14`      | Fruit & Nuts        |
| `16`      | Fish                |
| `18`, `19`| Meat                |
| `17`, `50`| Food Cupboard       |
| null / other | General          |

Prefixes form loose but supermarket-aligned clusters — good enough for UX grouping; refinement deferred. Category is a presentation concern — not stored in the DB.

- Sort by category then name (currently insertion order).

> **Phase 3 note:** `NutritionService` will need numeric quantities and ingredient IDs — keeping `totalQuantity` as
> a `Double` (rather than collapsing into a display string) preserves this. When mixed-unit concatenation is
> implemented, the numeric quantity and unit should remain accessible separately for nutrition calculation.

**Done when:** `ShoppingListService.buildList()` returns a non-empty list for the seeded plan; duplicate ingredients are consolidated; category is derived from food code prefix; items are sorted by category then name; mixed units are concatenated rather than summed.

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

Foreign key relationships are defined in `Schema.java` but no `ON DELETE` behaviour has been specified (SQLite defaults
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
- [x] Preferences sidebar reads/writes preferences; survives restart (all fields including avoid-ingredients)
- [x] Weekly grid layout uses `weekStartDay` from loaded preferences
- [x] Saving preferences refreshes the grid immediately without restart
- [ ] Controllers hold no direct repository references; `UserPreferencesService` mediates all preference and ingredient access; dietary constraints use `@FXML`-wired fields; text-search listener registered once; minor UI polish applied
- [x] `MealIngredientRepository` returns correct rows and joined names for seeded data
- [~] `ShoppingListService` aggregates ingredients by ID and sums quantities (category derivation, mixed-unit handling, and sort not yet implemented)
- [ ] Shopping list panel renders and copy-to-clipboard works
