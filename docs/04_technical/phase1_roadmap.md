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

## Task 7 — Add basic logging ⬜

The app currently has no logging — only `System.err` and print statements. Add a `java.util.logging.Logger` field (no
new dependency, built into the JDK) to the key classes in scope for Phase 1: `DatabaseManager` and `MainController`.

```java
private static final Logger log = Logger.getLogger(DatabaseManager.class.getName());
```

Log at `INFO` for lifecycle events (connection opened, app started) and `WARNING`/`SEVERE` for errors. This keeps
observability in place as the service layer grows without adding a logging framework dependency now.

> SLF4J + Logback is the right long-term choice (see Phase 2) — but that's a dependency + JPMS wiring exercise. For
> Phase 1, `java.util.logging` ships with the JDK and requires zero configuration.

**Done when:** `DatabaseManager` and `MainController` log key events; no raw `System.err` calls remain in those classes.

---

## Task 8 — Light model validation ⬜ *(optional)*

Before the service layer starts persisting data, decide where input validation lives. For Phase 1, constructor guards are
sufficient:

- `Meal`: reject null or blank `name`
- `MealIngredient`: reject non-positive `quantity`
- `UserPreferences`: reject non-positive `servingsPerMeal`

```java
public Meal(int id, String name) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Meal name must not be blank");
    this.id = id;
    this.name = name;
}
```

Keep guards narrow — only fields where a bad value would cause a silent bug downstream. Do not add validation
complexity for fields that are only ever set from trusted DB reads.

**Done when:** guards are in place on the three models above; no service-layer validation is needed yet.

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
- [ ] Basic logging in place (`DatabaseManager`, `MainController`)
- [ ] (optional) Constructor guards on `Meal`, `MealIngredient`, `UserPreferences`
