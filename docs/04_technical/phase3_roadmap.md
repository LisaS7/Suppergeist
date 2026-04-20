# Phase 3 Roadmap — Data Foundation

Goal: build the data foundation — week navigation and nutrition estimates.
By the end of Phase 3, a user can navigate between weeks and see real kcal figures on meal cards.
Ollama integration and plan generation are Phase 4.

Status markers: ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## Task 1 — Active week / plan navigation ✅

The UI currently uses a fixed reference date (`2026-04-06`). Replace it with a "current week" calculation and add
navigation controls so the user can move between weeks without touching code.

**UI changes:**

- Header area above the meal grid shows the week range (e.g. `"7 Apr – 13 Apr 2026"`) ✅
- Prev / Next arrow buttons on either side step the display back or forward by one week ✅
- The loaded week derives from `LocalDate.now()` on startup; prev/next adjust a `currentWeekStart` field in
  `MainController` ✅ (`LocalDate.now()` with `TemporalAdjusters` is already in place)

**Service / repository behaviour:**

- `MealPlanService.getWeeklyMeals(userId, startDate)` already accepts a `startDate` — no change needed there
- When navigating to a week with no stored plan, the grid shows empty cells (the "no plan" state is already
  supported by the service returning an empty list)
- The meal-plan lookup must remain tied to `startDate` — week-start navigation must not change how plans are
  retrieved from the DB, only which `startDate` is passed in

**Done when:** the user can click Prev/Next to move between weeks; the date label updates; empty weeks show an empty
grid; the hardcoded reference date is gone from `MainController`.

---

## Task 2 — `NutritionService` ✅

Computes a `NutritionalEstimate` for a meal from its stored `MealIngredient` rows and the nutrition columns on
`ingredients`. The CoFID nutrition data is already in `app.db` — this task just wires it up.

```java
public class NutritionService {
    /** Returns estimates keyed by mealId; meals with no matched nutrition data are absent from the map. */
    public Map<Integer, NutritionalEstimate> estimatesForMeals(List<Integer> mealIds) throws SQLException { ... }

    /** Returns daily kcal totals keyed by date, for days that have at least one matched meal. */
    public Map<LocalDate, Integer> dailyCalorieTotals(List<WeeklyMealViewModel> meals, Map<Integer, NutritionalEstimate> estimates) { ... }
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

## Task 3 — Wire nutrition into UI 🔄

*(Depends on Task 2)*

Replace the `"-- kcal"` placeholder on each meal card with the computed value where available. Respect the
`showCalories` and `showNutritionalInfo` preference toggles the user already has.

**Meal card changes:**

- `kcal` label populated from `NutritionService.estimateForMeal()` where non-null; otherwise stays `"-- kcal"`
- If `showNutritionalInfo` is true, also show protein / carbs / fat under the kcal line
- If `showCalories` is false, hide the kcal label entirely (and the macro line if present)

**Totals:**

- Daily total row at the bottom of each column: sum of kcal for that day's meals (only shown when `showCalories` is true)
- Weekly total in the footer / summary area

> Do not block the UI thread computing nutrition for 7 meals. Call `NutritionService` once per meal during
> grid population, which already runs after the DB load.

**Done when:** meals with matched ingredients show real kcal values; toggling `showCalories` off hides the figures
and totals; `"-- kcal"` still appears for unmatched meals; daily and weekly totals are visible.

---

## Task 4 — Validation & feedback layer ⬜

*(Depends on Tasks 2 and 3)*

Add lightweight, non-intrusive feedback for nutrition-related failure states. Generation-related states (no saved
plan, generation failure) are handled in Phase 4.

**kcal label:** always shows `"-- kcal"` when nutrition data is absent — no sub-variants on the label itself.

**`!` indicator:** a small `!` label on the meal card, visible only when nutrition data could not be computed.
Uses a JavaFX `Tooltip` (native, no extra dependencies) to show the specific reason on hover:

| Situation                                       | Tooltip text                        |
|-------------------------------------------------|-------------------------------------|
| Meal has ingredients but none matched to the DB | `"No nutrition data for this meal"` |
| Meal has no ingredients at all                  | `"No ingredients recorded"`         |

These are display-layer changes; no schema or service changes required.

**Done when:** meals missing nutrition data show `"-- kcal"` and a `!` indicator; hovering the indicator shows
the correct reason; meals with matched data show no indicator.

---

## Task 5 — Empty state messages ⬜

*(Depends on Task 1)*

When the user navigates to a week with no saved plan, the grid and shopping list are silently blank. Add simple
messages so the empty state is clearly intentional rather than a loading failure.

| Location             | Message                    | Condition                              |
|----------------------|----------------------------|----------------------------------------|
| Meal grid            | `"No plan for this week"`  | Week has no `MealPlan` in the DB       |
| Shopping list panel  | `"No plan loaded"`         | Current week has no plan               |

Display-layer changes only; no service or schema changes required.

**Done when:** navigating to an empty week shows the message in the grid; the shopping list panel shows its
message when no plan exists for the current week.

---

## Future Considerations

### JSON export / import (deferred)

Flagged during Phase 3 planning as out of scope. The differentiating work in this project is Ollama integration
and nutritional data — JSON portability adds non-trivial effort (Jackson, schema versioning) for low demo value.
Revisit post-submission if needed.

---

## Phase 3 Completion Criteria

- [x] Hardcoded reference date replaced with current-week calculation
- [x] Prev/Next week buttons and week range label working in the UI
- [ ] Empty week state shows `"No plan for this week"` in the grid; shopping list shows `"No plan loaded"`
- [x] `NutritionService` computes estimates from stored ingredient data
- [x] Meal cards show real kcal figures where data exists; `showCalories` toggle is functional
- [x] Daily and weekly kcal totals visible when `showCalories` is on
- [ ] `!` indicator with tooltip appears on meal cards missing nutrition data
