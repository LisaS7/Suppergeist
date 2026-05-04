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
- `MealPlanService` — loads weekly meal view models and owns manual plan/meal create, update, and delete operations
- `GeneratePlanService` — orchestrates AI generation: loads preferences and ingredients, builds the prompt, calls Ollama, parses the response, and saves the generated plan
- `OllamaClient` — thin HTTP wrapper around the Ollama local API; returns raw response strings
- `PromptBuilder` — constructs structured prompts from user preferences and format templates
- `MealPlanParser` — parses Ollama's response into typed `ParsedMeal` / `ParsedIngredient` records; throws on unparseable output
- `MealIngredientService` — adds, removes, lists, and searches ingredients for individual meals
- `ShoppingListService` — derives a grouped ingredient list from a `MealPlan`
- `NutritionService` — computes calories, macros, and micronutrient estimates from stored meal ingredients

### Data Layer
- `MealPlanRepository` — CRUD for saved plans (`app.db`)
- `MealRepository` — CRUD for meals (`app.db`)
- `MealIngredientRepository` — ingredient lines per meal, with joined ingredient names (`app.db`)
- `UserRepository` — read/write user record including all preference fields (`app.db`)
- `IngredientRepository` — queries against the `ingredients` table, which includes seeded CoFID 2021 nutrition columns (`app.db`)

---

## Domain Models

```
MealPlan
├── id: Integer
├── userId: int
└── startDate: LocalDate

Meal
├── id: Integer
├── mealPlanId: int
├── dayOffset: int
├── mealType: String
└── mealName: String

Ingredient
├── id: Integer
├── name: String
├── foodCode: String
└── CoFID nutrition columns

NutritionalEstimate
├── cal: int
├── proteinG: double
├── carbsG: double
├── fatG: double
└── additional sugar, fibre, vitamin, and folate fields

User
├── dietaryConstraints: Set<String>
├── avoidFoodCodes: Set<String>
├── servingsPerMeal: int
├── showCalories: boolean
└── showNutritionalInfo: boolean
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
MealPlanParser ──► List<ParsedMeal> or MealPlanParseException
      │
      ▼
Validation ──► exactly 7 meals? required fields present? positive quantities?
      │
      ▼
GeneratePlanService persists the plan and returns MealPlan
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
├── service/             ← business logic, AI integration, shopping lists, nutrition
├── repository/          ← SQLite repositories
├── database/            ← DatabaseManager, Schema
└── model/               ← domain models
```
