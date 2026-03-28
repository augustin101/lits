package com.example.lits.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.progressDataStore by preferencesDataStore(name = "progress")

class ProgressStore(private val context: Context) {

    // ── Level completion ──────────────────────────────────────────────────────

    private fun completedKey(gridSize: Int) = stringSetPreferencesKey("completed_$gridSize")

    fun completedLevels(gridSize: Int): Flow<Set<Int>> =
        context.progressDataStore.data.map { prefs ->
            prefs[completedKey(gridSize)]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun markCompleted(gridSize: Int, levelIndex: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[completedKey(gridSize)] ?: emptySet()
            prefs[completedKey(gridSize)] = current + levelIndex.toString()
        }
    }

    // ── In-progress cell states ───────────────────────────────────────────────
    //
    // Cell states are serialized as a flat string of digits, one per cell:
    //   '0' = EMPTY, '1' = SHADED, '2' = MARKED
    // A 10×10 grid produces a 100-character string.

    private fun stateKey(gridSize: Int, levelIndex: Int) =
        stringPreferencesKey("state_${gridSize}_${levelIndex}")

    fun startedLevels(gridSize: Int): Flow<Set<Int>> =
        context.progressDataStore.data.map { prefs ->
            prefs.asMap()
                .entries
                .filter { (key, value) ->
                    key.name.startsWith("state_${gridSize}_") && (value as? String)?.isNotEmpty() == true
                }
                .mapNotNull { (key, _) ->
                    key.name.removePrefix("state_${gridSize}_").toIntOrNull()
                }
                .toSet()
        }

    fun savedCellStates(gridSize: Int, levelIndex: Int): Flow<String?> =
        context.progressDataStore.data.map { prefs ->
            prefs[stateKey(gridSize, levelIndex)]
        }

    suspend fun saveCellStates(gridSize: Int, levelIndex: Int, encoded: String) {
        context.progressDataStore.edit { prefs ->
            prefs[stateKey(gridSize, levelIndex)] = encoded
        }
    }

    suspend fun clearCellStates(gridSize: Int, levelIndex: Int) {
        context.progressDataStore.edit { prefs ->
            prefs.remove(stateKey(gridSize, levelIndex))
        }
    }

    // ── Timer: in-progress elapsed seconds ───────────────────────────────────

    private fun elapsedKey(gridSize: Int, levelIndex: Int) =
        longPreferencesKey("timer_${gridSize}_${levelIndex}")

    fun loadElapsedTime(gridSize: Int, levelIndex: Int): Flow<Long> =
        context.progressDataStore.data.map { it[elapsedKey(gridSize, levelIndex)] ?: 0L }

    suspend fun saveElapsedTime(gridSize: Int, levelIndex: Int, seconds: Long) {
        context.progressDataStore.edit { it[elapsedKey(gridSize, levelIndex)] = seconds }
    }

    suspend fun clearElapsedTime(gridSize: Int, levelIndex: Int) {
        context.progressDataStore.edit { it.remove(elapsedKey(gridSize, levelIndex)) }
    }

    // ── Completion time (best time when solved) ───────────────────────────────

    private fun completionTimeKey(gridSize: Int, levelIndex: Int) =
        longPreferencesKey("ctime_${gridSize}_${levelIndex}")

    fun completionTimes(gridSize: Int): Flow<Map<Int, Long>> =
        context.progressDataStore.data.map { prefs ->
            prefs.asMap()
                .entries
                .filter { (key, _) -> key.name.startsWith("ctime_${gridSize}_") }
                .mapNotNull { (key, value) ->
                    val index = key.name.removePrefix("ctime_${gridSize}_").toIntOrNull()
                        ?: return@mapNotNull null
                    val time = value as? Long ?: return@mapNotNull null
                    index to time
                }
                .toMap()
        }

    suspend fun saveCompletionTime(gridSize: Int, levelIndex: Int, seconds: Long) {
        context.progressDataStore.edit { it[completionTimeKey(gridSize, levelIndex)] = seconds }
    }
}
