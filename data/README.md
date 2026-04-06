# Data Directory

This folder contains all datasets used by Suppergeist.

## Structure

```
data/
├── raw/                        # Original, unmodified source files
├── processed/                  # Generated files used at runtime
│   ├── nutrients.csv           # Intermediate CSV (produced by process_nutrients.py)
│   └── nutrients.db            # CoFID SQLite database (read-only at runtime)
├── app/
│   └── schema.sql              # DDL for app.db (users, meals, plans, ingredients)
├── scripts/                    # Data pipeline scripts (run from project root)
│   ├── process_nutrients.py
│   ├── import_to_sqlite.py
│   ├── create_app_db.py
│   ├── seed_test_data.py
│   └── seed_ingredients.py
└── ingredient_mapping.csv      # Maps ingredient names to CoFID food_codes
```

---

## Source Dataset: CoFID 2021

**Source:** https://www.gov.uk/government/publications/composition-of-foods-integrated-dataset-cofid

**File:** `raw/McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx`

UK government food composition dataset. Sheets used:
- `1.3 Proximates` — macros (protein, fat, carbs, energy, sugars, fibre)
- `1.5 Vitamins` — micronutrients (vitamins A, C, D, E, B12, folate)

---

## Scripts

All scripts are run from the **project root**, not from inside `data/scripts/`.

---

### `process_nutrients.py`

Parses the raw CoFID Excel file and produces `data/processed/nutrients.csv`.

Merges data from the Proximates and Vitamins sheets on `Food Code`. The CSV has
two header rows: row 1 = display names, row 2 = snake_case column names (used as
database column names by the next script).

```bash
python3 data/scripts/process_nutrients.py
```

**Input:** `data/raw/McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx`  
**Output:** `data/processed/nutrients.csv`

---

### `import_to_sqlite.py`

Loads `nutrients.csv` into a SQLite database. Derives column types and schema
from the CSV headers automatically, then creates an index on `food_name`.
Deletes and recreates the database on every run.

```bash
python3 data/scripts/import_to_sqlite.py
```

**Input:** `data/processed/nutrients.csv`  
**Output:** `data/processed/nutrients.db` (the `foods` table, ~3000 rows)

---

### `create_app_db.py`

Creates `app.db` in the project root by applying `data/app/schema.sql`. Safe to
re-run only on a fresh (non-existent) `app.db` — it does not drop existing tables.

```bash
python3 data/scripts/create_app_db.py
```

**Input:** `data/app/schema.sql`  
**Output:** `app.db` (empty schema)

---

### `seed_test_data.py`

Populates `app.db` with a minimal dataset for development:
- 1 user (`Test User`)
- 7 meals (one dinner per day)
- 1 meal plan for the current week
- 7 meal plan entries

Uses `INSERT OR IGNORE` so it is safe to re-run without creating duplicates.

```bash
python3 data/scripts/seed_test_data.py
```

**Input:** `app.db` (must exist — run `create_app_db.py` first)  
**Output:** rows in `users`, `meals`, `meal_plans`, `meal_plan_entries`

---

### `seed_ingredients.py`

Two-step script:
1. Reads `data/ingredient_mapping.csv` and upserts rows into the `ingredients`
   table in `app.db` (name + CoFID `food_code`).
2. Looks up each `food_code` in `nutrients.db` and writes the full nutritional
   profile back into the same rows.

Nutrition columns populated: `energy_kcal`, `protein_g`, `fat_g`,
`carbohydrate_g`, `total_sugars_g`, `fibre_g`, `vitamin_a_µg`, `vitamin_c_mg`,
`vitamin_d_µg`, `vitamin_e_mg`, `vitamin_b12_µg`, `folate_µg`.

Also handles schema migrations: if `app.db` pre-dates this script and is missing
any nutrition columns, they are added automatically via `ALTER TABLE`.

Uses upserts (`ON CONFLICT DO UPDATE`) so it is safe to re-run.

```bash
python3 data/scripts/seed_ingredients.py
```

**Input:** `data/ingredient_mapping.csv`, `app.db`, `data/processed/nutrients.db`  
**Output:** populated `ingredients` table in `app.db`

Optional flags:
```
--csv          path to ingredient_mapping.csv   (default: data/ingredient_mapping.csv)
--db           path to app.db                   (default: app.db)
--nutrients-db path to nutrients.db             (default: data/processed/nutrients.db)
```

---

## Full Setup (from scratch)

To build everything from a clean state:

```bash
# 1. Build nutrients.db from the raw Excel file
python3 data/scripts/process_nutrients.py
python3 data/scripts/import_to_sqlite.py

# 2. Create and seed app.db
python3 data/scripts/create_app_db.py
python3 data/scripts/seed_test_data.py
python3 data/scripts/seed_ingredients.py
```

To rebuild just `app.db` (e.g. after a schema change):

```bash
rm app.db
python3 data/scripts/create_app_db.py
python3 data/scripts/seed_test_data.py
python3 data/scripts/seed_ingredients.py
```
