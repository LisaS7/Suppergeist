# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Suppergeist is a local-first meal planning desktop application (portfolio project). It generates a 7-meal weekly plan, an aggregated shopping list, and rough nutritional estimates, with dietary preference tracking. AI generation is handled via **Ollama** (local LLM, `localhost:11434`) — no cloud dependency, no internet required at runtime.

Tech stack: Java 21 + JavaFX (FXML), SQLite for persistence, Ollama for AI.

## Build & Run Commands

```bash
# Run the application
./gradlew run

# Build (compile + resources)
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.suppergeist.SomeTest"

# Package as a self-contained image (jlink)
./gradlew jlinkZip
```

## Data Pipeline

On first launch, `AppSeedService` seeds the `ingredients` table in `app.db` from a bundled CSV resource at `/data/ingredient_mapping.csv` (derived from CoFID 2021 data). This is a one-time operation — subsequent launches skip it if the table is non-empty.

To regenerate the ingredient CSV from the raw source:

```bash
# Step 1: Parse raw Excel → CSV
python data/scripts/process_nutrients.py

# Step 2: Load CSV → SQLite
python data/scripts/import_to_sqlite.py
```

Raw source: `data/raw/McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx` (CoFID 2021).

Runtime SQLite file:
- `app.db` — all application data: user preferences, ingredients (seeded from CoFID), meals, and meal plans (read-write, created on first run)

## Architecture (Three-Layer, Strict)

No layer skipping. UI never touches the database; services never touch the UI.

```
┌─────────────────────────────┐
│         UI Layer            │  JavaFX controllers + FXML views
│  (interaction only)         │  No business logic; delegates to services
├─────────────────────────────┤
│       Service Layer         │  Meal planning logic
│  (business rules + AI)      │  Prompt construction, response parsing,
│                             │  preference application, plan assembly
├─────────────────────────────┤
│        Data Layer           │  SQLite repositories
│  (persistence + nutrients)  │  Plans, preferences, CoFID nutrient lookup
└─────────────────────────────┘
         │
         ▼
   External Systems
   ├── Ollama (HTTP, localhost:11434)
   └── app.db (SQLite)
```

### Service Layer Classes (built)
- `MealPlanService` — loads the current week's meal plan entries from the DB and assembles `WeeklyMealViewModel` records for the UI
- `AppSeedService` — seeds the `ingredients` table from the bundled CoFID CSV on first launch
- `WeeklyMealViewModel` — record passed from `MealPlanService` to the UI (date, dayLabel, mealType, mealName)

### Service Layer Classes (planned — not yet built)
- `OllamaClient` — thin HTTP wrapper; returns raw response strings; calls are blocking (run off the JavaFX thread via `Task<MealPlan>`)
- `PromptBuilder` — constructs structured prompts (with explicit JSON output format) from user preferences
- `MealPlanParser` — parses Ollama's response into typed models; throws `ParseException` on failure; never trusts raw LLM output
- `ShoppingListService` — derives and groups an ingredient list from a meal plan

### Data Layer Classes
- `UserRepository` — CRUD for user record including all preference fields (`app.db`)
- `MealRepository` — CRUD for meals (`app.db`)
- `MealPlanRepository` — CRUD for meal plans (`app.db`)
- `MealPlanEntryRepository` — CRUD for meal plan entries (join between plans and meals) (`app.db`)
- `IngredientRepository` — queries on the ingredients/CoFID data (`app.db`)

### Concurrency
Ollama calls will run in a JavaFX `Task<MealPlan>`, results returned via `Platform.runLater`. The UI shows a progress indicator and remains interactive during generation.

## Package Structure

```
com.example.suppergeist
├── ui/                  ← controllers (MainController, PreferencesSidebarController)
├── service/             ← business logic, AI integration (flat for now; ai/ and plan/ subpackages planned)
├── repository/          ← SQLite repositories + DatabaseManager, Schema
├── database/            ← DatabaseManager, Schema
└── model/               ← domain models (see below)
```

## Domain Models

```
MealPlan
├── id: Integer
├── userId: int
└── startDate: LocalDate

MealPlanEntry  (join between MealPlan and Meal)
├── id: Integer
├── mealPlanId: int
├── mealId: int
├── dayOffset: int  (0–6, days from plan startDate)
└── mealType: String  (e.g. "dinner")

Meal
├── id: Integer
└── name: String

MealIngredient  (join between Meal and Ingredient)
├── id: Integer
├── mealId: int
├── ingredientId: int
├── quantity: double
└── unit: String

Ingredient
├── id: Integer
├── name: String
├── foodCode: String  (CoFID food code, nullable)
└── [nutritional columns: energy_kcal, protein_g, fat_g, carbohydrate_g, etc.]

NutritionalEstimate
├── cal: int
├── proteinG: double
├── carbsG: double
└── fatG: double

User  (carries all preference fields directly — no separate UserPreferences model)
├── id: Integer
├── name: String
├── dietaryConstraints: Set<String>  (e.g. "vegetarian", "gluten-free")
├── avoidFoodCodes: Set<String>      (CoFID food codes to exclude)
├── servingsPerMeal: int
├── showCalories: boolean
├── showNutritionalInfo: boolean
└── weekStartDay: int  (1 = Monday … 7 = Sunday, ISO-8601)
```

## Feature Scope (MVP)

- **Plan generation** — one action generates a full 7-meal week via Ollama
- **Preference-aware** — dietary constraints, avoided ingredients, servings; all persisted and applied automatically
- **Per-meal editing** — Regenerate (re-run AI for one slot) or Override (free-text manual entry)
- **Shopping list** — aggregated ingredients grouped by category; copyable as plain text or downloadable; checkbox state held in-memory for the session
- **Plan persistence** — plans saved to SQLite; user can browse past plans (read-only) and delete them
- **Nutritional estimates** — rough macros per meal using CoFID 2021 data where ingredient matches are possible; advisory only

**Post-MVP (out of scope now):** meal feedback (like/dislike) to influence future prompts.

## AI Integration Rules

Ollama is treated as an unreliable external source, not a trusted component:
1. `PromptBuilder` specifies the exact JSON structure expected in the response
2. `MealPlanParser` validates every field — throws on unparseable output
3. Validation checks: all 7 meals present? required fields non-null? values sensible?
4. Raw LLM output is never rendered directly to the UI
5. Errors are surfaced with a clear message and retry option

## Key Dependencies

- JavaFX 21.0.6 (`javafx.controls`, `javafx.fxml`) via Gradle plugin `org.openjfx.javafxplugin`
- Lombok for `@Getter` / `@AllArgsConstructor`
- Apache Commons CSV for parsing the ingredient seed CSV
- JUnit Jupiter 5.12.1 for tests
- `org.javamodularity.moduleplugin` for JPMS-aware compilation
- `org.beryx.jlink` for native image packaging

## Current State

The app has moved past initial scaffolding. What's working:

- **Entry point**: `SuppergeistApplication` (JPMS-safe wrapper: `Launcher`)
- **DB schema**: fully defined in `Schema.java`; created on first launch by `DatabaseManager.init()`
- **Seeding**: `AppSeedService` populates the `ingredients` table from the bundled CoFID CSV on first launch
- **User setup**: `UserRepository.ensureDefaultUserExists()` runs at startup to guarantee a user record
- **Meal plan loading**: `MealPlanService.getWeeklyMeals()` reads from DB and returns view models to the UI
- **Main view**: `MainController` renders a weekly meal grid from the DB; date range currently hardcoded while Ollama generation is not yet wired up
- **Preferences sidebar**: `PreferencesSidebarController` + `preferences-sidebar.fxml` (in progress)

Ollama AI integration (`OllamaClient`, `PromptBuilder`, `MealPlanParser`) has not been started yet.
