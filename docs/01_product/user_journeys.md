# User Journeys

## Journey 1 — Generate a Weekly Meal Plan (Core Journey)

**Persona:** Maya (Pragmatic Home Cook)  
**Goal:** Get a full week of meal suggestions and a shopping list

| Step | User Action | System Response |
|------|-------------|-----------------|
| 1 | Opens Suppergeist | Main screen loads; shows last plan if one exists |
| 2 | Reviews or sets dietary preferences | Preferences panel shows current settings (e.g. no red meat) |
| 3 | Clicks "Generate Plan" | App constructs a structured prompt from preferences + CoFID data |
| 4 | Waits a few seconds | Ollama processes locally; progress indicator shown |
| 5 | Views generated plan | 7 meals displayed with name, key ingredients, rough macros |
| 6 | Edits a meal she dislikes | Swaps one suggestion manually or regenerates a single slot |
| 7 | Views shopping list | Aggregated ingredient list derived from finalised plan |
| 8 | Screenshots or copies the list | Ready to take to the supermarket |

**Success:** Maya has a plan and list in under 2 minutes, without leaving the app or touching the internet.

---

## Journey 2 — First-Time Setup

**Persona:** Tom (Casual User)  
**Goal:** Get started with no prior configuration

| Step | User Action | System Response |
|------|-------------|-----------------|
| 1 | Launches app for the first time | Onboarding prompt: set a few basic preferences (optional) |
| 2 | Skips preferences | App uses sensible defaults |
| 3 | Clicks "Generate Plan" | Plan generated with no dietary constraints |
| 4 | Sees a meal he doesn't like | Marks it as disliked |
| 5 | Regenerates | New suggestion avoids that meal; preference is saved |

**Success:** Tom gets value from the app in his first session without reading any documentation.

---

## Journey 3 — Update Dietary Preferences

**Persona:** Maya  
**Goal:** Tell the app she's now avoiding gluten

| Step | User Action | System Response |
|------|-------------|-----------------|
| 1 | Opens Preferences panel | Current settings shown |
| 2 | Adds "gluten-free" constraint | Saved to SQLite preferences store |
| 3 | Generates a new plan | Prompt includes gluten-free constraint; plan reflects this |
| 4 | Verifies no gluten-containing meals | Validates against ingredients where possible |

**Success:** Preference is persisted and reflected in the next generation without extra steps.

---

## Journey 4 — Revisit a Previous Plan

**Persona:** Maya  
**Goal:** See what she planned last week

| Step | User Action | System Response |
|------|-------------|-----------------|
| 1 | Opens plan history | List of saved plans shown with dates |
| 2 | Selects last week's plan | Plan displayed in read-only view |
| 3 | Uses it as a reference | No action required from the app |

**Success:** Previous plans are accessible without needing to regenerate.
