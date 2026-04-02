# Risks

## Risk Register

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|------------|--------|------------|
| R-01 | Ollama response is unparseable or malformed | High | Medium | Strict parser with clear error surfacing; retry option; log raw output |
| R-02 | Local model quality is poor | Medium | High | Prompt engineering; structured output format; user can retry or override manually |
| R-03 | Ollama is not running when user opens the app | Medium | High | Detect early; show clear actionable error ("Start Ollama and try again") |
| R-04 | Ingredient matching against CoFID is inaccurate | High | Low | Clearly label estimates as approximate; do not over-engineer the matcher |
| R-05 | Generation is too slow on low-end hardware | Medium | Medium | Non-blocking UI with progress indicator; set expectations in UI copy |
| R-06 | SQLite file becomes corrupted | Low | High | Keep schema simple; wrap writes in transactions; validate on open |
| R-07 | Module system (JPMS) causes packaging issues | Medium | Medium | Use established jlink + moduleplugin setup from Gradle; test packaging early |
| R-08 | Scope creep derails the portfolio focus | Medium | Medium | Maintain a strict non-goals list; resist adding features that obscure the architecture |

---

## Risk Detail

### R-01 — Unparseable Ollama Response
The biggest technical risk. LLMs do not always follow a specified output format, even with explicit JSON instructions. The parser must be written defensively: expect missing fields, extra text before/after JSON, and type mismatches. Never render raw output in the UI.

**Mitigation detail:** `MealPlanParser` throws a typed `ParseException` on any failure. `MealPlanService` catches this and returns an error result to the UI. The raw response string is logged. The UI offers a single-click retry.

### R-02 — Local Model Quality
A smaller local model (e.g. 7B parameter) may produce repetitive, bland, or nutritionally nonsensical suggestions. This is a product quality risk rather than a correctness risk.

**Mitigation detail:** The prompt includes explicit variety instructions and a structured format that forces the model to think meal-by-meal. Manual edit/override is always available as a fallback.

### R-03 — Ollama Not Running
Users may forget to start Ollama before opening the app. The HTTP call will fail immediately.

**Mitigation detail:** Detect connection failure at the `OllamaClient` level. Surface a specific error message distinguishing "Ollama not reachable" from other failures. Ideally, check on app startup and warn proactively.

### R-04 — Nutrient Estimate Accuracy
AI-generated ingredient quantities are vague ("some olive oil", "a handful of spinach"). Matching these to CoFID entries and computing accurate macros is not reliably possible.

**Mitigation detail:** Show estimates with a clear "~" prefix and a disclaimer. Do not invest engineering effort in a precise matching algorithm — approximate is acceptable and clearly labelled.

### R-05 — Generation Latency
On a mid-range laptop, a 7B model via Ollama may take 15–45 seconds to respond. This is long enough to feel broken if the UI gives no feedback.

**Mitigation detail:** Run generation on a background thread (`Task<MealPlan>`); show an animated progress indicator; keep the rest of the UI responsive.

### R-08 — Scope Creep
As a portfolio project, there is a temptation to keep adding features. Each addition that isn't directly demonstrating the core architecture dilutes the portfolio signal.

**Mitigation detail:** Follow the planned development order (vertical slice first). New feature ideas go on a backlog, not into the current sprint.
