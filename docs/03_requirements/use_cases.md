# Use Cases

## UC-01 — Generate Weekly Meal Plan

**Actor:** User  
**Preconditions:** Ollama is running locally; app is open  
**Trigger:** User clicks "Generate Plan"

**Main Flow:**
1. If a plan already exists, prompt user to confirm replacement
2. System reads current preferences from SQLite
3. System constructs a structured prompt (preferences + output format instructions)
4. System sends prompt to Ollama via local API
5. System receives response and parses it into a `MealPlan` model
6. System validates the parsed plan (all 7 meals present, required fields populated)
7. System displays the plan in the UI
8. System persists the plan to SQLite

**Alternate Flow — Ollama unavailable:**
- Step 4 fails; system shows error "Could not reach Ollama. Is it running?" with retry option

**Alternate Flow — Response unparseable:**
- Step 5 fails; system shows error and offers retry; raw response is logged for debugging

---

## UC-02 — Regenerate a Single Meal

**Actor:** User  
**Preconditions:** A weekly plan is displayed  
**Trigger:** User clicks Edit → Regenerate on a day card

**Main Flow:**
1. System identifies which day slot is being replaced
2. System constructs a single-meal prompt (preferences + context of remaining meals to avoid repetition)
3. System sends prompt to Ollama
4. System parses and validates the response
5. System replaces only that day's meal in the displayed plan
6. System saves the updated plan to SQLite

---

## UC-03 — Override a Meal Manually

**Actor:** User  
**Preconditions:** A weekly plan is displayed  
**Trigger:** User clicks Edit → Free-text override on a day card

**Main Flow:**
1. User types a meal name and optional notes
2. System saves the override to that day's slot (no Ollama call)
3. Plan is updated in the UI and persisted to SQLite

---

## UC-04 — View Shopping List

**Actor:** User  
**Preconditions:** A weekly plan exists  
**Trigger:** User clicks "Shopping List"

**Main Flow:**
1. System aggregates all ingredients across the 7 meals
2. System groups ingredients by category
3. System displays the shopping list view
4. User optionally checks off items (session-only state)
5. User optionally copies the list as plain text

---

## UC-05 — Set Dietary Preferences

**Actor:** User  
**Preconditions:** App is open  
**Trigger:** User opens Preferences panel

**Main Flow:**
1. System loads current preferences from SQLite and displays them
2. User toggles dietary constraints and/or edits avoid list and servings
3. User clicks Save
4. System persists updated preferences to SQLite
5. System returns user to previous screen

**Postcondition:** Next plan generation will use the updated preferences

---

## UC-06 — View Plan History

**Actor:** User  
**Preconditions:** At least one plan has been saved  
**Trigger:** User opens History view

**Main Flow:**
1. System loads list of saved plans from SQLite (date, week label)
2. User selects a plan
3. System displays the plan in read-only mode
4. User returns to history list or main view
