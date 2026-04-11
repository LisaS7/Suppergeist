# Phase 1 Roadmap — Foundation

Goal: app compiles, runs, and shows real data. All domain models defined, DB connected, UI shell in place, minimal
observability added.

Status markers: ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## Task 1 — Fix `mainClass` in `build.gradle.kts` ✅

`mainClass` updated to `com.example.suppergeist.SuppergeistApplication`. `./gradlew run` launches cleanly.

---

## Task 2 — Set a real window size in `SuppergeistApplication` ✅

Scene is `1200 × 900`. Launch size is acceptable for Phase 1.

---

## Task 3 — Create package directory structure ✅

All packages created: `model/`, `database/`, `repository/`, `service/`, `ui/`. `MainController` moved to `ui/`.

---

## Task 4 — Define domain model classes (`model` package) ✅

All eight model types exist and compile:

- `Ingredient.java` — `id`, `name`, `foodCode`
- `MealIngredient.java` — `id`, `mealId`, `ingredientId`, `quantity`, `unit`
- `Meal.java` — `id`, `name`
- `MealPlan.java` — `id`, `userId`, `startDate`
- `User.java`
- `MealPlanEntry.java` — maps to the `meal_plan_entries` join table
- `NutritionalEstimate.java` — record with `cal`, `proteinG`, `carbsG`, `fatG` (non-persisted)
- `UserPreferences.java` — record with `dietaryConstraints`, `avoidIngredients`, `servingsPerMeal`

> **Note:** `NutritionalEstimate` uses `cal` rather than `kcal` as originally spec'd.

---

## Task 5 — Add SQLite JDBC dependency ✅

`org.xerial:sqlite-jdbc:3.45.3.0` is present in `build.gradle.kts` and resolves cleanly.

---

## Task 6 — Basic `module-info.java` ✅

App compiles and runs. Current state:

```java
module com.example.suppergeist {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;

    opens com.example.suppergeist to javafx.fxml;   // redundant — remove in Phase 2
    exports com.example.suppergeist;
    exports com.example.suppergeist.ui;
    opens com.example.suppergeist.ui to javafx.fxml;
}
```

Strict cleanup (add `requires org.xerial.sqlitejdbc`, remove redundant root `opens`, add exports for all active
packages) is Phase 2 Task 1.

---

## Task 7 — Add basic logging ✅

`java.util.logging.Logger` fields added to `DatabaseManager` and `MainController`. `DatabaseManager` logs at `INFO` on
each connection opened. `MainController` logs `initialize()` entry, meal count on success, and `SEVERE` with the
exception on `SQLException` — no raw `System.err` calls remain in either class.

---

## Task 8 — Light model validation ✅

Constructor guards in place on all three models:

- `Meal` — rejects null or blank `name`
- `MealIngredient` — rejects non-positive `quantity` (uses `!(quantity > 0)` to also catch `NaN`)
- `UserPreferences` — rejects non-positive `servingsPerMeal` via compact record constructor

---

> **Phase 2 tasks** (DB init, schema, preferences, shopping list) are tracked in
> [phase2_roadmap.md](phase2_roadmap.md).

---

## Future Considerations

### CSS — replace blanket `HBox`/`VBox` padding reset with a style class

`style.css` resets padding and spacing to `0` on every `HBox` and `VBox` in the scene. This will require re-specifying
spacing on every layout container added in future. Replace the element-type selectors with a utility class (e.g.
`.no-pad`) applied only where the reset is actually needed.

---

## Phase 1 Completion Criteria

- [x] `./gradlew run` launches the app
- [x] SQLite JDBC dependency resolves (`sqlite-jdbc:3.45.3.0`)
- [x] All domain model types compile
- [x] `module-info.java` compiles cleanly; app runs
- [x] UI shell shows weekly grid with real data from seeded `app.db`
- [x] Basic logging in place (`DatabaseManager`, `MainController`)
- [x] Constructor guards on `Meal`, `MealIngredient`, `UserPreferences`
