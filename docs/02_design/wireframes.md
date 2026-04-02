# Wireframes

Wireframes are described textually here as low-fidelity layouts. Visual assets will live alongside this file.

The visual direction is **neobrutalist / retro-90s**: bold borders, flat colours, chunky typography, minimal decoration. Fast and unpretentious.

---

## Screen 0 — Empty State (no plan yet)

Shown on first launch, or any time the app has no saved plan. Replaces the meal plan grid with a centred message and prominent call to action.

```
┌─────────────────────────────────────────────────────┐
│  SUPPERGEIST                               [≡ Prefs] │
├─────────────────────────────────────────────────────┤
│                                                     │
│                                                     │
│                                                     │
│            No meal plan yet.                        │
│                                                     │
│     Start by setting your dietary preferences,      │
│     then generate your first week of meals.         │
│                                                     │
│                                                     │
│              [ ≡  Set Preferences ]                 │
│                                                     │
│              [▶  Generate My Plan ]                 │
│                                                     │
│                                                     │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**Notes:**
- The two CTAs are stacked and large — this is the entire purpose of the screen
- "Set Preferences" opens the slide-out preferences panel (same as `[≡ Prefs]` in the header)
- "Generate My Plan" works immediately with default preferences if the user skips that step
- Once a plan is generated, this screen is replaced by Screen 1 and never shown again unless all plans are deleted (out of scope)

---

## Screen 1 — Main / Meal Plan View

```
┌─────────────────────────────────────────────────────┐
│  SUPPERGEIST                               [≡ Prefs] │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Week of 31 Mar 2026                                │
│                                                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ MON      │ │ TUE      │ │ WED      │   ...       │
│  │          │ │          │ │          │            │
│  │ Meal     │ │ Meal     │ │ Meal     │            │
│  │ name     │ │ name     │ │ name     │            │
│  │          │ │          │ │          │            │
│  │ ~500kcal │ │ ~620kcal │ │ ~480kcal │            │
│  │ [Edit]   │ │ [Edit]   │ │ [Edit]   │            │
│  └──────────┘ └──────────┘ └──────────┘            │
│                                                     │
│         [Generate Plan]   [Shopping List]           │
└─────────────────────────────────────────────────────┘
```

**Notes:**
- Each day card shows meal name and rough kcal estimate
- Edit opens an inline swap or free-text override
- Generate Plan replaces the whole week; confirmation if a plan already exists
- `[≡ Prefs]` button in the header triggers the slide-out preferences panel (see Screen 3)

---

## Screen 2 — Shopping List View

```
┌─────────────────────────────────────────────────────┐
│  SHOPPING LIST — Week of 31 Mar 2026     [← Back]   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Produce                                            │
│  ☐  Broccoli (500g)                                 │
│  ☐  Cherry tomatoes (250g)                          │
│  ☐  Garlic (1 bulb)                                 │
│                                                     │
│  Protein                                            │
│  ☐  Chicken thighs (800g)                           │
│  ☐  Eggs (6)                                        │
│                                                     │
│  Pantry                                             │
│  ☐  Tinned chickpeas (2x400g)                       │
│  ☐  Olive oil                                       │
│                                                     │
│                              [Copy to Clipboard]    │
└─────────────────────────────────────────────────────┘
```

**Notes:**
- Ingredients aggregated and grouped by category
- Checkboxes for in-store use (state not persisted)
- Copy to clipboard for sharing/notes app use

---

## Screen 3 — Preferences Panel (slide-out)

The preferences panel slides in from the right edge of the main window, overlaying the content without navigating away. The rest of the UI dims behind it. Closing the panel (via `[✕]` or clicking outside) returns focus to the main view.

```
┌──────────────────────────────┬──────────────────────┐
│  SUPPERGEIST      [≡ Prefs]  │  PREFERENCES    [✕]  │
├──────────────────────────────┼──────────────────────┤
│                              │                      │
│  Week of 31 Mar 2026         │  Dietary constraints │
│                              │  ☑  Vegetarian       │
│  ┌────────┐ ┌────────┐       │  ☐  Vegan            │
│  │ MON    │ │ TUE    │  ...  │  ☐  Gluten-free      │
│  │        │ │        │       │  ☐  Dairy-free       │
│  │ Meal   │ │ Meal   │       │                      │
│  │ name   │ │ name   │       │  Avoid ingredients   │
│  │        │ │        │       │  [mushrooms    ][+]  │
│  │~500kcal│ │~620kcal│       │  > mushrooms  [x]   │
│  │ [Edit] │ │ [Edit] │       │  > shellfish  [x]   │
│  └────────┘ └────────┘       │                      │
│                              │  Servings per meal   │
│  [Generate Plan]             │  [  2  ]             │
│  [Shopping List]             │                      │
│                              │              [Save]  │
└──────────────────────────────┴──────────────────────┘
                                ▲ slides in from right
```

**Notes:**
- Panel is always one click away from any screen — no navigation required
- Dimmed backdrop signals modal-like focus without a full screen change
- `[Save]` persists changes to SQLite; panel closes automatically after save
- Preferences are injected into the next Ollama prompt
- Panel can be dismissed without saving by clicking `[✕]` or the backdrop

---

## Screen 4 — Plan History

```
┌─────────────────────────────────────────────────────┐
│  PLAN HISTORY                            [← Back]   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ▶  Week of 31 Mar 2026  (current)                  │
│  ▶  Week of 24 Mar 2026                             │
│  ▶  Week of 17 Mar 2026                             │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**Notes:**
- Selecting a past plan opens it in read-only mode
- Current plan always shown first
