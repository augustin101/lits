package com.example.lits.logic

// ─────────────────────────────────────────────────────────────────────────────
// 5×5 levels
// ─────────────────────────────────────────────────────────────────────────────

private val level5x5_1 = Level(
    size = 5,
    regionGrid = listOf(
        listOf(0, 0, 0, 1, 1),
        listOf(0, 0, 2, 1, 1),
        listOf(3, 3, 2, 2, 1),
        listOf(3, 3, 2, 2, 4),
        listOf(3, 4, 4, 4, 4)
    ),
    regionCount = 5
)

// Add more 5×5 levels here following the same pattern:
// private val level5x5_2 = Level(size = 5, regionGrid = listOf(...), regionCount = ?)

private val levels5 = listOf(level5x5_1)

// ─────────────────────────────────────────────────────────────────────────────
// 6×6 levels
// ─────────────────────────────────────────────────────────────────────────────

/*
 * Region map for level 6×6 #1:
 *   ? ? ? ? ? ?
 *   ? ? ? ? ? ?
 *   ? ? ? ? ? ?
 *   ? ? ? ? ? ?
 *   ? ? ? ? ? ?
 *   ? ? ? ? ? ?
 *
 * Replace each '?' with the region ID (0-based) that cell belongs to.
 * A 6×6 grid has 36 cells; with 9 regions of 4 cells each = 36 cells.
 * regionCount should match the number of distinct IDs used.
 */
private val level6x6_1 = Level(
    size = 6,
    regionGrid = listOf(
        listOf(0, 0, 2, 2, 3, 3), // row 0 — fill in real IDs
        listOf(0, 2, 2, 3, 3, 3), // row 1
        listOf(0, 5, 4, 4, 4, 3), // row 2
        listOf(0, 5, 5, 4, 4, 4), // row 3
        listOf(1, 5, 5, 5, 4, 4), // row 4
        listOf(1, 1, 1, 5, 5, 4)  // row 5
    ),
    regionCount = 1 // update once real IDs are filled in
)

// Add more 6×6 levels here:
// private val level6x6_2 = Level(size = 6, regionGrid = listOf(...), regionCount = ?)

private val levels6 = listOf(level6x6_1)

// ─────────────────────────────────────────────────────────────────────────────
// 7×7 levels  (add your levels above the list)
// ─────────────────────────────────────────────────────────────────────────────

private val levels7 = emptyList<Level>()

// ─────────────────────────────────────────────────────────────────────────────
// 8×8 levels
// ─────────────────────────────────────────────────────────────────────────────

private val levels8 = emptyList<Level>()

// ─────────────────────────────────────────────────────────────────────────────
// 9×9 levels
// ─────────────────────────────────────────────────────────────────────────────

private val levels9 = emptyList<Level>()

// ─────────────────────────────────────────────────────────────────────────────
// 10×10 levels
// ─────────────────────────────────────────────────────────────────────────────

private val levels10 = emptyList<Level>()

// ─────────────────────────────────────────────────────────────────────────────
// Public interface
// ─────────────────────────────────────────────────────────────────────────────

object Levels {
    private fun levelsForSize(size: Int): List<Level> = when (size) {
        5  -> levels5
        6  -> levels6
        7  -> levels7
        8  -> levels8
        9  -> levels9
        10 -> levels10
        else -> emptyList()
    }

    fun getLevelCount(size: Int): Int = levelsForSize(size).size

    /** Returns null when the index is out of range (no level defined yet). */
    fun getLevel(size: Int, index: Int): Level? = levelsForSize(size).getOrNull(index)
}
