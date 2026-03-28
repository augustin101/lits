package com.example.lits.data

import android.content.Context
import android.util.Log
import com.example.lits.logic.Level
import com.example.lits.logic.LevelRepository

/**
 * Loads levels from `assets/levels/<size>/<index>.json`.
 *
 * - [getLevelCount] lists the asset directory and caches the result per size.
 * - [getLevel] loads and parses individual files on demand, caching each result.
 */
class LevelRepositoryImpl(private val context: Context) : LevelRepository {

    private val countCache = HashMap<Int, Int>()
    private val levelCache = HashMap<Pair<Int, Int>, Level>()

    override fun getLevelCount(size: Int): Int =
        countCache.getOrPut(size) {
            try {
                context.assets.list("levels/$size")?.size ?: 0
            } catch (e: Exception) {
                Log.w("LevelRepository", "Could not list levels for size $size: ${e.message}")
                0
            }
        }

    override fun getLevel(size: Int, index: Int): Level? {
        val key = size to index
        levelCache[key]?.let { return it }
        val level = loadLevel(size, index) ?: return null
        levelCache[key] = level
        return level
    }

    private fun loadLevel(size: Int, index: Int): Level? {
        return try {
            val content = context.assets
                .open("levels/$size/$index.txt")
                .bufferedReader()
                .readText()
            LevelParser.parseString(content, size)
        } catch (e: Exception) {
            Log.w("LevelRepository", "Failed to load level size=$size index=$index: ${e.message}")
            null
        }
    }
}
