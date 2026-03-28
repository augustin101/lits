#!/usr/bin/env python3
"""
LITS level generator / validator.

Workflow:
  1. Define your levels in the LEVELS_* lists below using compact strings.
  2. Run `python tools/generate_levels.py` to validate all levels.
  3. Run `python tools/generate_levels.py --write` to write valid levels to assets/.

Level string format:
  A flat string of lowercase letters (a–z), read row-by-row left-to-right.
  Each character is a region ID: 'a' → 0, 'b' → 1, …, 'z' → 25.
  The grid size is derived from sqrt(len(string)).

  Example 5×5:  "aaabbaacbbddccbddccedeeee"
  Example 6×6:  "aaccddaccdddafeeedaffeeebfffeebbbffe"
"""

import os
import sys
from collections import Counter
from dataclasses import dataclass
from math import isqrt

# Resolve paths relative to project root (parent of tools/)
_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEVELS_DIR = os.path.join(_PROJECT_ROOT, "app", "src", "main", "assets", "levels")


# ─────────────────────────────────────────────────────────────────────────────
# Data model
# ─────────────────────────────────────────────────────────────────────────────

@dataclass
class LevelData:
    """
    A single LITS level, stored as a compact flat string.

    Each character is a region ID: 'a' → 0, 'b' → 1, …, 'z' → 25.
    The string is read row-by-row left-to-right; size = sqrt(len(s)).
    """
    s: str

    @property
    def size(self) -> int:
        return isqrt(len(self.s))

    @property
    def grid(self) -> list[list[int]]:
        n = self.size
        ids = [ord(c) - ord('a') for c in self.s]
        return [ids[r * n:(r + 1) * n] for r in range(n)]

    @property
    def region_count(self) -> int:
        return max(ord(c) - ord('a') for c in self.s) + 1 if self.s else 0


# ─────────────────────────────────────────────────────────────────────────────
# Validator
# ─────────────────────────────────────────────────────────────────────────────

def validate_level(level: LevelData) -> list[str]:
    """Returns a list of error strings. An empty list means the level is valid."""
    errors: list[str] = []

    if not level.s:
        return ["Level string is empty"]

    # Must be a-z only
    bad = {c for c in level.s if c not in 'abcdefghijklmnopqrstuvwxyz'}
    if bad:
        errors.append(f"Invalid characters: {sorted(bad)}")
        return errors

    n = isqrt(len(level.s))
    if n * n != len(level.s):
        errors.append(f"String length {len(level.s)} is not a perfect square")
        return errors

    flat = [ord(c) - ord('a') for c in level.s]
    region_count = level.region_count

    # No gaps in region IDs
    used = set(flat)
    missing = [i for i in range(region_count) if i not in used]
    if missing:
        errors.append(f"Region IDs {missing} never appear (gap in sequence)")

    # Each region must have at least 4 cells
    counts = Counter(flat)
    for region_id, count in sorted(counts.items()):
        if count < 4:
            errors.append(
                f"Region '{chr(ord('a') + region_id)}' ({region_id}): "
                f"only {count} cells (need at least 4)"
            )

    return errors


# ─────────────────────────────────────────────────────────────────────────────
# I/O helpers
# ─────────────────────────────────────────────────────────────────────────────

def load_levels(size: int) -> list[LevelData]:
    """Load existing levels from assets/levels/<size>/."""
    folder = os.path.join(LEVELS_DIR, str(size))
    if not os.path.isdir(folder):
        return []
    levels = []
    for filename in sorted(os.listdir(folder)):
        if not filename.endswith(".txt"):
            continue
        with open(os.path.join(folder, filename)) as f:
            levels.append(LevelData(s=f.read().strip()))
    return levels


def save_levels(size: int, levels: list[LevelData]) -> None:
    """Write each level to assets/levels/<size>/<index>.txt (0-based index)."""
    folder = os.path.join(LEVELS_DIR, str(size))
    os.makedirs(folder, exist_ok=True)
    for index, level in enumerate(levels):
        path = os.path.join(folder, f"{index}.txt")
        with open(path, "w") as f:
            f.write(level.s)
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
    LevelData("aaabbaacbbddccbddccedeeee"),
    # Add more 5×5 levels here:
    # LevelData("..."),
]

LEVELS_6: list[LevelData] = [
    # Level 1
    LevelData("aaccddaccdddafeeedaffeeebfffeebbbffe"),
    # Add more 6×6 levels here:
    # LevelData("..."),
]

LEVELS_8: list[LevelData] = [
    # An 8×8 grid needs 16 regions of 4 cells each.
    # LevelData("..."),  # 64 chars
]

LEVELS_10: list[LevelData] = [
    # A 10×10 grid needs 25 regions of 4 cells each.
    # LevelData("..."),  # 100 chars
]

ALL_LEVELS: dict[int, list[LevelData]] = {
    5:  LEVELS_5,
    6:  LEVELS_6,
    8:  LEVELS_8,
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
