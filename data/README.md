# Data Directory

This folder contains all datasets used by Suppergeist.

## Structure

```
data/
├── raw/                        # Original, unmodified source files
├── scripts/                    # Data pipeline scripts (run from project root)
│   ├── enrich_ingredients.py
│   ├── seed_ingredients.py
│   └── seed_test_data.py
└── ingredient_mapping.csv      # Curated ingredient list with CoFID codes and nutrition data
```

---

## Source Dataset: CoFID 2021

**Source:** https://www.gov.uk/government/publications/composition-of-foods-integrated-dataset-cofid

**File:** `raw/McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx`

UK government food composition dataset. Sheets used:
- `1.3 Proximates` — macros (protein, fat, carbs, energy, sugars, fibre)
- `1.5 Vitamins` — micronutrients (vitamins A, C, D, E, B12, folate)

The raw file is used only by `enrich_ingredients.py`. It is never read at runtime.

---

## ingredient_mapping.csv

The only app-facing curated ingredient source. Contains the ingredients that
Suppergeist knows about, each paired with its CoFID `food_code` and full
nutritional profile.

**Adding a new ingredient:** manually add a row with `food_code` and `name`, then
run `enrich_ingredients.py` to populate the nutrition columns.

**Columns:** `food_code`, `name`, `energy_kcal`, `protein_g`, `fat_g`,
`carbohydrate_g`, `total_sugars_g`, `fibre_g`, `vitamin_a_µg`, `vitamin_c_mg`,
`vitamin_d_µg`, `vitamin_e_mg`, `vitamin_b12_µg`, `folate_µg`

---

## Scripts

All scripts are run from the **project root**, not from inside `data/scripts/`.

---

### `enrich_ingredients.py`

Reads the `food_code` values already present in `ingredient_mapping.csv`, looks
each one up in the raw CoFID Excel, and writes the nutrition columns back into
`ingredient_mapping.csv` in place.

Rules enforced:
- Only processes rows already present in `ingredient_mapping.csv`
- Only matches by existing `food_code` — no fuzzy matching, no new rows created
- Ingredients without a `food_code` are left with empty nutrition columns
- Prints a warning for any `food_code` not found in CoFID

Run this whenever:
- The raw CoFID source is updated, or
- A new ingredient row is manually added to `ingredient_mapping.csv`

```bash
python3 data/scripts/enrich_ingredients.py
```

**Input:** `data/raw/McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx`,
`data/ingredient_mapping.csv`  
**Output:** `data/ingredient_mapping.csv` (nutrition columns updated in place)

---

### `seed_ingredients.py`

Reads the enriched `ingredient_mapping.csv` and upserts all rows into the
`ingredients` table in `app.db`. Uses `ON CONFLICT DO UPDATE` so it is safe to
re-run without creating duplicates.

```bash
python3 data/scripts/seed_ingredients.py
```

**Input:** `data/ingredient_mapping.csv`, `app.db`  
**Output:** populated `ingredients` table in `app.db`

Optional flags:
```
--csv   path to ingredient_mapping.csv   (default: data/ingredient_mapping.csv)
--db    path to app.db                   (default: app.db)
```

---

### `seed_test_data.py`

Populates `app.db` with a minimal dataset for development:
- 1 user (`Test User`)
- 7 meals (one dinner per day)
- 1 meal plan for the week of 2026-04-06
- 7 meal plan entries

Uses `INSERT OR IGNORE` so it is safe to re-run without creating duplicates.

```bash
python3 data/scripts/seed_test_data.py
```

**Input:** `app.db` (must exist — the app creates it on first run)  
**Output:** rows in `users`, `meals`, `meal_plans`, `meal_plan_entries`

---

## Pipeline

### Enriching ingredient_mapping.csv from CoFID (occasional)

Run after adding new ingredients or when the CoFID source is updated:

```bash
python3 data/scripts/enrich_ingredients.py
```

### Seeding app.db (after app first run or schema reset)

```bash
python3 data/scripts/seed_ingredients.py
python3 data/scripts/seed_test_data.py
```

To reset `app.db` from scratch:

```bash
rm app.db
# Launch the app once to recreate app.db, then:
python3 data/scripts/seed_ingredients.py
python3 data/scripts/seed_test_data.py
```
