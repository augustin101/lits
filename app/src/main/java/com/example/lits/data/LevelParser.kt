package com.example.lits.data

import android.util.Log
import com.example.lits.logic.Level
import com.example.lits.logic.LevelSchemaValidator
import org.json.JSONObject

/**
 * Parses a single-level JSON file into a [Level].
 *
 * Expected format:
 * ```json
 * {
 *   "grid": [[0,1,1,2], [0,0,1,2], ...]
 * }
 * ```
 * `size` and `regionCount` are derived from the grid.
 * Returns null and logs a warning if schema validation fails.
 */
object LevelParser {

    private const val TAG = "LevelParser"

    fun parseSingle(json: String): Level? {
        val obj = JSONObject(json)
        val gridArray = obj.getJSONArray("grid")

        if (gridArray.length() == 0) {
            Log.w(TAG, "Empty grid")
            return null
        }

        val regionGrid = (0 until gridArray.length()).map { r ->
            val row = gridArray.getJSONArray(r)
            (0 until row.length()).map { c -> row.getInt(c) }
        }

        val regionCount = (regionGrid.flatten().maxOrNull() ?: run {
            Log.w(TAG, "Could not determine regionCount")
            return null
        }) + 1

        val level = Level(regionGrid.size, regionGrid, regionCount)
        val validation = LevelSchemaValidator.validate(level)
        if (!validation.isValid) {
            Log.w(TAG, "Level failed schema validation:")
            validation.errors.forEach { Log.w(TAG, "  $it") }
            return null
        }
        return level
    }
}
