import openpyxl
import csv
import os
import re

DATA_DIR = os.path.dirname(os.path.dirname(__file__))
XLSX_PATH = os.path.join(DATA_DIR, "raw", "McCance_Widdowsons_Composition_of_Foods_Integrated_Dataset_2021..xlsx")
OUTPUT_PATH = os.path.join(DATA_DIR, "processed", "nutrients.csv")

PROXIMATES_COLUMNS = {
    "Food Code": "Food Code",
    "Food Name": "Food Name",
    "Description": "Description",
    "Protein (g)": "Protein (g)",
    "Fat (g)": "Fat (g)",
    "Carbohydrate (g)": "Carbohydrate (g)",
    "Energy (kcal) (kcal)": "Energy (kcal)",
    "Total sugars (g)": "Total sugars (g)",
    "AOAC fibre (g)": "Fibre (g)",
}

VITAMINS_COLUMNS = {
    "Food Code": "Food Code",
    "Retinol Equivalent (µg)": "Vitamin A (µg)",
    "Vitamin C (mg)": "Vitamin C (mg)",
    "Vitamin D (µg)": "Vitamin D (µg)",
    "Vitamin E (mg)": "Vitamin E (mg)",
    "Vitamin B12 (µg)": "Vitamin B12 (µg)",
    "Folate (µg)": "Folate (µg)",
}

# Columns where values should remain as strings (not converted to float)
STRING_COLUMNS = {"Food Code", "Food Name", "Description"}


def to_snake_case(name):
    name = re.sub(r"[^\w\s]", "", name)
    name = name.strip().lower()
    name = re.sub(r"\s+", "_", name)
    return name


def clean_value(col_name, value):
    if col_name in STRING_COLUMNS:
        return value if value not in (None, "", "N") else None
    if value == "Tr":
        return 0.0
    if value in (None, "", "N"):
        return None
    try:
        return float(value)
    except (ValueError, TypeError):
        return None


def read_sheet(wb, sheet_name, column_map):
    ws = wb[sheet_name]
    rows = list(ws.iter_rows(values_only=True))

    # Row 0 is the header row with full column names
    header = rows[0]
    col_indices = {}
    for i, col_name in enumerate(header):
        if col_name in column_map:
            col_indices[col_name] = i

    missing = set(column_map.keys()) - set(col_indices.keys())
    if missing:
        raise ValueError(f"Sheet '{sheet_name}': columns not found: {missing}")

    # Data starts at row 3 (rows 1 and 2 are code/description rows)
    records = {}
    for row in rows[3:]:
        food_code = row[col_indices["Food Code"]]
        if food_code is None:
            continue
        record = {}
        for src_col, out_col in column_map.items():
            record[out_col] = clean_value(out_col, row[col_indices[src_col]])
        records[food_code] = record

    return records


def main():
    wb = openpyxl.load_workbook(XLSX_PATH, read_only=True, data_only=True)

    proximates = read_sheet(wb, "1.3 Proximates", PROXIMATES_COLUMNS)
    vitamins = read_sheet(wb, "1.5 Vitamins", VITAMINS_COLUMNS)

    # Merge on Food Code
    all_food_codes = sorted(set(proximates.keys()) | set(vitamins.keys()))

    proximate_out_cols = list(PROXIMATES_COLUMNS.values())
    vitamin_out_cols = [v for k, v in VITAMINS_COLUMNS.items() if k != "Food Code"]
    display_fieldnames = proximate_out_cols + vitamin_out_cols
    snake_fieldnames = [to_snake_case(col) for col in display_fieldnames]

    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    with open(OUTPUT_PATH, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(display_fieldnames)
        writer.writerow(snake_fieldnames)
        for food_code in all_food_codes:
            row = {}
            row.update(proximates.get(food_code, {col: None for col in proximate_out_cols}))
            for col in vitamin_out_cols:
                row[col] = vitamins.get(food_code, {}).get(col)
            writer.writerow([row[col] for col in display_fieldnames])

    print(f"Written {len(all_food_codes)} rows to {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
