package com.example.lits.data

import android.content.Context
import androidx.datastore.preferences.core.edit
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
}
