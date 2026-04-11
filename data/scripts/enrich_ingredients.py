#!/usr/bin/env python3
"""
enrich_ingredients.py

Reads food_codes from ingredient_mapping.csv, looks up the matching rows in
the raw CoFID Excel, and writes nutrition columns back into ingredient_mapping.csv.

Run this whenever the raw CoFID source changes or a new ingredient row is
manually added to ingredient_mapping.csv with a food_code.

Rules enforced:
  - Only rows already present in ingredient_mapping.csv are processed.
  - Only food_codes already present in those rows are looked up.
  - No new ingredient rows are added; no fuzzy matching is performed.
  - Ingredients whose food_code has no match in CoFID are left with empty
    nutrition columns and a warning is printed.

Usage (run from the project root):
    python3 data/scripts/enrich_ingredients.py
    python3 data/scripts/enrich_ingredients.py --xlsx path/to/CoFID.xlsx --csv data/ingredient_mapping.csv
"""

import csv
import os
import argparse

import openpyxl

DATA_DIR = os.path.dirname(os.path.dirname(__file__))
DEFAULT_XLSX = os.path.join(DATA_DIR, "raw", "McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx")
DEFAULT_CSV = os.path.join(DATA_DIR, "ingredient_mapping.csv")

PROXIMATES_COLUMNS = {
    "Food Code": "food_code",
    "Protein (g)": "protein_g",
    "Fat (g)": "fat_g",
    "Carbohydrate (g)": "carbohydrate_g",
    "Energy (kcal) (kcal)": "energy_kcal",
    "Total sugars (g)": "total_sugars_g",
    "AOAC fibre (g)": "fibre_g",
}

VITAMINS_COLUMNS = {
    "Food Code": "food_code",
    "Retinol Equivalent (µg)": "vitamin_a_µg",
    "Vitamin C (mg)": "vitamin_c_mg",
    "Vitamin D (µg)": "vitamin_d_µg",
    "Vitamin E (mg)": "vitamin_e_mg",
    "Vitamin B12 (µg)": "vitamin_b12_µg",
    "Folate (µg)": "folate_µg",
}

NUTRITION_COLS = [
    "energy_kcal", "protein_g", "fat_g", "carbohydrate_g",
    "total_sugars_g", "fibre_g",
    "vitamin_a_µg", "vitamin_c_mg", "vitamin_d_µg",
    "vitamin_e_mg", "vitamin_b12_µg", "folate_µg",
]


def clean_numeric(value):
    if value == "Tr":
        return 0.0
    if value in (None, "", "N"):
        return None
    try:
        return float(value)
    except (ValueError, TypeError):
        return None


def read_sheet(wb, sheet_name, column_map, target_food_codes):
    """Return a dict keyed by food_code containing only the columns in column_map."""
    ws = wb[sheet_name]
    rows = list(ws.iter_rows(values_only=True))

    header = rows[0]
    col_indices = {}
    for i, col_name in enumerate(header):
        if col_name in column_map:
            col_indices[col_name] = i

    missing = set(column_map.keys()) - set(col_indices.keys())
    if missing:
        raise ValueError(f"Sheet '{sheet_name}': columns not found: {missing}")

    records = {}
    for row in rows[3:]:  # rows 1-2 are code/description rows
        food_code = row[col_indices["Food Code"]]
        if food_code is None or food_code not in target_food_codes:
            continue
        record = {}
        for src_col, out_col in column_map.items():
            if out_col == "food_code":
                continue
            record[out_col] = clean_numeric(row[col_indices[src_col]])
        records[food_code] = record

    return records


def main(xlsx_path, csv_path):
    # 1. Read current ingredient_mapping.csv
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    target_food_codes = {r["food_code"] for r in rows if r.get("food_code")}
    print(f"Ingredients with a food_code: {len(target_food_codes)}")

    # 2. Read CoFID Excel — only rows matching our food_codes
    wb = openpyxl.load_workbook(xlsx_path, read_only=True, data_only=True)
    proximates = read_sheet(wb, "1.3 Proximates", PROXIMATES_COLUMNS, target_food_codes)
    vitamins = read_sheet(wb, "1.5 Vitamins", VITAMINS_COLUMNS, target_food_codes)
    wb.close()

    # 3. Merge nutrition onto each ingredient row
    output_fieldnames = ["food_code", "name"] + NUTRITION_COLS
    matched = 0
    unmatched = []

    for row in rows:
        food_code = row.get("food_code")
        for col in NUTRITION_COLS:
            row[col] = None

        if food_code:
            prox = proximates.get(food_code, {})
            vit = vitamins.get(food_code, {})
            if prox or vit:
                for col in NUTRITION_COLS:
                    row[col] = prox.get(col) if col in prox else vit.get(col)
                matched += 1
            else:
                unmatched.append(food_code)

    # 4. Write enriched CSV back in place
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=output_fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Enriched {matched} ingredient(s) with nutrition data.")
    if unmatched:
        print(f"WARNING: {len(unmatched)} food_code(s) not found in CoFID:")
        for code in unmatched:
            print(f"  {code}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Enrich ingredient_mapping.csv with CoFID nutrition data."
    )
    parser.add_argument("--xlsx", default=DEFAULT_XLSX, help="Path to CoFID Excel file")
    parser.add_argument("--csv", default=DEFAULT_CSV, help="Path to ingredient_mapping.csv")
    args = parser.parse_args()
    main(args.xlsx, args.csv)
