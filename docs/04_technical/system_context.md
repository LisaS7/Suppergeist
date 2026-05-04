# System Context

## Context Diagram

```mermaid
C4Context
    title System Context — Suppergeist

    Person(user, "User", "Plans meals and manages dietary preferences")

    System(suppergeist, "Suppergeist", "Local-first desktop meal planning app (JavaFX + Java)")

    System_Ext(ollama, "Ollama", "Locally-running LLM inference engine — no internet required")
    SystemDb_Ext(sqlite, "SQLite (app.db)", "Local database — plans, preferences, and CoFID nutrition data")

    Rel(user, suppergeist, "Uses", "Desktop UI")
    Rel(suppergeist, ollama, "Sends prompts / receives meal suggestions", "HTTP (localhost)")
    Rel(suppergeist, sqlite, "Reads/writes plans, preferences; reads nutrients", "JDBC")
```

---

## External Dependencies

| Dependency | Type | Role | Required at runtime? |
|------------|------|------|----------------------|
| Ollama | Local process | LLM inference for meal generation | Required for AI generation; manual plan editing works without calling it |
| SQLite (`app.db`) | File (read-write) | Plans, preferences, and CoFID nutrition data (seeded on first run) | Yes — created on first run |
| Java 21 JRE | Runtime | Executes the application | Yes |

---

## What Suppergeist Does NOT Depend On

- No internet connection at runtime
- No remote API keys or cloud services
- No external database server or daemon
- No separate backend process — the Java app is the backend

---

## Deployment Context

Suppergeist runs as a single desktop process on the user's machine. It is packaged via `jlink` into a self-contained image. The user is responsible for installing Ollama separately and pulling the model used by the app:

```bash
ollama pull qwen2.5:7b
```

```
User's Machine
├── Suppergeist (JavaFX desktop app)
├── Ollama (separate install, localhost:11434)
└── ~/.suppergeist/
    └── app.db   ← created on first run; contains plans, preferences, and CoFID nutrition data
```
