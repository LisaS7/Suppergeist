#!/usr/bin/env python3
"""
seed_ingredients.py

1. Reads data/ingredient_mapping.csv and upserts rows into the ingredients
   table in app.db (name + food_code).
2. Looks up each food_code in nutrients.db and writes the nutrition values
   back into the same ingredient rows in app.db.

Safe to re-run: existing rows are updated in place, not duplicated.

Usage (run from the project root):
    python3 data/scripts/seed_ingredients.py
    python3 data/scripts/seed_ingredients.py \
        --csv path/to/mapping.csv \
        --db  path/to/app.db \
        --nutrients-db path/to/nutrients.db
"""

import csv
import sqlite3
import argparse

DEFAULT_CSV         = "data/ingredient_mapping.csv"
DEFAULT_DB          = "app.db"
DEFAULT_NUTRIENTS   = "data/processed/nutrients.db"


# ---------------------------------------------------------------------------
# Step 1 — import CSV into app.db
# ---------------------------------------------------------------------------

def import_csv(conn: sqlite3.Connection, csv_path: str) -> int:
    rows = []
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for line in reader:
            name      = line["name"].strip()
            food_code = line["food_code"].strip() or None  # blank → NULL
            if name:
                rows.append((name, food_code))

    if not rows:
        print("No rows found in CSV — nothing imported.")
        return 0

    conn.executemany(
        """
        INSERT INTO ingredients (name, food_code)
        VALUES (?, ?)
        ON CONFLICT(name) DO UPDATE SET food_code = excluded.food_code
        """,
        rows,
    )
    return len(rows)


# ---------------------------------------------------------------------------
# Step 2 — add nutrition columns if the existing db pre-dates this script
# ---------------------------------------------------------------------------

def ensure_nutrition_columns(conn: sqlite3.Connection) -> None:
    existing = {row[1] for row in conn.execute("PRAGMA table_info(ingredients)")}
    for col in ("energy_kcal", "protein_g", "fat_g", "carbohydrate_g"):
        if col not in existing:
            conn.execute(f"ALTER TABLE ingredients ADD COLUMN {col} REAL")


# ---------------------------------------------------------------------------
# Step 3 — enrich ingredient rows with values from nutrients.db
# ---------------------------------------------------------------------------

def enrich_from_nutrients(conn: sqlite3.Connection, nutrients_db: str) -> tuple[int, int]:
    # Collect every food_code we need to look up
    ingredient_rows = conn.execute(
        "SELECT id, food_code FROM ingredients WHERE food_code IS NOT NULL"
    ).fetchall()

    if not ingredient_rows:
        return 0, 0

    food_codes = [row[1] for row in ingredient_rows]
    placeholders = ",".join("?" * len(food_codes))

    with sqlite3.connect(nutrients_db) as nutrients_conn:
        nutrient_rows = nutrients_conn.execute(
            f"""
            SELECT food_code, energy_kcal, protein_g, fat_g, carbohydrate_g
            FROM foods
            WHERE food_code IN ({placeholders})
            """,
            food_codes,
        ).fetchall()

    # Index by food_code for fast lookup
    nutrition_by_code = {row[0]: row[1:] for row in nutrient_rows}

    matched = 0
    for _, food_code in ingredient_rows:
        if food_code in nutrition_by_code:
            kcal, protein, fat, carbs = nutrition_by_code[food_code]
            conn.execute(
                """
                UPDATE ingredients
                SET energy_kcal = ?, protein_g = ?, fat_g = ?, carbohydrate_g = ?
                WHERE food_code = ?
                """,
                (kcal, protein, fat, carbs, food_code),
            )
            matched += 1

    return len(ingredient_rows), matched


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main(csv_path: str, db_path: str, nutrients_db: str) -> None:
    conn = sqlite3.connect(db_path)
    try:
        ensure_nutrition_columns(conn)

        count = import_csv(conn, csv_path)
        print(f"Upserted {count} ingredient(s) from CSV")

        total, matched = enrich_from_nutrients(conn, nutrients_db)
        print(f"Enriched {matched}/{total} ingredient(s) with nutrition data")

        unmatched = total - matched
        if unmatched:
            print(f"  {unmatched} food_code(s) not found in nutrients.db")

        conn.commit()
    finally:
        conn.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Import ingredient mapping and enrich with CoFID nutrition data."
    )
    parser.add_argument("--csv",          default=DEFAULT_CSV,       help="Path to ingredient_mapping.csv")
    parser.add_argument("--db",           default=DEFAULT_DB,        help="Path to app.db")
    parser.add_argument("--nutrients-db", default=DEFAULT_NUTRIENTS, help="Path to nutrients.db")
    args = parser.parse_args()
    main(args.csv, args.db, args.nutrients_db)
