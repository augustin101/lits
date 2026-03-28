package com.example.lits.logic

object LitsValidator {

    private val I_SHAPES: Set<Set<Cell>> = setOf(
        setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3)),
        setOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0))
    )

    private val T_SHAPES: Set<Set<Cell>> = setOf(
        setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(1, 1)), // T down
        setOf(Cell(0, 1), Cell(1, 0), Cell(1, 1), Cell(1, 2)), // T up
        setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(2, 0)), // T right
        setOf(Cell(0, 1), Cell(1, 0), Cell(1, 1), Cell(2, 1))  // T left
    )

    // L and J (reflections treated as same type)
    private val L_SHAPES: Set<Set<Cell>> = setOf(
        setOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(2, 1)), // L
        setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(1, 0)), // L 90°
        setOf(Cell(0, 0), Cell(0, 1), Cell(1, 1), Cell(2, 1)), // L 180°
        setOf(Cell(0, 2), Cell(1, 0), Cell(1, 1), Cell(1, 2)), // L 270°
        setOf(Cell(0, 1), Cell(1, 1), Cell(2, 0), Cell(2, 1)), // J
        setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(1, 2)), // J 90°
        setOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(2, 0)), // J 180°
        setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(1, 2))  // J 270°
    )

    // S and Z (reflections treated as same type)
    private val S_SHAPES: Set<Set<Cell>> = setOf(
        setOf(Cell(0, 1), Cell(0, 2), Cell(1, 0), Cell(1, 1)), // S horizontal
        setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(2, 1)), // S vertical
        setOf(Cell(0, 0), Cell(0, 1), Cell(1, 1), Cell(1, 2)), // Z horizontal
        setOf(Cell(0, 1), Cell(1, 0), Cell(1, 1), Cell(2, 0))  // Z vertical
    )

    private fun normalize(cells: List<Cell>): Set<Cell> {
        val minRow = cells.minOf { it.row }
        val minCol = cells.minOf { it.col }
        return cells.map { Cell(it.row - minRow, it.col - minCol) }.toSet()
    }

    fun detectShape(cells: List<Cell>): PolyominoType? {
        if (cells.size != 4) return null
        val normalized = normalize(cells)
        return when {
            I_SHAPES.any { it == normalized } -> PolyominoType.I
            T_SHAPES.any { it == normalized } -> PolyominoType.T
            L_SHAPES.any { it == normalized } -> PolyominoType.L
            S_SHAPES.any { it == normalized } -> PolyominoType.S
            else -> null
        }
    }

    fun validate(level: Level, cellStates: List<List<CellState>>): ValidationResult {
        val size = level.size

        // Validate each region using pre-built regionCells lookup
        val regionValidations = (0 until level.regionCount).associate { regionId ->
            val shaded = level.regionCells[regionId]
                ?.filter { cellStates[it.row][it.col] == CellState.SHADED }
                ?: emptyList()
            val shapeType = if (shaded.size == 4) detectShape(shaded) else null
            regionId to RegionValidation(
                regionId = regionId,
                shadedCount = shaded.size,
                shapeType = shapeType,
                isValid = shapeType != null
            )
        }

        // Check 2x2 violations
        val violating2x2Cells = mutableSetOf<Cell>()
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                if (cellStates[r][c] == CellState.SHADED &&
                    cellStates[r][c + 1] == CellState.SHADED &&
                    cellStates[r + 1][c] == CellState.SHADED &&
                    cellStates[r + 1][c + 1] == CellState.SHADED
                ) {
                    violating2x2Cells += listOf(Cell(r, c), Cell(r, c + 1), Cell(r + 1, c), Cell(r + 1, c + 1))
                }
            }
        }

        // Check connectivity
        val allShaded = mutableListOf<Cell>()
        for (r in 0 until size) for (c in 0 until size)
            if (cellStates[r][c] == CellState.SHADED) allShaded.add(Cell(r, c))
        val isConnected = allShaded.isEmpty() || checkConnected(allShaded)

        // Check identical adjacent shapes — collect conflicting pairs
        val conflictingPairs = mutableSetOf<Pair<Int, Int>>()
        val checkedPairs = mutableSetOf<Pair<Int, Int>>()
        val directions = listOf(0 to 1, 1 to 0)
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (cellStates[r][c] != CellState.SHADED) continue
                val regionA = level.regionGrid[r][c]
                for ((dr, dc) in directions) {
                    val nr = r + dr; val nc = c + dc
                    if (nr < size && nc < size && cellStates[nr][nc] == CellState.SHADED) {
                        val regionB = level.regionGrid[nr][nc]
                        if (regionA == regionB) continue
                        val pair = if (regionA < regionB) regionA to regionB else regionB to regionA
                        if (pair in checkedPairs) continue
                        checkedPairs += pair
                        val typeA = regionValidations[regionA]?.shapeType
                        val typeB = regionValidations[regionB]?.shapeType
                        if (typeA != null && typeB != null && typeA == typeB) {
                            conflictingPairs += pair
                        }
                    }
                }
            }
        }

        val allRegionsValid = (0 until level.regionCount).all { regionValidations[it]?.isValid == true }
        val isSolved = allRegionsValid && isConnected && violating2x2Cells.isEmpty() && conflictingPairs.isEmpty()

        return ValidationResult(
            regionValidations = regionValidations,
            isConnected = isConnected,
            violating2x2Cells = violating2x2Cells,
            conflictingPairs = conflictingPairs,
            isSolved = isSolved
        )
    }

    /**
     * Incremental validation: only re-validates what could have changed after a single cell flip.
     * - Region validation: only the changed region (O(regionSize) instead of O(n²))
     * - 2×2 check: only the ≤4 windows that contain (changedRow, changedCol)
     * - Conflict pairs: remove all pairs involving changed region, then re-check its neighbors
     * - Connectivity: full BFS (unavoidable — any cell toggle can disconnect the graph)
     */
    fun validateIncremental(
        level: Level,
        cellStates: List<List<CellState>>,
        changedRow: Int,
        changedCol: Int,
        previous: ValidationResult
    ): ValidationResult {
        val size = level.size
        val changedRegionId = level.regionGrid[changedRow][changedCol]

        // --- Re-validate changed region ---
        val newRegionValidations = previous.regionValidations.toMutableMap()
        val shadedInRegion = level.regionCells[changedRegionId]
            ?.filter { cellStates[it.row][it.col] == CellState.SHADED }
            ?: emptyList()
        val shapeType = if (shadedInRegion.size == 4) detectShape(shadedInRegion) else null
        newRegionValidations[changedRegionId] = RegionValidation(
            regionId = changedRegionId,
            shadedCount = shadedInRegion.size,
            shapeType = shapeType,
            isValid = shapeType != null
        )

        // --- Update 2×2 violations: only windows that include (changedRow, changedCol) ---
        val newViolating2x2 = update2x2(previous.violating2x2Cells, cellStates, changedRow, changedCol, size)

        // --- Update conflict pairs: remove pairs involving changedRegion, re-check its border ---
        val newConflictingPairs = updateConflictPairs(
            level, cellStates, changedRegionId, newRegionValidations,
            previous.conflictingPairs, size
        )

        // --- Connectivity: full BFS (no shortcut) ---
        val allShaded = mutableListOf<Cell>()
        for (r in 0 until size) for (c in 0 until size)
            if (cellStates[r][c] == CellState.SHADED) allShaded.add(Cell(r, c))
        val isConnected = allShaded.isEmpty() || checkConnected(allShaded)

        val allRegionsValid = (0 until level.regionCount).all { newRegionValidations[it]?.isValid == true }
        val isSolved = allRegionsValid && isConnected && newViolating2x2.isEmpty() && newConflictingPairs.isEmpty()

        return ValidationResult(
            regionValidations = newRegionValidations,
            isConnected = isConnected,
            violating2x2Cells = newViolating2x2,
            conflictingPairs = newConflictingPairs,
            isSolved = isSolved
        )
    }

    /**
     * Recomputes 2×2 violations by:
     * 1. Removing cells from previous set that belong to any window touching (changedRow, changedCol).
     * 2. Re-checking those same ≤4 windows.
     */
    private fun update2x2(
        previous: Set<Cell>,
        cellStates: List<List<CellState>>,
        changedRow: Int,
        changedCol: Int,
        size: Int
    ): Set<Cell> {
        // Top-left corners of 2×2 windows that include (changedRow, changedCol)
        val windowTopLefts = listOf(
            changedRow - 1 to changedCol - 1,
            changedRow - 1 to changedCol,
            changedRow to changedCol - 1,
            changedRow to changedCol
        ).filter { (r, c) -> r in 0 until size - 1 && c in 0 until size - 1 }

        // Cells that might have changed status (all 4 cells of each affected window)
        val affectedCells = windowTopLefts.flatMapTo(mutableSetOf()) { (r, c) ->
            listOf(Cell(r, c), Cell(r, c + 1), Cell(r + 1, c), Cell(r + 1, c + 1))
        }

        val result = previous.toMutableSet()
        result.removeAll(affectedCells)

        for ((r, c) in windowTopLefts) {
            if (cellStates[r][c] == CellState.SHADED &&
                cellStates[r][c + 1] == CellState.SHADED &&
                cellStates[r + 1][c] == CellState.SHADED &&
                cellStates[r + 1][c + 1] == CellState.SHADED
            ) {
                result += listOf(Cell(r, c), Cell(r, c + 1), Cell(r + 1, c), Cell(r + 1, c + 1))
            }
        }
        return result
    }

    /**
     * Updates conflicting pairs by:
     * 1. Removing all previous pairs that involve changedRegionId.
     * 2. Re-checking all region borders adjacent to changedRegionId.
     */
    private fun updateConflictPairs(
        level: Level,
        cellStates: List<List<CellState>>,
        changedRegionId: Int,
        regionValidations: Map<Int, RegionValidation>,
        previousPairs: Set<Pair<Int, Int>>,
        size: Int
    ): Set<Pair<Int, Int>> {
        val result = previousPairs.filterTo(mutableSetOf()) { (a, b) ->
            a != changedRegionId && b != changedRegionId
        }

        // Find all region IDs adjacent (by shared edge) to changedRegionId
        val neighborRegions = mutableSetOf<Int>()
        val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        level.regionCells[changedRegionId]?.forEach { cell ->
            for ((dr, dc) in directions) {
                val nr = cell.row + dr; val nc = cell.col + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val neighborRegion = level.regionGrid[nr][nc]
                    if (neighborRegion != changedRegionId) neighborRegions += neighborRegion
                }
            }
        }

        // For each neighbor, check if the border is actually shaded on both sides
        val checkedPairs = mutableSetOf<Pair<Int, Int>>()
        for (neighborId in neighborRegions) {
            val pair = if (changedRegionId < neighborId) changedRegionId to neighborId
                       else neighborId to changedRegionId
            if (pair in checkedPairs) continue
            checkedPairs += pair

            val typeA = regionValidations[changedRegionId]?.shapeType
            val typeB = regionValidations[neighborId]?.shapeType
            if (typeA == null || typeB == null || typeA != typeB) continue

            // Verify they actually share a shaded border (regions might be adjacent but neither fully shaded)
            val sharesEdge = level.regionCells[changedRegionId]?.any { cell ->
                directions.any { (dr, dc) ->
                    val nr = cell.row + dr; val nc = cell.col + dc
                    nr in 0 until size && nc in 0 until size &&
                    level.regionGrid[nr][nc] == neighborId &&
                    cellStates[cell.row][cell.col] == CellState.SHADED &&
                    cellStates[nr][nc] == CellState.SHADED
                }
            } ?: false

            if (sharesEdge) result += pair
        }

        return result
    }

    private fun checkConnected(cells: List<Cell>): Boolean {
        val cellSet = cells.toHashSet()
        val visited = mutableSetOf(cells.first())
        val queue = ArrayDeque<Cell>().also { it.add(cells.first()) }
        val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for ((dr, dc) in directions) {
                val neighbor = Cell(current.row + dr, current.col + dc)
                if (neighbor in cellSet && neighbor !in visited) {
                    visited += neighbor
                    queue.add(neighbor)
                }
            }
        }
        return visited.size == cells.size
    }
}
