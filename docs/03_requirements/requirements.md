# Requirements

## Functional Requirements

### FR-01 — Meal Plan Generation
- The system shall generate a 7-meal weekly plan using a locally-running Ollama model
- Each generated meal shall include: name, meal type, and ingredient lines
- Calorie and macro estimates shall be computed from stored ingredients after generation, where matched nutrition data exists
- Generation shall be triggered by a single user action

### FR-02 — Preference-Aware Generation
- The system shall accept dietary constraints (e.g. vegetarian, gluten-free, dairy-free)
- The system shall accept a list of ingredients/foods to avoid
- The system shall accept a servings-per-meal setting
- All preferences shall be persisted and applied automatically to future generations

### FR-03 — Manual Plan Editing
- The user shall be able to create and delete a plan for the displayed week
- Empty day slots shall expose an add-meal action
- Existing meal cards shall support edit and remove actions
- The user shall be able to add and remove ingredient lines for each meal
- Manual edits shall be saved to the current plan

### FR-04 — Shopping List
- The system shall derive an aggregated ingredient list from the current weekly plan
- Ingredients shall be grouped by category derived from CoFID food-code prefixes
- The user shall be able to copy the list as plain text
- Checkbox state (checked/unchecked) shall be maintained in-memory for the session

### FR-05 — Plan Persistence
- Generated and edited plans shall be saved to local SQLite storage
- The user shall be able to navigate between weeks and view any saved plan for the selected week

### FR-06 — Nutritional Estimates
- Rough calorie and macro estimates shall be shown per meal
- Estimates shall draw on CoFID 2021 data (seeded into `app.db.ingredients` at first launch from the bundled `ingredient_mapping.csv`) where ingredient matches are possible
- Estimates are advisory only; no guarantee of accuracy

### FR-08 — Plan Management
- The user shall be able to delete the saved plan for the displayed week

---

## Non-Functional Requirements

### NFR-01 — Local-Only Operation
- The app shall function without any internet connection at runtime
- All AI inference shall run via Ollama on the local machine
- No user data shall leave the device

### NFR-02 — Performance
- Plan generation shall complete within a time acceptable for the local model in use (target: under 60 seconds on a mid-range laptop)
- The UI shall remain responsive (non-blocking) during Ollama calls

### NFR-03 — Resilience
- The system shall handle malformed or unexpected Ollama responses gracefully
- Errors shall be surfaced to the user with a clear message
- Raw LLM output shall never be rendered directly without parsing and validation

### NFR-04 — Portability
- The app shall run on any machine with Java 21 and Ollama installed
- No additional server, database daemon, or cloud service shall be required

### NFR-05 — Maintainability
- Code shall follow a strict three-layer separation: UI, Service, Data
- Each layer shall be independently comprehensible
- No business logic in controllers; no data access in services beyond repository calls

---

## Post-MVP / Future Considerations

These are intentionally out of scope for the initial release but worth tracking as potential enhancements.

### FUT-01 — Meal Feedback (Like / Dislike)
- The user shall be able to mark any generated meal as liked or disliked directly from the plan view
- Feedback shall be persisted per meal (identified by name or a stable hash of name + key ingredients)
- The Ollama prompt shall incorporate accumulated feedback: liked meals can be used as positive examples; disliked meals shall be excluded or deprioritised in future generations
- **Why deferred:** Requires a feedback schema, prompt-engineering work to reliably use it, and enough generation history to be meaningful. Core generation must be solid first.
