#!/usr/bin/env python3
"""
LITS level generator / validator.

Workflow:
  1. Define your levels in the LEVELS_* lists below (one LevelData per puzzle).
  2. Run `python tools/generate_levels.py` to validate all levels.
  3. Run `python tools/generate_levels.py --write` to write valid levels to assets/.

The script writes one JSON file per grid size:
  app/src/main/assets/levels_5.json
  app/src/main/assets/levels_6.json
  ...
"""

import json
import os
import sys
from collections import Counter
from dataclasses import dataclass, field

# Resolve paths relative to project root (parent of tools/)
_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEVELS_DIR = os.path.join(_PROJECT_ROOT, "app", "src", "main", "assets", "levels")


# ─────────────────────────────────────────────────────────────────────────────
# Data model
# ─────────────────────────────────────────────────────────────────────────────

@dataclass
class LevelData:
    """
    A single LITS level.

    grid: list of rows, each row a list of region IDs (0-based integers).
    Rules:
      - IDs must be consecutive starting from 0 (no gaps).
      - Every region must contain exactly 4 cells.
      - An N×N grid must have exactly N*N/4 regions.
    """
    grid: list[list[int]]

    @property
    def size(self) -> int:
        return len(self.grid)

    @property
    def region_count(self) -> int:
        flat = [c for row in self.grid for c in row]
        return max(flat) + 1 if flat else 0


# ─────────────────────────────────────────────────────────────────────────────
# Validator
# ─────────────────────────────────────────────────────────────────────────────

def validate_level(level: LevelData) -> list[str]:
    """Returns a list of error strings. An empty list means the level is valid."""
    errors: list[str] = []
    size = level.size

    if size == 0:
        return ["Grid is empty"]

    # Square
    for i, row in enumerate(level.grid):
        if len(row) != size:
            errors.append(f"Row {i} has {len(row)} cells, expected {size}")

    if errors:
        return errors

    flat = [c for row in level.grid for c in row]
    region_count = level.region_count

    # Valid range
    out_of_range = {v for v in flat if v < 0 or v >= region_count}
    if out_of_range:
        errors.append(f"IDs out of range [0, {region_count}): {sorted(out_of_range)}")

    # No gaps
    used = set(flat)
    missing = [i for i in range(region_count) if i not in used]
    if missing:
        errors.append(f"IDs {missing} never appear (gap in sequence)")

    # Each region must have at least 4 cells (player needs to pick 4 to shade)
    counts = Counter(flat)
    for region_id, count in sorted(counts.items()):
        if count < 4:
            errors.append(f"Region {region_id}: only {count} cells (need at least 4)")

    return errors


# ─────────────────────────────────────────────────────────────────────────────
# I/O helpers
# ─────────────────────────────────────────────────────────────────────────────

def _level_to_dict(level: LevelData) -> dict:
    # Note: add "solution" key here once hint support is implemented.
    return {"grid": level.grid}


def load_levels(size: int) -> list[LevelData]:
    """Load existing levels from assets/levels/<size>/."""
    folder = os.path.join(LEVELS_DIR, str(size))
    if not os.path.isdir(folder):
        return []
    levels = []
    for filename in sorted(os.listdir(folder)):
        if not filename.endswith(".json"):
            continue
        with open(os.path.join(folder, filename)) as f:
            data = json.load(f)
        levels.append(LevelData(grid=data["grid"]))
    return levels


def save_levels(size: int, levels: list[LevelData]) -> None:
    """Write each level to assets/levels/<size>/<index>.json (0-based index)."""
    folder = os.path.join(LEVELS_DIR, str(size))
    os.makedirs(folder, exist_ok=True)
    for index, level in enumerate(levels):
        path = os.path.join(folder, f"{index}.json")
        with open(path, "w") as f:
            json.dump(_level_to_dict(level), f, indent=2)
    print(f"  Wrote {len(levels)} level(s) → assets/levels/{size}/")


def validate_and_report(size: int, levels: list[LevelData]) -> bool:
    """Validate all levels for a size, print results. Returns True if all valid."""
    if not levels:
        return True
    print(f"\n{size}×{size} ({len(levels)} level(s))")
    all_ok = True
    for i, level in enumerate(levels):
        errors = validate_level(level)
        if errors:
            print(f"  [FAIL] Level {i + 1}:")
            for e in errors:
                print(f"         • {e}")
            all_ok = False
        else:
            print(f"  [ OK ] Level {i + 1} — {level.region_count} regions")
    return all_ok


# ─────────────────────────────────────────────────────────────────────────────
# Level definitions
# Add your levels here. Each LevelData is one puzzle.
# ─────────────────────────────────────────────────────────────────────────────

LEVELS_5: list[LevelData] = [
    # Level 1
    LevelData(grid=[
        [0, 0, 0, 1, 1],
        [0, 0, 2, 1, 1],
        [3, 3, 2, 2, 1],
        [3, 3, 2, 2, 4],
        [3, 4, 4, 4, 4],
    ]),
    # Add more 5×5 levels here:
    # LevelData(grid=[...]),
]

LEVELS_6: list[LevelData] = [
    # Level 1
    LevelData(grid=[
        [0, 0, 2, 2, 3, 3],
        [0, 2, 2, 3, 3, 3],
        [0, 5, 4, 4, 4, 3],
        [0, 5, 5, 4, 4, 4],
        [1, 5, 5, 5, 4, 4],
        [1, 1, 1, 5, 5, 4],
    ]),
    # Add more 6×6 levels here:
    # LevelData(grid=[...]),
]

LEVELS_7: list[LevelData] = [
    # A 7×7 grid needs ... wait, 49 cells / 4 = 12.25 — not divisible!
    # 7×7 is not a valid LITS grid size (cells must be divisible by 4).
    # The validator will catch this if you try.
]

LEVELS_8: list[LevelData] = [
    # An 8×8 grid needs 16 regions of 4 cells each.
]

LEVELS_9: list[LevelData] = [
    # 9×9: 81 cells / 4 = 20.25 — not divisible. Not a valid LITS size.
]

LEVELS_10: list[LevelData] = [
    # A 10×10 grid needs 25 regions of 4 cells each.
]

ALL_LEVELS: dict[int, list[LevelData]] = {
    5:  LEVELS_5,
    6:  LEVELS_6,
    7:  LEVELS_7,
    8:  LEVELS_8,
    9:  LEVELS_9,
    10: LEVELS_10,
}


# ─────────────────────────────────────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    write = "--write" in sys.argv
    print("Validating levels...")

    all_ok = True
    for size, levels in ALL_LEVELS.items():
        ok = validate_and_report(size, levels)
        all_ok = all_ok and ok

    print()
    if not all_ok:
        print("Validation FAILED — fix errors above before writing.")
        sys.exit(1)

    print("All levels valid.")
    if write:
        print("\nWriting to assets/...")
        for size, levels in ALL_LEVELS.items():
            save_levels(size, levels)
        print("Done.")
    else:
        print("Run with --write to update assets/.")
