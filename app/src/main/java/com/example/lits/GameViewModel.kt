package com.example.lits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.lits.data.ProgressStore
import com.example.lits.logic.CellState
import com.example.lits.logic.GameState
import com.example.lits.logic.LitsValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val gridSize: Int = savedStateHandle.get<Int>("gridSize") ?: 5
    val levelIndex: Int = savedStateHandle.get<Int>("levelIndex") ?: 0

    private val level = requireNotNull(
        (application as LitsApp).levelRepository.getLevel(gridSize, levelIndex)
    ) { "No level defined for size=$gridSize index=$levelIndex" }

    private val progressStore = ProgressStore(application)

    private val _gameState = MutableStateFlow(emptyState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        // Restore in-progress state asynchronously.
        // The grid shows empty briefly (typically < 100ms) before the saved state loads.
        viewModelScope.launch {
            val encoded = progressStore.savedCellStates(gridSize, levelIndex).first()
            val restored = encoded?.decodeCellStates(level.size)
            if (restored != null) {
                _gameState.value = GameState(
                    level = level,
                    cellStates = restored,
                    validationResult = LitsValidator.validate(level, restored)
                )
            }
        }
    }

    private fun emptyState(): GameState {
        val cellStates = List(level.size) { List(level.size) { CellState.EMPTY } }
        return GameState(
            level = level,
            cellStates = cellStates,
            validationResult = LitsValidator.validate(level, cellStates)
        )
    }

    // ── Cell interaction ──────────────────────────────────────────────────────

    private var saveJob: Job? = null

    fun setCellState(row: Int, col: Int, state: CellState) {
        val current = _gameState.value
        val newCellStates = current.cellStates.toMutableList().also { rows ->
            rows[row] = rows[row].toMutableList().also { it[col] = state }
        }
        val newGameState = GameState(
            level = level,
            cellStates = newCellStates,
            validationResult = LitsValidator.validateIncremental(
                level, newCellStates, row, col, current.validationResult
            )
        )
        _gameState.value = newGameState

        if (newGameState.validationResult.isSolved) {
            saveJob?.cancel()
            viewModelScope.launch { progressStore.clearCellStates(gridSize, levelIndex) }
        } else {
            scheduleSave(newCellStates)
        }
    }

    fun resetGame() {
        saveJob?.cancel()
        viewModelScope.launch { progressStore.clearCellStates(gridSize, levelIndex) }
        _gameState.value = emptyState()
    }

    override fun onCleared() {
        super.onCleared()
        // Flush any pending debounced save immediately when the ViewModel is destroyed.
        saveJob?.cancel()
        val state = _gameState.value
        if (!state.validationResult.isSolved) {
            viewModelScope.launch {
                progressStore.saveCellStates(gridSize, levelIndex, state.cellStates.encode())
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun scheduleSave(cellStates: List<List<CellState>>) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1_000)
            progressStore.saveCellStates(gridSize, levelIndex, cellStates.encode())
        }
    }

    private fun List<List<CellState>>.encode(): String =
        flatten().joinToString("") { it.ordinal.toString() }

    private fun String.decodeCellStates(size: Int): List<List<CellState>>? {
        if (length != size * size) return null
        val values = CellState.values()
        return chunked(size).map { row ->
            row.map { ch ->
                val ord = ch.digitToIntOrNull() ?: return null
                values.getOrNull(ord) ?: return null
            }
        }
    }
}
