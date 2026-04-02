# User Flows

## Flow 1 — Generate a Meal Plan

```mermaid
flowchart TD
    A([App Launch]) --> B[Main View]
    B --> C{Plan exists?}
    C -- Yes --> D[Display existing plan]
    D --> E[Generate Plan clicked]
    C -- No --> F[Build prompt from preferences]
    E --> G{Confirm replace?}
    G -- No --> D
    G -- Yes --> F
    F --> H[Send to Ollama - local]
    H --> I[Parse + validate response]
    I --> J{Valid?}
    J -- Yes --> K[Display meal plan]
    J -- No --> L[Show error / offer retry]
    K --> M[Save plan to SQLite]
```

---

## Flow 2 — Edit a Single Meal

```mermaid
flowchart TD
    A[Meal Plan View] --> B[Click Edit on a day card]
    B --> C{Edit option}
    C -- Regenerate slot --> D[Prompt Ollama for one meal]
    D --> E[Parse + validate]
    E --> F[Replace card in view]
    F --> G[Save updated plan]
    C -- Free-text override --> H[User types meal name/details]
    H --> G
```

---

## Flow 3 — View Shopping List

```mermaid
flowchart TD
    A[Meal Plan View] --> B[Click Shopping List]
    B --> C[Aggregate ingredients across all 7 meals]
    C --> D[Group by category]
    D --> E[Display Shopping List View]
    E --> F{User action}
    F -- Check off item --> G[Toggle checkbox - in-memory only]
    F -- Copy to Clipboard --> H[Plain-text list copied]
```

---

## Flow 4 — Update Preferences

```mermaid
flowchart TD
    A[Any screen] --> B[Click Preferences]
    B --> C[Preferences panel opens with current settings]
    C --> D[User edits constraints / avoids / servings]
    D --> E[Click Save]
    E --> F[Persist to SQLite preferences table]
    F --> G[Panel closes, main UI regains focus]
    G --> H[/Preferences take effect on next plan generation/]
```

---

## Flow 5 — View Plan History

```mermaid
flowchart TD
    A[Main View] --> B[Click History]
    B --> C[List of saved plans with dates]
    C --> D[Select a past plan]
    D --> E[Display plan in read-only mode]
    E --> F[Back]
    F --> C
```
