# Architecture

## Overview

Suppergeist follows a strict three-layer architecture. Each layer has a single responsibility and depends only on the layer below it. There is no skipping layers.

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
   ├── Ollama (HTTP, localhost)
   └── SQLite files
```

---

## Layer Responsibilities

### UI Layer
- JavaFX controllers wired to FXML views
- Receives user input, passes it to services, renders results
- No direct database access; no prompt construction
- Handles async calls to keep the UI non-blocking (JavaFX `Task` / `Platform.runLater`)

### Service Layer
- `MealPlanService` — orchestrates plan generation: reads preferences, builds prompt, calls Ollama, parses response, validates, saves via repository
- `OllamaClient` — thin HTTP wrapper around the Ollama local API; returns raw response strings
- `PromptBuilder` — constructs structured prompts from user preferences and format templates
- `MealPlanParser` — parses Ollama's response into typed `MealPlan` / `Meal` models; throws on unparseable output
- `ShoppingListService` — derives and groups an ingredient list from a `MealPlan`

### Data Layer
- `MealPlanRepository` — CRUD for saved plans (`app.db`)
- `MealPlanEntryRepository` — CRUD for plan entries (join between plans and meals) (`app.db`)
- `MealRepository` — CRUD for meals (`app.db`)
- `MealIngredientRepository` — ingredient lines per meal, with joined ingredient names (`app.db`)
- `UserRepository` — read/write user record including all preference fields (`app.db`)
- `IngredientRepository` — queries against the `ingredients` table, which includes seeded CoFID 2021 nutrition columns (`app.db`)

---

## Domain Models

```
MealPlan
├── weekOf: LocalDate
└── meals: List<Meal>  (7 entries, one per day)

Meal
├── day: DayOfWeek
├── name: String
├── ingredients: List<Ingredient>
└── estimate: NutritionalEstimate

Ingredient
├── name: String
└── quantity: String  (free-text, e.g. "200g")

NutritionalEstimate
├── kcal: int
├── proteinG: double
├── carbsG: double
└── fatG: double

UserPreferences
├── dietaryConstraints: Set<String>  (e.g. "vegetarian", "gluten-free")
├── avoidIngredients: List<String>
└── servingsPerMeal: int
```

---

## AI Integration

Ollama is treated as an unreliable external data source, not a trusted system component.

```
UserPreferences
      │
      ▼
PromptBuilder ──► structured prompt (JSON output format specified)
      │
      ▼
OllamaClient ──► HTTP POST localhost:11434/api/generate
      │
      ▼
raw String response
      │
      ▼
MealPlanParser ──► MealPlan (typed) or ParseException
      │
      ▼
Validation ──► all 7 meals? required fields present? sensible values?
      │
      ▼
MealPlanService returns MealPlan (or propagates error to UI)
```

The prompt specifies the expected JSON structure explicitly. The parser does not rely on LLM goodwill — it validates every field.

---

## Concurrency

Ollama calls are blocking HTTP and may take several seconds. These are executed off the JavaFX Application Thread using JavaFX `Task<MealPlan>`, with results returned via `Platform.runLater`. The UI shows a progress indicator during generation and remains interactive.

---

## Package Structure

```
com.example.suppergeist
├── ui/                  ← controllers (MainController, PreferencesSidebarController)
├── service/             ← business logic, AI integration (flat for now; ai/ and plan/ subpackages planned)
├── repository/          ← SQLite repositories + DatabaseManager, Schema
├── database/            ← DatabaseManager, Schema
└── model/               ← domain models
```
