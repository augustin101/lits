package com.example.lits.logic

object Levels {
    /**
     * 5x5 grid with 5 regions (5 cells each).
     *
     * Region map:
     *   0 0 0 1 1
     *   0 0 2 1 1
     *   3 3 2 2 1
     *   3 3 2 2 4
     *   3 4 4 4 4
     */
    val level1 = Level(
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

    fun getLevel(size: Int): Level = when (size) {
        5 -> level1
        else -> generatePlaceholderLevel(size)
    }

    /**
     * Generates a simple placeholder level for any grid size by assigning
     * cells to regions in row-major order, 5 cells per region.
     */
    private fun generatePlaceholderLevel(size: Int): Level {
        val regionGrid = Array(size) { IntArray(size) }
        var regionId = 0
        var count = 0
        val totalCells = size * size
        var cellIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                regionGrid[r][c] = regionId
                count++
                cellIndex++
                if (count == 5 && cellIndex < totalCells) {
                    regionId++
                    count = 0
                }
            }
        }
        return Level(
            size = size,
            regionGrid = regionGrid.map { it.toList() },
            regionCount = regionId + 1
        )
    }
}
