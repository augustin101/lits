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

        // Group shaded cells by region
        val shadedByRegion = mutableMapOf<Int, MutableList<Cell>>()
        val allShaded = mutableListOf<Cell>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (cellStates[r][c] == CellState.SHADED) {
                    val regionId = level.regionGrid[r][c]
                    shadedByRegion.getOrPut(regionId) { mutableListOf() }.add(Cell(r, c))
                    allShaded.add(Cell(r, c))
                }
            }
        }

        // Validate each region
        val regionValidations = (0 until level.regionCount).associate { regionId ->
            val shaded = shadedByRegion[regionId] ?: emptyList()
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
        val isConnected = allShaded.isEmpty() || checkConnected(allShaded)

        // Check identical adjacent shapes
        val conflictingRegions = mutableSetOf<Int>()
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
                            conflictingRegions += regionA
                            conflictingRegions += regionB
                        }
                    }
                }
            }
        }

        val allRegionsValid = (0 until level.regionCount).all { regionValidations[it]?.isValid == true }
        val isSolved = allRegionsValid && isConnected && violating2x2Cells.isEmpty() && conflictingRegions.isEmpty()

        return ValidationResult(
            regionValidations = regionValidations,
            isConnected = isConnected,
            violating2x2Cells = violating2x2Cells,
            conflictingRegions = conflictingRegions,
            isSolved = isSolved
        )
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
