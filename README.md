# Suppergeist

Suppergeist is a local-first JavaFX desktop app for weekly meal planning. It uses a locally running Ollama model to generate meal plans from user preferences, saves plans to SQLite, and builds shopping lists from the selected ingredients.

## Requirements

- Java 21
- Ollama installed and running
- The Ollama `qwen2.5:7b` model

Install the model before generating meal plans:

```bash
ollama pull qwen2.5:7b
```

Make sure Ollama is available on its default local API endpoint:

```bash
ollama serve
```

The app sends generation requests to `http://localhost:11434/api/generate`.

## Running the App

Use the Gradle wrapper from the project root:

```bash
./gradlew run
```

On first launch, Suppergeist creates `app.db`, ensures a default user exists, and seeds initial app data if needed.

## Testing

Run the test suite with:

```bash
./gradlew test
```

The Gradle `check` task also runs JaCoCo coverage verification:

```bash
./gradlew check
```

## Data

Suppergeist uses a curated ingredient dataset backed by CoFID 2021 nutrition data. The app-facing ingredient data lives in:

```text
src/main/resources/data/ingredient_mapping.csv
```

Additional data pipeline notes and scripts are documented in [data/README.md](data/README.md).

## Project Structure

```text
src/main/java/com/example/suppergeist/
├── database/      SQLite setup and schema
├── model/         Domain models
├── repository/    Data access
├── service/       Meal planning, Ollama integration, shopping lists, nutrition
└── ui/            JavaFX controllers and view helpers
```

More technical detail is available in [docs/04_technical/architecture.md](docs/04_technical/architecture.md).
