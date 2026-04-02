import csv
import os
import sqlite3

DATA_DIR = os.path.dirname(os.path.dirname(__file__))
CSV_PATH = os.path.join(DATA_DIR, "processed", "nutrients.csv")
DB_PATH = os.path.join(DATA_DIR, "processed", "nutrients.db")

TEXT_COLUMNS = {"food_code", "food_name", "description"}


def col_type(col_name):
    return "TEXT" if col_name in TEXT_COLUMNS else "REAL"


def main():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print(f"Deleted existing database at {DB_PATH}")

    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)  # skip display names row
        snake_cols = next(reader)  # snake_case column names

        col_defs = ", ".join(
            f"{col} {col_type(col)} PRIMARY KEY" if col == "food_code" else f"{col} {col_type(col)}"
            for col in snake_cols
        )

        placeholders = ", ".join("?" * len(snake_cols))
        rows = ([val if val != "" else None for val in row] for row in reader)

        with sqlite3.connect(DB_PATH) as conn:
            conn.execute(f"CREATE TABLE foods ({col_defs})")
            conn.execute("CREATE INDEX idx_foods_food_name ON foods (food_name)")
            conn.executemany(f"INSERT INTO foods VALUES ({placeholders})", rows)
            row_count = conn.execute("SELECT COUNT(*) FROM foods").fetchone()[0]

    print(f"Created {DB_PATH} with {row_count} rows in 'foods' table")


if __name__ == "__main__":
    main()
