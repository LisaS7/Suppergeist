# Assumptions and Constraints

## Assumptions

### A-01 — Ollama is installed and running
The app assumes Ollama is installed on the user's machine and a suitable model has been pulled before first use. The app does not install or manage Ollama itself.

### A-02 — A single local user
The app is designed for a single user on a single machine. There is no concept of accounts, profiles, or multi-user access. All data is stored locally and treated as belonging to one person.

### A-03 — Meals are evening / main meals
The meal planner generates one meal per day, assumed to be dinner (or the main meal). Breakfast and lunch are out of scope.

### A-04 — Nutritional estimates are rough
CoFID 2021 data provides a basis for estimates, but ingredient matching is approximate and quantities in AI-generated meals are not precise. Estimates are shown for guidance only.

### A-05 — English-language output
Ollama prompts and expected responses are in English. No internationalisation is planned.

### A-06 — The LLM response format can be guided but not guaranteed
The prompt specifies a JSON output structure. The parser must handle deviations gracefully. It is assumed that a capable local model (e.g. Llama 3, Mistral) will comply with structured prompts most of the time.

### A-07 — Java 21 and JavaFX are available
The app targets Java 21. Users are expected to have a compatible JRE, or the app is distributed as a self-contained jlink image.

---

## Constraints

### C-01 — No internet at runtime
The app must function entirely offline. No external API calls, no CDN assets, no telemetry.

### C-02 — No cloud or remote database
All persistence is local SQLite. No sync, no backup service, no remote access.

### C-03 — Single process
The app is a single JavaFX desktop process. No microservices, no embedded web server, no separate backend.

### C-04 — Ollama API surface
Integration is limited to what Ollama's local HTTP API exposes. The app uses the `/api/generate` endpoint only. Streaming responses are acceptable but the app may opt for non-streaming for simplicity.

### C-05 — CoFID 2021 data is read-only
`nutrients.db` is a static dataset bundled with the app. It is not updated at runtime. Users cannot modify nutritional data.

### C-06 — Portfolio scope
This is a portfolio project, not a commercial product. Scope is intentionally bounded. Features that would require significant ongoing maintenance (e.g. barcode scanning, recipe databases, cloud sync) are excluded.
