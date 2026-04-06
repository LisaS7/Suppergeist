#!/usr/bin/env python3
"""
seed_test_data.py

Seeds app.db with a minimal but complete set of test data:
  - 1 user
  - 7 meals (one per day, dinner only — keeps it simple)
  - 1 meal plan starting 2026-04-06 (Monday)
  - 7 meal_plan_entries, one per day

Safe to re-run: existing rows are skipped (INSERT OR IGNORE).

Usage (run from the project root):
    python3 data/scripts/seed_test_data.py
    python3 data/scripts/seed_test_data.py --db path/to/app.db
"""

import argparse
import sqlite3
from pathlib import Path

DEFAULT_DB = Path("app.db")
WEEK_START = "2026-04-06"

MEALS = [
    "Spaghetti Bolognese",
    "Chicken and Vegetable Stir-Fry",
    "Lentil and Sweet Potato Curry",
    "Grilled Salmon with Roasted Veg",
    "Mushroom Risotto",
    "Black Bean Tacos",
    "Roast Chicken with Potatoes",
]


def seed(db_path: Path) -> None:
    conn = sqlite3.connect(db_path)
    conn.execute("PRAGMA foreign_keys = ON")

    with conn:
        # --- user ---
        conn.execute(
            "INSERT OR IGNORE INTO users (id, name) VALUES (1, 'Test User')"
        )
        user_id = 1

        # --- meals ---
        meal_ids = []
        for name in MEALS:
            cur = conn.execute(
                "INSERT OR IGNORE INTO meals (name) VALUES (?)", (name,)
            )
            if cur.lastrowid:
                meal_ids.append(cur.lastrowid)
            else:
                row = conn.execute(
                    "SELECT id FROM meals WHERE name = ?", (name,)
                ).fetchone()
                meal_ids.append(row[0])

        # --- meal plan ---
        conn.execute(
            "INSERT OR IGNORE INTO meal_plans (user_id, start_date) VALUES (?, ?)",
            (user_id, WEEK_START),
        )
        plan_id = conn.execute(
            "SELECT id FROM meal_plans WHERE user_id = ? AND start_date = ?",
            (user_id, WEEK_START),
        ).fetchone()[0]

        # --- meal plan entries (one dinner per day) ---
        for day_offset, meal_id in enumerate(meal_ids):
            conn.execute(
                """
                INSERT OR IGNORE INTO meal_plan_entries
                    (meal_plan_id, meal_id, day_offset, meal_type)
                VALUES (?, ?, ?, 'dinner')
                """,
                (plan_id, meal_id, day_offset),
            )

    conn.close()
    print(f"Seeded {db_path}: 1 user, {len(MEALS)} meals, 1 plan, {len(MEALS)} entries.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed app.db with test data.")
    parser.add_argument("--db", type=Path, default=DEFAULT_DB)
    args = parser.parse_args()

    if not args.db.exists():
        print(f"ERROR: {args.db} not found. Run create_app_db.py first.")
        raise SystemExit(1)

    seed(args.db)


if __name__ == "__main__":
    main()
