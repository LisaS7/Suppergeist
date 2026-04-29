# Phase 4 Roadmap — Manual Data Entry

Goal: make the app usable without AI by letting the user build and edit a meal plan by hand.
By the end of Phase 4, a user can create a plan for any week, fill in meal slots, and add ingredients —
giving the nutrition estimates from Phase 3 something real to work with.
AI generation in Phase 5 will reuse the write paths built here.

Status markers: ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## Task 1 — Schema cascade rules + repository write methods ✅

Two prerequisites before any create/delete UI can be built.

**ON DELETE cascade rules**

Decide and apply `ON DELETE` behaviour in `Schema.java` before implementing any delete operations. Recommended
defaults:

| Parent deleted | Child rows in       | Behaviour                                                          |
|----------------|---------------------|--------------------------------------------------------------------|
| `meal_plans`   | `meals`             | CASCADE — meal slots are meaningless without their plan            |
| `meals`        | `meal_ingredients`  | CASCADE — ingredient list is part of the meal                      |
| `ingredients`  | `meal_ingredients`  | CASCADE — ingredient line is meaningless without the ingredient    |
| `users`        | `meal_plans`        | CASCADE                                                            |

Note: SQLite foreign key enforcement requires `PRAGMA foreign_keys = ON` per connection — confirm
`DatabaseManager.getConnection()` sets this.

**Repository write methods**

Audit each repository for missing insert/update/delete methods and add what's needed:

- `MealRepository` — `insert(Meal)`, `update(Meal)`, `delete(int mealId)`
- `MealPlanRepository` — `insert(MealPlan)`, `delete(int mealPlanId)`
- `MealIngredientRepository` — `insert(int mealId, int ingredientId, double quantity, String unit)`, `delete(int id)`

Unit-test each write method against a real in-memory or temp DB — do not mock at the repository level.

**Done when:** cascade rules are applied in `Schema.java`; all listed write methods exist and are tested;
`PRAGMA foreign_keys = ON` is confirmed active.

---

## Task 2 — Create / delete a week's plan ✅

*(Depends on Task 1)*

The simplest entry point: let the user start a blank plan for the current week, or clear one they no longer want.

**UI:**

- When the displayed week has no plan, keep the empty-state message in the grid area and show a "Create plan for this week" button in the footer bar
- When a plan exists, a "Delete plan" option is available in the footer bar (confirmation dialog before proceeding)

**Service:**

```java
// in MealPlanService
public MealPlan createEmptyPlan(int userId, LocalDate weekStart) throws SQLException { ... }
public void deletePlan(int mealPlanId) throws SQLException { ... }
```

`createEmptyPlan` inserts a `MealPlan` row and returns it; no entries yet.
`deletePlan` deletes the plan row — cascade rules from Task 1 remove the entries automatically.

**Done when:** clicking "Create plan" produces a plan row in the DB and the grid refreshes (still empty);
clicking "Delete plan" removes it and the grid returns to the empty state.

---

## Task 3 — Add / edit / remove meals in slots ✅

*(Depends on Task 2)*

Let the user fill in the meal grid slot by slot.

**Interaction:**

- Empty slot shows a small "+" affordance; clicking it opens an add-meal dialog ✅
- Filled slot supports right-click to open a `ContextMenu` with "Edit" and "Remove" items ✅
- A short hint in the header area ("Right-click a card for options") explains the affordance ✅
- Left-click flip animation is preserved and unaffected ✅

**Add / edit dialog:**

- Text field for meal name ✅ (dialog shell exists with TextField + ComboBox)
- Dropdown / selector for meal type (e.g. breakfast, lunch, dinner — driven by the types already in the schema) ✅
- Save writes a single `Meal` row carrying `mealPlanId`, `dayOffset`, `mealType`, and `mealName` ✅
- Edit pre-populates the dialog with the current name and type; save updates the `Meal` row in place ✅
- Remove (via context menu) deletes the `Meal` row; cascade removes any associated `meal_ingredients` rows ✅

**Implementation notes:**

- `MealCardBuilder.buildMealCard` accepts `Runnable onEdit` and `Runnable onRemove` callbacks; the controller
  passes lambdas that open the appropriate dialog / call the service ✅
- `MealRepository.update(int mealId, String mealName, String mealType)` ✅
- `MealPlanService.updateMeal(int mealId, String mealName, String mealType)` ✅
- `MealPlanService.deleteMeal(int mealId)` ✅

**Done when:** a user can fill every slot in a week, rename meals, and clear slots; changes persist across
restarts; the grid reflects the DB state accurately after each operation.

---

## Task 4 — Ingredient editing per meal ✅

*(Depends on Task 3)*

Allow the user to attach ingredients to a meal so that nutrition estimates (Phase 3) have real data to work with.

**UI:**

- Meal card has an "Edit ingredients" affordance ✅
- Opens an ingredient dialog showing the current ingredient list for that meal ✅
- Each row shows: ingredient name, quantity, unit; a remove button ✅
- An "Add ingredient" row at the bottom: ingredient search field (same pattern as the avoid-foods picker in
  preferences), quantity input, unit input, confirm button ✅

**Service / repository:**

A new `MealIngredientService` was added (rather than expanding `MealPlanService`) to keep the meal-planning
service from getting crowded:

```java
// in MealIngredientService
public List<MealIngredientRow> getIngredientsForMeal(int mealId) throws SQLException { ... } ✅
public int addIngredientToMeal(int mealId, int ingredientId, double quantity, String unit) throws SQLException { ... } ✅
public void removeIngredientFromMeal(int mealIngredientId) throws SQLException { ... } ✅
public List<Ingredient> searchIngredients(String searchTerm) throws SQLException { ... } ✅
```

`addIngredientToMeal` returns the generated `meal_ingredients.id` so the UI can wire up the per-row remove
button without an extra round-trip. Ingredient search goes through `MealIngredientService.searchIngredients`
wrapping `IngredientRepository.searchByName()` — the same fuzzy LIKE query that Phase 5 will reuse for
AI-matched ingredients. ✅

**Done when:** a user can add and remove ingredients on any meal; ingredient lines are persisted; the kcal figure
on the meal card updates when the ingredient panel is closed (the controller calls `refreshMealPlanGrid()` after
the dialog closes, which re-renders the cards with updated nutrition). ✅

---

## Phase 4 Completion Criteria

- [x] Cascade rules applied in `Schema.java`; `PRAGMA foreign_keys = ON` confirmed active; all write methods present and tested
- [x] User can create and delete a week's plan via the UI
- [x] User can add, rename, and remove meals in individual slots
- [x] User can add and remove ingredients on a meal; nutrition estimate updates accordingly
