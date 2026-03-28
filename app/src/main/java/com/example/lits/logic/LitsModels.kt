package com.example.lits.logic

import androidx.compose.runtime.Immutable

enum class CellState { EMPTY, SHADED, MARKED }

enum class PolyominoType { L, I, T, S }

@Immutable
data class Cell(val row: Int, val col: Int)

@Immutable
data class Level(
    val size: Int,
    val regionGrid: List<List<Int>>,
    val regionCount: Int
) {
    // Pre-built lookup: regionId → all cells in that region.
    // Computed once at construction via lazy to avoid O(n²) scan on every incremental validation.
    val regionCells: Map<Int, List<Cell>> by lazy {
        val map = HashMap<Int, MutableList<Cell>>(regionCount)
        for (r in 0 until size) for (c in 0 until size)
            map.getOrPut(regionGrid[r][c]) { mutableListOf() }.add(Cell(r, c))
        map
    }
}

@Immutable
data class RegionValidation(
    val regionId: Int,
    val shadedCount: Int,
    val shapeType: PolyominoType?,
    val isValid: Boolean
)

@Immutable
data class ValidationResult(
    val regionValidations: Map<Int, RegionValidation>,
    val isConnected: Boolean,
    val violating2x2Cells: Set<Cell>,
    // Pairs of adjacent region IDs sharing the same valid shape type.
    // Stored as pairs so incremental updates can remove only affected pairs.
    val conflictingPairs: Set<Pair<Int, Int>>,
    val isSolved: Boolean
) {
    // Derived from conflictingPairs — UI reads this, so no UI changes needed.
    val conflictingRegions: Set<Int> =
        conflictingPairs.flatMapTo(mutableSetOf()) { (a, b) -> listOf(a, b) }
}

@Immutable
data class GameState(
    val level: Level,
    val cellStates: List<List<CellState>>,
    val validationResult: ValidationResult
)
