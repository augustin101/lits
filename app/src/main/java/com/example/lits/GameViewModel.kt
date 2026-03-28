package com.example.lits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.lits.logic.CellState
import com.example.lits.logic.GameState
import com.example.lits.logic.Levels
import com.example.lits.logic.LitsValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val level = Levels.getLevel(savedStateHandle.get<Int>("gridSize") ?: 5)

    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private fun createInitialState(): GameState {
        val cellStates = List(level.size) { List(level.size) { CellState.EMPTY } }
        return GameState(
            level = level,
            cellStates = cellStates,
            validationResult = LitsValidator.validate(level, cellStates)
        )
    }

    fun setCellState(row: Int, col: Int, state: CellState) {
        val current = _gameState.value
        val newCellStates = current.cellStates.toMutableList().also { rows ->
            rows[row] = rows[row].toMutableList().also { it[col] = state }
        }
        _gameState.value = GameState(
            level = level,
            cellStates = newCellStates,
            validationResult = LitsValidator.validate(level, newCellStates)
        )
    }

    fun resetGame() {
        _gameState.value = createInitialState()
    }
}
