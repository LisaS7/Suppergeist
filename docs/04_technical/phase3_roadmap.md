# Phase 3 Roadmap — AI Generation & Real Data

Goal: build the data foundation first (nutrition, shopping list polish), then layer Ollama generation on top.
By the end of Phase 3, a user can generate a plan, see actual kcal figures, and navigate between weeks.

Status markers: ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## Task 1 — Active week / plan navigation ⬜

The UI currently uses a fixed reference date (`2026-04-06`). Replace it with a "current week" calculation and add
navigation controls so the user can move between weeks without touching code.

**UI changes:**

- Header area above the meal grid shows the week range (e.g. `"7 Apr – 13 Apr 2026"`)
- Prev / Next arrow buttons on either side step the display back or forward by one week
- The loaded week derives from `LocalDate.now()` on startup; prev/next adjust a `currentWeekStart` field in
  `MainController`

**Service / repository behaviour:**

- `MealPlanService.getWeeklyMeals(userId, startDate)` already accepts a `startDate` — no change needed there
- When navigating to a week with no stored plan, the grid shows empty cells (the "no plan" state is already
  supported by the service returning an empty list)
- The meal-plan lookup must remain tied to `startDate` — week-start navigation must not change how plans are
  retrieved from the DB, only which `startDate` is passed in

**Done when:** the user can click Prev/Next to move between weeks; the date label updates; empty weeks show an empty
grid; the hardcoded reference date is gone from `MainController`.

---

## Task 2 — `NutritionService` ⬜

Computes a `NutritionalEstimate` for a meal from its stored `MealIngredient` rows and the nutrition columns on
`ingredients`. The CoFID nutrition data is already in `app.db` — this task just wires it up.

```java
public class NutritionService {
    /** Returns null if the meal has no matched ingredients with nutrition data. */
    public NutritionalEstimate estimateForMeal(int mealId) throws SQLException { ... }
}
```

**Calculation:**

For each `MealIngredient` row where `ingredients.energy_kcal` is non-null:
- normalise quantity to grams (treat unitless or unknown units as grams)
- `contribution = (quantity / 100) × nutrient_per_100g`
- sum contributions across all matched ingredients

Return `null` if no ingredient rows matched (caller shows `"-- kcal"` placeholder in that case).

> `NutritionService` reads the inline nutrition columns already on `app.db.ingredients`. It does not query a
> separate `nutrients.db` at runtime — see the Phase 2 roadmap note on this design decision.

**Done when:** `estimateForMeal()` returns a non-null estimate for at least one meal in the seeded data; returns
null correctly for a meal with no ingredient matches; unit tests cover both cases.

---

## Task 3 — Wire nutrition into UI ⬜

*(Depends on Task 2)*

Replace the `"-- kcal"` placeholder on each meal card with the computed value where available. Respect the
`showCalories` and `showNutritionalInfo` preference toggles the user already has.

**Meal card changes:**

- `kcal` label populated from `NutritionService.estimateForMeal()` where non-null; otherwise stays `"-- kcal"`
- If `showNutritionalInfo` is true, also show protein / carbs / fat under the kcal line
- If `showCalories` is false, hide the kcal label entirely (and the macro line if present)

**Optional (stretch):**

- Daily total row at the bottom of each column: sum of kcal for that day's meals
- Weekly total in the footer / summary area

> Do not block the UI thread computing nutrition for 7 meals. Call `NutritionService` once per meal during
> grid population, which already runs after the DB load.

**Done when:** meals with matched ingredients show real kcal values; toggling `showCalories` off hides the figures;
`"-- kcal"` still appears for unmatched meals.

---

## Task 4 — Shopping list polish ⬜

Improve the shopping list so it reads like a real supermarket list rather than a raw data dump.

**Quantity handling:**

- Same unit → sum: `200g + 100g → 300g`
- Different units → fallback string: `"200ml, 1 cup"` (acceptable for MVP)
- Aggregate by `ingredientId`, not name, to avoid silent mismatches on case or whitespace differences

**Ingredient categorisation:**

Derive category from the two-character CoFID `foodCode` prefix in `ShoppingListService.determineCategory()`.
Not stored in the DB — presentation concern only.

| Prefix(es)   | Category            |
|--------------|---------------------|
| `11`         | Bakery & Grains     |
| `12`         | Dairy & Eggs        |
| `13`         | Vegetables & Beans  |
| `14`         | Fruit & Nuts        |
| `16`         | Fish                |
| `18`, `19`   | Meat                |
| `17`, `50`   | Food Cupboard       |
| null / other | General             |

Sort output by category then name.

> Keep raw ingredient IDs and quantities accessible until after nutrition calculation — do not collapse them into
> the display string prematurely. `NutritionService` (Task 2) will need numeric quantities too.

**Done when:** common same-unit cases sum correctly; cross-unit cases show a readable fallback; shopping list is
grouped by category.

---

## Task 5 — Ollama integration: `OllamaClient` ⬜

A thin, blocking HTTP wrapper around the Ollama `/api/generate` endpoint. Lives in
`com.example.suppergeist.service`.

```java
public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    /** Sends a prompt and returns the raw response string. Blocking. */
    public String generate(String prompt) throws IOException { ... }
}
```

- Uses `java.net.http.HttpClient` (available in Java 11+; no new dependencies)
- Single public method; returns the raw string; no parsing, no retry logic here
- Throws `IOException` on network failure — callers handle it
- Use `"stream": false` in the request body for a single complete response

**Done when:** `OllamaClient.generate("say hello")` returns a non-empty string when Ollama is running locally.

---

## Task 6 — `PromptBuilder` ⬜

Constructs a structured prompt for meal plan generation from a `User`. Lives in
`com.example.suppergeist.service`.

```java
public class PromptBuilder {
    public String buildMealPlanPrompt(User user) { ... }
}
```

- Prompt instructs Ollama to return a JSON array of 7 meal objects
- Each object must include: `name` (String), `ingredients` (array of `{name, quantity, unit}`), and
  `mealType` (String — e.g. `"dinner"`)
- Prompt embeds the user's `dietaryConstraints` and a note to avoid ingredients matching `avoidFoodCodes`
  (name-level description — LLMs do not know CoFID codes)
- Output format is specified explicitly in the prompt with an example object — do not rely on the model to infer it
- `servingsPerMeal` is included as a context hint

> The prompt format must match exactly what `MealPlanParser` expects. Write both together.

**Done when:** `PromptBuilder.buildMealPlanPrompt(user)` returns a non-empty, well-formed prompt string for a
default user; output format matches the schema `MealPlanParser` will parse.

---

## Task 7 — `MealPlanParser` ⬜

*(Depends on Task 6 — prompt format defines parse schema)*

Parses Ollama's raw response into a typed `List<ParsedMeal>`. Lives in `com.example.suppergeist.service`.

```java
public class MealPlanParser {
    /** @throws ParseException if the response is missing, malformed, or fails validation */
    public List<ParsedMeal> parse(String rawResponse) throws ParseException { ... }
}
```

`ParsedMeal` is a record: `name`, `mealType`, `List<ParsedIngredient>` where `ParsedIngredient` is
`name`, `quantity` (double), `unit`.

**Validation rules (throw `ParseException` on failure):**

- Response must be valid JSON
- Top-level must be an array with exactly 7 elements
- Each object must have non-null, non-blank `name` and `mealType`
- Each ingredient must have non-blank `name` and a positive `quantity`
- `unit` is allowed to be null/blank (some ingredients are unitless)

**Jackson:** add `com.fasterxml.jackson.core:jackson-databind` to `build.gradle.kts` and
`requires com.fasterxml.jackson.databind` to `module-info.java`. This was flagged as a Phase 3 dependency in
the Phase 2 future considerations section.

> Raw LLM output must never reach the UI. `MealPlanParser` is the validation boundary — if it throws, the
> caller shows an error to the user and offers a retry.

**Done when:** parser correctly handles a well-formed response; throws `ParseException` with a clear message on
each failure case; unit tests cover: valid input, missing field, wrong meal count, malformed JSON.

---

## Task 8 — Plan generation end-to-end ⬜

*(Depends on Tasks 1, 5, 6, 7)*

Wire generation into the UI. The user clicks "Generate Plan", a plan is created for the current week, and the
grid refreshes with the new meals.

**UI changes:**

- "Generate Plan" button in the toolbar / header area
- Button is disabled while generation is in progress; a `ProgressIndicator` is shown
- On success: grid refreshes automatically
- On failure: an error alert shows the failure reason with a "Try again" option (do not silently swallow parse
  or network errors)

**Service method:**

```java
// in MealPlanService (or a new GeneratePlanService — decide based on size)
public MealPlan generateAndSave(int userId, LocalDate weekStart) throws IOException, ParseException, SQLException { ... }
```

1. Load `User` from `UserRepository`
2. Build prompt via `PromptBuilder`
3. Call `OllamaClient.generate(prompt)`
4. Parse response via `MealPlanParser`
5. Persist: insert a `MealPlan` row, then for each `ParsedMeal` insert a `Meal` row +
   `MealPlanEntry` row; attempt ingredient name lookup and insert `MealIngredient` rows where a match is found
   (unmatched ingredients are skipped, not an error)
6. Return the saved `MealPlan`

**Ingredient matching strategy:**

`IngredientRepository.searchByName()` must do better than an exact match — LLM-generated names like
`"chicken breast"` will not match a CoFID row called `"chicken"`.

Minimum viable approach:
- Normalise both sides to lowercase before comparing
- Use `LIKE '%query%'` (contains) rather than exact equality
- Take the first result if multiple rows match (the match is advisory — a wrong match just means a slightly
  wrong nutrition estimate, not a crash)

This is still optimistic but covers the common cases. Refine in a later phase if match quality is visibly poor.

**Regeneration / overwrite policy:**

Decide before implementing: if a plan already exists for the target week, does `generateAndSave` overwrite it,
create a second plan alongside it, or block with an error?

Recommended default: **overwrite** — delete the existing `MealPlan` (and its entries, via cascade) for that
`weekStart` before inserting the new one. This keeps the DB clean and matches the user's mental model of
"replace this week's plan". Surface a confirmation dialog if the existing plan has any manually overridden meals
(post-MVP concern — for now, silently replace).

**Concurrency:** the Ollama call is blocking. Run the entire `generateAndSave` call inside a JavaFX `Task<MealPlan>`;
update the UI via `Platform.runLater` on success or failure.

**Done when:** clicking "Generate Plan" produces a populated weekly grid; the app stays responsive during generation;
parse and network errors surface as user-visible messages with a retry path; regenerating an existing week replaces
it cleanly.

---

## Task 9 — Validation & feedback layer ⬜

*(Depends on Tasks 2, 3, 8)*

The app currently says nothing when data is absent or wrong. Add lightweight feedback for the most likely
failure states.

| Situation                                              | User-visible feedback                                          |
|--------------------------------------------------------|----------------------------------------------------------------|
| Current week has no saved plan                         | Grid cells show a "No plan — click Generate to start" prompt   |
| Generation failed (network / parse)                    | Already handled in Task 8 — error alert + retry button         |
| Meal has ingredients but none matched to the DB        | Meal card shows `"-- kcal (no data)"` instead of `"-- kcal"`  |
| Meal has no ingredients at all                         | Meal card shows `"No ingredients recorded"`                    |
| Shopping list is empty (no plan for the current week)  | Shopping list panel shows `"Generate a plan to see your list"` |

These are display-layer changes; no schema or service changes required.

**Done when:** each scenario above shows the correct message rather than a blank or misleading state.

---

## Future Considerations

### JSON export / import (deferred)

Flagged during Phase 3 planning as out of scope. The differentiating work in this project is Ollama integration
and nutritional data — JSON portability adds non-trivial effort (Jackson, schema versioning) for low demo value.
Revisit post-submission if needed.

### ON DELETE cascade rules (carried from Phase 2)

Foreign key `ON DELETE` behaviour must be decided before any delete operations are implemented. See Phase 2
future considerations for the full decision table.

### SLF4J + Logback (carried from Phase 2)

Still using `java.util.logging`. Upgrade when per-package log level control becomes useful — likely warranted
once the Ollama integration is in place and verbose HTTP logging needs to be silenced in tests.

### Per-meal Regenerate / Override (post-Phase 3)

The product vision includes regenerating a single meal slot and free-text override. These are post-Phase 3 once
bulk generation is stable.

### Meal feedback (post-MVP)

Like/dislike signals to influence future prompts. Explicitly out of scope until the core generation loop is solid.

---

## Phase 3 Completion Criteria

- [ ] Hardcoded reference date replaced with current-week calculation; prev/next navigation works
- [ ] `NutritionService` computes estimates from stored ingredient data
- [ ] Meal cards show real kcal figures where data exists; `showCalories` toggle is functional
- [ ] Shopping list groups by category and sums same-unit quantities correctly
- [ ] `OllamaClient`, `PromptBuilder`, `MealPlanParser` implemented and tested in isolation
- [ ] "Generate Plan" button produces a populated weekly grid; errors surface with retry option; regenerating replaces the existing plan cleanly
- [ ] Validation messages appear for the no-plan and no-nutrition-data states
