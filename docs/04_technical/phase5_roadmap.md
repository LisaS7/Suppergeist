# Phase 5 Roadmap — Ollama Integration & Plan Generation

Goal: layer AI plan generation on top of the manual editing foundation built in Phase 4.
By the end of Phase 5, a user can click "Generate Plan", see a populated weekly grid, and get clear feedback
when generation fails. The write paths from Phase 4 are reused directly.

Status markers: ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## Task 1 — `OllamaClient` ⬜

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

## Task 2 — `PromptBuilder` ⬜

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

## Task 3 — `MealPlanParser` ⬜

*(Depends on Task 2 — prompt format defines parse schema)*

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
`requires com.fasterxml.jackson.databind` to `module-info.java`. This was flagged as a future dependency in
the Phase 2 roadmap.

> Raw LLM output must never reach the UI. `MealPlanParser` is the validation boundary — if it throws, the
> caller shows an error to the user and offers a retry.

**Done when:** parser correctly handles a well-formed response; throws `ParseException` with a clear message on
each failure case; unit tests cover: valid input, missing field, wrong meal count, malformed JSON.

---

## Task 4 — Plan generation end-to-end ⬜

*(Depends on Phase 3 Task 1 for week navigation, Phase 4 write paths, and Tasks 1–3 above)*

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
5. Persist using the write methods from Phase 4: insert a `MealPlan` row, then for each `ParsedMeal` insert
   a `Meal` row + `MealPlanEntry` row; attempt ingredient name lookup via `IngredientRepository.searchByName()`
   and insert `MealIngredient` rows where a match is found (unmatched ingredients are skipped, not an error)
6. Return the saved `MealPlan`

**Ingredient matching strategy:**

`IngredientRepository.searchByName()` uses the fuzzy LIKE query already built for Phase 4 ingredient editing —
no new query needed. LLM-generated names like `"chicken breast"` match against `"chicken"` via `LIKE '%query%'`.
Take the first result if multiple rows match (advisory — a wrong match means a slightly wrong nutrition estimate,
not a crash).

**Regeneration / overwrite policy:**

Recommended default: **overwrite** — delete the existing `MealPlan` (and its entries, via cascade) for that
`weekStart` before inserting the new one. This keeps the DB clean and matches the user's mental model of
"replace this week's plan". For now, silently replace; a confirmation dialog for manually edited plans is a
post-Phase 5 concern.

**Concurrency:** the Ollama call is blocking. Run the entire `generateAndSave` call inside a JavaFX `Task<MealPlan>`;
update the UI via `Platform.runLater` on success or failure.

**Done when:** clicking "Generate Plan" produces a populated weekly grid; the app stays responsive during generation;
parse and network errors surface as user-visible messages with a retry path; regenerating an existing week replaces
it cleanly.

---

## Task 5 — Generation feedback states ⬜

*(Depends on Task 4)*

Add the feedback states that are only meaningful once generation exists.

| Situation                              | User-visible feedback                                        |
|----------------------------------------|--------------------------------------------------------------|
| Generation failed (network / parse)    | Error alert with failure reason + "Try again" button         |
| Shopping list empty (no plan)          | Shopping list panel updates to `"Generate a plan to see      |
|                                        | your list"` (replaces Phase 3 "No plan loaded" message)      |

These are display-layer changes; no schema or service changes required.

**Done when:** each scenario above shows the correct message.

---

## Future Considerations

### Per-meal Regenerate / Override (post-Phase 5)

The product vision includes regenerating a single meal slot and free-text override. Post-Phase 5 once bulk
generation is stable.

### Meal feedback (post-MVP)

Like/dislike signals to influence future prompts. Explicitly out of scope until the core generation loop is solid.

### JSON export / import (deferred)

Adds non-trivial effort (Jackson, schema versioning) for low demo value. Revisit post-submission if needed.

---

## Phase 5 Completion Criteria

- [ ] `OllamaClient`, `PromptBuilder`, `MealPlanParser` implemented and tested in isolation
- [ ] "Generate Plan" button produces a populated weekly grid; the app stays responsive during generation
- [ ] Parse and network errors surface as user-visible messages with a retry path
- [ ] Regenerating an existing week replaces it cleanly
- [ ] Generation-failure and empty shopping list states show correct messages
