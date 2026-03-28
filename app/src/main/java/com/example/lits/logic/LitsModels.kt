package com.example.lits.logic

enum class CellState { EMPTY, SHADED, MARKED }

enum class PolyominoType { L, I, T, S }

data class Cell(val row: Int, val col: Int)

data class Level(
    val size: Int,
    val regionGrid: List<List<Int>>,
    val regionCount: Int
)

data class RegionValidation(
    val regionId: Int,
    val shadedCount: Int,
    val shapeType: PolyominoType?,
    val isValid: Boolean
)

data class ValidationResult(
    val regionValidations: Map<Int, RegionValidation>,
    val isConnected: Boolean,
    val violating2x2Cells: Set<Cell>,
    val conflictingRegions: Set<Int>,
    val isSolved: Boolean
)

data class GameState(
    val level: Level,
    val cellStates: List<List<CellState>>,
    val validationResult: ValidationResult
)
