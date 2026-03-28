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

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var saveJob: Job? = null

    init {
        check(level.size < 17) {
            "Grid size ${level.size} exceeds the maximum of 16 for 2-bit-per-cell UInt32 row encoding"
        }
        viewModelScope.launch {
            val savedEncoded = progressStore.savedCellStates(gridSize, levelIndex).first()
            val savedElapsed = progressStore.loadElapsedTime(gridSize, levelIndex).first()

            _elapsedSeconds.value = savedElapsed

            val restored = savedEncoded?.decodeCellStates(level.size)
            if (restored != null) {
                val result = LitsValidator.validate(level, restored)
                _gameState.value = GameState(level, restored, result)
                if (!result.isSolved) startTimer()
            } else {
                startTimer()
            }
        }
    }

    // ── Timer ──────────────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _elapsedSeconds.value++
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        viewModelScope.launch {
            progressStore.saveElapsedTime(gridSize, levelIndex, _elapsedSeconds.value)
        }
    }

    fun resumeTimer() {
        if (_gameState.value.validationResult.isSolved) return
        if (timerJob?.isActive == true) return
        startTimer()
    }

    // ── Cell interaction ──────────────────────────────────────────────────────

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
            timerJob?.cancel()
            timerJob = null
            val finalTime = _elapsedSeconds.value
            viewModelScope.launch {
                progressStore.clearCellStates(gridSize, levelIndex)
                progressStore.clearElapsedTime(gridSize, levelIndex)
                progressStore.saveCompletionTime(gridSize, levelIndex, finalTime)
            }
        } else {
            scheduleSave(newCellStates)
        }
    }

    fun resetGame() {
        saveJob?.cancel()
        timerJob?.cancel()
        timerJob = null
        _elapsedSeconds.value = 0L
        viewModelScope.launch {
            progressStore.clearCellStates(gridSize, levelIndex)
            progressStore.clearElapsedTime(gridSize, levelIndex)
        }
        _gameState.value = emptyState()
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        val state = _gameState.value
        if (!state.validationResult.isSolved) {
            saveJob?.cancel()
            viewModelScope.launch {
                progressStore.saveCellStates(gridSize, levelIndex, state.cellStates.encode())
                progressStore.saveElapsedTime(gridSize, levelIndex, _elapsedSeconds.value)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun emptyState(): GameState {
        val cellStates = List(level.size) { List(level.size) { CellState.EMPTY } }
        return GameState(
            level = level,
            cellStates = cellStates,
            validationResult = LitsValidator.validate(level, cellStates)
        )
    }

    private fun scheduleSave(cellStates: List<List<CellState>>) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1_000)
            progressStore.saveCellStates(gridSize, levelIndex, cellStates.encode())
        }
    }

    // Each row packed into one UInt32: 2 bits per cell, cell 0 at MSB pair.
    // Stored as a hex string: 8 chars per row, rows concatenated.

    private fun List<List<CellState>>.encode(): String =
        joinToString("") { row ->
            row.fold(0u) { acc, cell -> (acc shl 2) or cell.ordinal.toUInt() }
                .toString(16)
                .padStart(8, '0')
        }

    private fun String.decodeCellStates(size: Int): List<List<CellState>>? {
        if (length != size * 8) return null
        val values = CellState.values()
        return chunked(8).map { chunk ->
            var packed = chunk.toUIntOrNull(16) ?: return null
            val row = MutableList(size) { CellState.EMPTY }
            for (i in size - 1 downTo 0) {
                row[i] = values.getOrNull((packed and 0b11u).toInt()) ?: return null
                packed = packed shr 2
            }
            row.toList()
        }
    }
}
