# Data Directory

This folder contains all datasets used by Suppergeist.

## Structure

- `raw/`
  Original, unmodified source datasets.

- `processed/`
  Cleaned and transformed data used by the application (e.g. CSV exports, SQLite database).

- `scripts/` (outside this folder)
  Scripts used to transform raw data into processed data.

---

## Dataset: CoFID (UK Food Composition Data)

Source:
https://www.gov.uk/government/publications/composition-of-foods-integrated-dataset-cofid

File:
- raw/McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021.xlsx

This is the UK food composition dataset containing nutritional values for common foods.

---

## Processing Pipeline

The application does not use the raw Excel file directly.

Instead:

1. Raw Excel file is parsed
2. Data is cleaned and normalised
3. Additional fields are derived:
   - canonical_name (e.g. "apple")
   - preparation (e.g. "raw", "stewed")
   - qualifiers (e.g. "with sugar", "flesh only")
   - search_text (simplified text for lookup)
   - default_priority (used for ranking matches)
4. Cleaned data is exported to:
   - CSV (optional)
   - SQLite database (primary runtime format)

---

## Runtime Data

The application reads from:

- `processed/nutrition.db`

This database is treated as **read-only** at runtime.

---

## Rebuilding the Data

If the processed data is missing or needs to be regenerated:

1. Ensure the raw dataset exists in `raw/`
2. Run the conversion script:

   ```bash
   python scripts/convert_cofid.py
   ```
