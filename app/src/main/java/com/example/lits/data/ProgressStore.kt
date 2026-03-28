package com.example.lits.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.progressDataStore by preferencesDataStore(name = "progress")

class ProgressStore(private val context: Context) {

    private fun key(gridSize: Int) = stringSetPreferencesKey("completed_$gridSize")

    fun completedLevels(gridSize: Int): Flow<Set<Int>> =
        context.progressDataStore.data.map { prefs ->
            prefs[key(gridSize)]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun markCompleted(gridSize: Int, levelIndex: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[key(gridSize)] ?: emptySet()
            prefs[key(gridSize)] = current + levelIndex.toString()
        }
    }
}
