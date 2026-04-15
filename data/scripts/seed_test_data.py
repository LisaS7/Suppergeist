#!/usr/bin/env python3
"""
seed_test_data.py

Seeds app.db with a minimal but complete set of test data:
  - 7 meals (one per day, dinner only — keeps it simple)
  - 1 meal plan starting 2026-04-06 (Monday)
  - 7 meal_plan_entries, one per day

Safe to re-run: existing rows are skipped (INSERT OR IGNORE).

The database must already exist (created by the app on first launch).

Usage (run from the project root):
    python3 data/scripts/seed_test_data.py
    python3 data/scripts/seed_test_data.py --db path/to/app.db
    python3 data/scripts/seed_test_data.py --clear   # wipe meals/plans/entries
    python3 data/scripts/seed_test_data.py --reset   # clear then reseed
"""

import argparse
import sqlite3
from pathlib import Path

DEFAULT_DB = Path.home() / ".suppergeist" / "app.db"
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

# Ingredient search terms use SQL LIKE patterns — partial matches keep this robust
# against CoFID naming variations. Each entry is (like_pattern, quantity, unit).
MEAL_INGREDIENTS = {
    "Spaghetti Bolognese": [
        ("%spaghetti%", 100.0, "g"),
        ("%beef%mince%", 150.0, "g"),
        ("%tomatoes%raw%", 200.0, "g"),
        ("%onions%raw%", 80.0, "g"),
        ("%olive oil%", 15.0, "ml"),
    ],
    "Chicken and Vegetable Stir-Fry": [
        ("%chicken%breast%raw%", 200.0, "g"),
        ("%carrots%raw%", 100.0, "g"),
        ("%broccoli%raw%", 100.0, "g"),
        ("%soy sauce%", 20.0, "ml"),
        ("%rapeseed oil%", 10.0, "ml"),
    ],
    "Lentil and Sweet Potato Curry": [
        ("%lentils%raw%", 150.0, "g"),
        ("%sweet potato%raw%", 200.0, "g"),
        ("%onions%raw%", 100.0, "g"),
        ("%tomatoes%raw%", 150.0, "g"),
    ],
    "Grilled Salmon with Roasted Veg": [
        ("%salmon%raw%", 180.0, "g"),
        ("%courgette%raw%", 150.0, "g"),
        ("%peppers%raw%", 100.0, "g"),
        ("%olive oil%", 15.0, "ml"),
    ],
    "Mushroom Risotto": [
        ("%rice%white%raw%", 150.0, "g"),
        ("%mushrooms%raw%", 200.0, "g"),
        ("%onions%raw%", 80.0, "g"),
        ("%butter%", 20.0, "g"),
        ("%parmesan%", 30.0, "g"),
    ],
    "Black Bean Tacos": [
        ("%black-eyed beans%", 200.0, "g"),
        ("%peppers%raw%", 120.0, "g"),
        ("%onions%raw%", 80.0, "g"),
        ("%tortilla%", 2.0, "wraps"),
    ],
    "Roast Chicken with Potatoes": [
        ("%chicken%whole%raw%", 400.0, "g"),
        ("%potatoes%raw%", 300.0, "g"),
        ("%carrots%raw%", 150.0, "g"),
        ("%olive oil%", 20.0, "ml"),
    ],
}


def clear(conn: sqlite3.Connection) -> None:
    """Delete only the rows inserted by this script (known meals, plan, and entries)."""
    placeholders = ",".join("?" * len(MEALS))
    meal_ids = [
        row[0]
        for row in conn.execute(
            f"SELECT id FROM meals WHERE name IN ({placeholders})", MEALS
        ).fetchall()
    ]

    if not meal_ids:
        print("Nothing to clear (no known seed meals found).")
        return

    user_row = conn.execute("SELECT id FROM users LIMIT 1").fetchone()
    plan_id_row = (
        conn.execute(
            "SELECT id FROM meal_plans WHERE user_id = ? AND start_date = ?",
            (user_row[0], WEEK_START),
        ).fetchone()
        if user_row
        else None
    )
    plan_id = plan_id_row[0] if plan_id_row else None

    with conn:
        entry_placeholders = ",".join("?" * len(meal_ids))
        params: list = list(meal_ids)
        conn.execute(
            f"DELETE FROM meal_ingredients WHERE meal_id IN ({entry_placeholders})",
            params,
        )
        if plan_id is not None:
            conn.execute(
                f"DELETE FROM meal_plan_entries WHERE meal_plan_id = ? OR meal_id IN ({entry_placeholders})",
                [plan_id] + params,
            )
            conn.execute("DELETE FROM meal_plans WHERE id = ?", (plan_id,))
        else:
            conn.execute(
                f"DELETE FROM meal_plan_entries WHERE meal_id IN ({entry_placeholders})",
                params,
            )
        conn.execute(f"DELETE FROM meals WHERE id IN ({entry_placeholders})", params)

    print("Cleared seed data: meal_ingredients, meal_plan_entries, meal_plans, meals.")


def seed(db_path: Path) -> None:
    conn = sqlite3.connect(db_path)
    conn.execute("PRAGMA foreign_keys = ON")

    with conn:
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
        user_row = conn.execute("SELECT id FROM users LIMIT 1").fetchone()
        if user_row is None:
            print("ERROR: No user found. Launch the app once to create the default user.")
            raise SystemExit(1)
        user_id = user_row[0]

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

        # --- meal ingredients ---
        ingredients_inserted = 0
        ingredients_skipped = 0
        for meal_name, meal_id in zip(MEALS, meal_ids):
            for pattern, quantity, unit in MEAL_INGREDIENTS.get(meal_name, []):
                row = conn.execute(
                    "SELECT id FROM ingredients WHERE name LIKE ? LIMIT 1", (pattern,)
                ).fetchone()
                if row is None:
                    print(f"  SKIP: no ingredient matched '{pattern}' for '{meal_name}'")
                    ingredients_skipped += 1
                    continue
                conn.execute(
                    """
                    INSERT OR IGNORE INTO meal_ingredients (meal_id, ingredient_id, quantity, unit)
                    VALUES (?, ?, ?, ?)
                    """,
                    (meal_id, row[0], quantity, unit),
                )
                ingredients_inserted += 1

    conn.close()
    print(
        f"Seeded {db_path}: {len(MEALS)} meals, 1 plan, {len(MEALS)} entries, "
        f"{ingredients_inserted} ingredient rows ({ingredients_skipped} skipped — no match in ingredients table)."
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed app.db with test data.")
    parser.add_argument("--db", type=Path, default=DEFAULT_DB)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--clear", action="store_true", help="Delete all meals, plans, and entries.")
    mode.add_argument("--reset", action="store_true", help="Clear then reseed.")
    args = parser.parse_args()

    if not args.db.exists():
        print(f"ERROR: {args.db} not found. Launch the app once to initialise the database.")
        raise SystemExit(1)

    if args.clear:
        conn = sqlite3.connect(args.db)
        conn.execute("PRAGMA foreign_keys = ON")
        clear(conn)
        conn.close()
    elif args.reset:
        conn = sqlite3.connect(args.db)
        conn.execute("PRAGMA foreign_keys = ON")
        clear(conn)
        conn.close()
        seed(args.db)
    else:
        seed(args.db)


if __name__ == "__main__":
    main()
