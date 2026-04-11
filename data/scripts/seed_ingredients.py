#!/usr/bin/env python3
"""
seed_ingredients.py

Reads the enriched ingredient_mapping.csv (which already contains nutrition
columns) and upserts all rows into the ingredients table in app.db.

Safe to re-run: existing rows are updated in place, not duplicated.

Usage (run from the project root):
    python3 data/scripts/seed_ingredients.py
    python3 data/scripts/seed_ingredients.py --csv path/to/mapping.csv --db path/to/app.db
"""

import csv
import sqlite3
import argparse
from pathlib import Path

DEFAULT_CSV = Path("src/main/resources/data/ingredient_mapping.csv")
DEFAULT_DB  = Path("app.db")

NUTRITION_COLS = [
    "energy_kcal",
    "protein_g",
    "fat_g",
    "carbohydrate_g",
    "total_sugars_g",
    "fibre_g",
    "vitamin_a_µg",
    "vitamin_c_mg",
    "vitamin_d_µg",
    "vitamin_e_mg",
    "vitamin_b12_µg",
    "folate_µg",
]


def to_float(value):
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (ValueError, TypeError):
        return None


def main(csv_path: Path, db_path: Path) -> None:
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    if not rows:
        print("No rows found in CSV — nothing imported.")
        return

    set_sql = ", ".join(f"{col} = excluded.{col}" for col in ["food_code"] + NUTRITION_COLS)
    col_list = ", ".join(["name", "food_code"] + NUTRITION_COLS)
    placeholders = ", ".join(["?"] * (2 + len(NUTRITION_COLS)))

    records = []
    for row in rows:
        name      = row["name"].strip()
        food_code = row["food_code"].strip() or None
        nutrition = [to_float(row.get(col)) for col in NUTRITION_COLS]
        if name:
            records.append((name, food_code, *nutrition))

    with sqlite3.connect(db_path) as conn:
        conn.executemany(
            f"""
            INSERT INTO ingredients ({col_list})
            VALUES ({placeholders})
            ON CONFLICT(name) DO UPDATE SET {set_sql}
            """,
            records,
        )

    print(f"Upserted {len(records)} ingredient(s) into {db_path}.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Seed app.db ingredients table from enriched ingredient_mapping.csv."
    )
    parser.add_argument("--csv", type=Path, default=DEFAULT_CSV, help="Path to ingredient_mapping.csv")
    parser.add_argument("--db",  type=Path, default=DEFAULT_DB,  help="Path to app.db")
    args = parser.parse_args()
    main(args.csv, args.db)
