# Use Cases

## UC-01 — Generate Weekly Meal Plan

**Actor:** User  
**Preconditions:** Ollama is running locally; app is open  
**Trigger:** User clicks "Conjure with AI"

**Main Flow:**
1. System reads the current cached user preferences
2. System loads the allowed ingredient list from SQLite
3. System constructs a structured prompt (preferences + ingredient list + output format instructions)
4. System sends prompt to Ollama via local API in a JavaFX background task
5. System receives response and parses it into validated generated meal records
6. System deletes any existing plan for the same week, then persists the generated plan, meals, and matched ingredient lines
7. System refreshes the meal grid, nutrition estimates, and shopping list

**Alternate Flow — Ollama unavailable:**
- Step 4 fails; system shows error "Could not reach Ollama. Is it running?" with retry option

**Alternate Flow — Response unparseable:**
- Step 5 fails; system shows error and offers retry; raw response is logged for debugging

---

## UC-02 — Add, Edit, or Remove a Meal

**Actor:** User  
**Preconditions:** A weekly plan exists for the displayed week  
**Trigger:** User clicks an empty slot add button, or right-clicks a meal card

**Main Flow:**
1. For an empty slot, user enters a meal name and meal type
2. For an existing meal, user chooses Edit or Remove from the context menu
3. System inserts, updates, or deletes the meal row in SQLite
4. System refreshes the grid, nutrition estimates, and shopping list

## UC-03 — Edit Meal Ingredients

**Actor:** User  
**Preconditions:** A meal exists  
**Trigger:** User right-clicks a meal card and opens ingredient editing

**Main Flow:**
1. System displays the meal's current ingredients
2. User searches for an ingredient, enters quantity and unit, and adds it
3. User optionally removes existing ingredient lines
4. System persists ingredient changes to SQLite
5. System refreshes nutrition estimates and the shopping list

---

## UC-04 — View Shopping List

**Actor:** User  
**Preconditions:** A weekly plan exists  
**Trigger:** User opens the "Shopping List" tab

**Main Flow:**
1. System aggregates all ingredients across the 7 meals
2. System groups ingredients by category derived from food-code prefixes
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

## UC-06 — Navigate Weeks

**Actor:** User  
**Preconditions:** App is open  
**Trigger:** User clicks previous/next week buttons

**Main Flow:**
1. System updates the displayed week start date
2. System loads the plan for that week if one exists
3. System displays the saved meals, or an empty-week state if no plan exists
4. Shopping list and totals update for the selected week
