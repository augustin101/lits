package com.example.lits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lits.data.ProgressStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProgressViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProgressStore(app)

    fun completedLevels(gridSize: Int): Flow<Set<Int>> = store.completedLevels(gridSize)

    fun markCompleted(gridSize: Int, levelIndex: Int) {
        viewModelScope.launch { store.markCompleted(gridSize, levelIndex) }
    }
}
