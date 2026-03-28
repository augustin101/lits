package com.example.lits.data

import android.util.Log
import com.example.lits.logic.Level
import com.example.lits.logic.LevelSchemaValidator
import kotlin.math.sqrt

/**
 * Parses a compact level string into a [Level].
 *
 * Format: a flat string of lowercase letters (a–z), read row-by-row left-to-right.
 * Each character maps to a region ID: 'a' → 0, 'b' → 1, …, 'z' → 25.
 * The grid size is derived from sqrt(length) — files must be square.
 *
 * Example 5×5 (25 chars): "aaabbaacbbddccbddccedeeee"
 */
object LevelParser {

    private const val TAG = "LevelParser"

    fun parseString(content: String, size: Int): Level? {
        val s = content.trim()
        if (s.length != size * size) {
            Log.w(TAG, "Expected ${size * size} chars for ${size}×${size}, got ${s.length}")
            return null
        }
        if (s.any { it !in 'a'..'z' }) {
            Log.w(TAG, "Level string contains invalid characters (only a–z allowed)")
            return null
        }

        val regionGrid = (0 until size).map { r ->
            (0 until size).map { c -> s[r * size + c] - 'a' }
        }
        val regionCount = (regionGrid.flatten().maxOrNull() ?: run {
            Log.w(TAG, "Could not determine regionCount")
            return null
        }) + 1

        val level = Level(size, regionGrid, regionCount)
        val validation = LevelSchemaValidator.validate(level)
        if (!validation.isValid) {
            Log.w(TAG, "Level failed schema validation:")
            validation.errors.forEach { Log.w(TAG, "  $it") }
            return null
        }
        return level
    }
}
