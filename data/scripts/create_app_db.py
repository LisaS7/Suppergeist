"""
Creates app.db in the project root and applies data/app/schema.sql.
Run from the project root:
    python data/scripts/create_app_db.py
"""

import sqlite3
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DB_PATH = PROJECT_ROOT / "app.db"
SCHEMA_PATH = PROJECT_ROOT / "data" / "app" / "schema.sql"


def main():
    schema = SCHEMA_PATH.read_text(encoding="utf-8")

    conn = sqlite3.connect(DB_PATH)
    try:
        conn.executescript(schema)
        conn.commit()
        print(f"Created {DB_PATH}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
