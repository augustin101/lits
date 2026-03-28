package com.example.lits.logic

/**
 * Validates that a [Level]'s data is structurally correct for a LITS puzzle:
 *   - regionGrid is size × size
 *   - region IDs are 0-based with no gaps (0 until regionCount)
 *   - every region contains exactly 4 cells
 *
 * This is separate from [LitsValidator], which validates a player's in-progress solution.
 */
object LevelSchemaValidator {

    data class Result(val isValid: Boolean, val errors: List<String>)

    fun validate(level: Level): Result {
        val errors = mutableListOf<String>()
        val size = level.size

        // Square grid
        if (level.regionGrid.size != size)
            errors += "Grid has ${level.regionGrid.size} rows, expected $size"
        for ((i, row) in level.regionGrid.withIndex())
            if (row.size != size) errors += "Row $i has ${row.size} columns, expected $size"

        if (errors.isNotEmpty()) return Result(false, errors)

        val allIds = level.regionGrid.flatten()

        // IDs in valid range
        val outOfRange = allIds.filter { it < 0 || it >= level.regionCount }
        if (outOfRange.isNotEmpty())
            errors += "IDs out of range [0, ${level.regionCount}): ${outOfRange.toSet()}"

        // No gaps — all IDs 0 until regionCount must appear at least once
        val usedIds = allIds.toSet()
        val missing = (0 until level.regionCount).filter { it !in usedIds }
        if (missing.isNotEmpty())
            errors += "regionCount=${level.regionCount} but IDs $missing are never used"

        // Each region must have at least 4 cells (player needs to pick 4 to shade)
        val countPer = allIds.groupingBy { it }.eachCount()
        for ((id, count) in countPer.entries.sortedBy { it.key })
            if (count < 4) errors += "Region $id has only $count cells — need at least 4"

        return Result(errors.isEmpty(), errors)
    }
}
