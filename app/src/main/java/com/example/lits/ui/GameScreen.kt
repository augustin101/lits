package com.example.lits.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import android.view.HapticFeedbackConstants
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lits.GameViewModel
import com.example.lits.logic.Cell
import com.example.lits.logic.CellState
import com.example.lits.logic.GameState
import com.example.lits.logic.PolyominoType

// Shape colors per CLAUDE.md
private val COLOR_L = Color(0xFF4A90E2)
private val COLOR_I = Color(0xFF00BCD4)
private val COLOR_T = Color(0xFF9C27B0)
private val COLOR_S = Color(0xFF4CAF50)
private val COLOR_SHADED = Color(0xFF707070)
private val COLOR_ERROR_STRIPE = Color(0xCCEF9A9A) // muted pastel red, semi-transparent
private val COLOR_EMPTY = Color(0xFFEEEEEE)
private val COLOR_GRID_LINE_THIN = Color(0xFFBDBDBD)
private val COLOR_GRID_LINE_THICK = Color(0xFF212121)

@Composable
fun GameScreen(
    hapticEnabled: Boolean = true,
    twoTapMode: Boolean = false,
    onBack: () -> Unit = {},
    onLevelSolved: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()

    LaunchedEffect(gameState.validationResult.isSolved) {
        if (gameState.validationResult.isSolved) onLevelSolved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "${gameState.level.size}×${gameState.level.size}  —  Level ${viewModel.levelIndex + 1}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (gameState.validationResult.isSolved) {
            Surface(
                color = Color(0xFF4CAF50),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SOLVED!",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
        ) {
            GameGrid(
                gameState = gameState,
                hapticEnabled = hapticEnabled,
                twoTapMode = twoTapMode,
                onSetCellState = viewModel::setCellState
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusRow(gameState = gameState)

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { viewModel.resetGame() }) {
            Text("Reset")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Legend()
    }
}

@Composable
private fun StatusRow(gameState: GameState) {
    val result = gameState.validationResult
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(label = "Connected", ok = result.isConnected)
        StatusChip(label = "No 2×2", ok = result.violating2x2Cells.isEmpty())
        StatusChip(label = "No twins", ok = result.conflictingRegions.isEmpty())
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    Surface(
        color = if (ok) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = if (ok) Color(0xFF388E3C) else Color(0xFFC62828),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem("L", COLOR_L)
        LegendItem("I", COLOR_I)
        LegendItem("T", COLOR_T)
        LegendItem("S", COLOR_S)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(14.dp), shape = RoundedCornerShape(2.dp), color = color) {}
        Text(
            text = " $label",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GameGrid(
    gameState: GameState,
    hapticEnabled: Boolean = true,
    twoTapMode: Boolean = false,
    onSetCellState: (row: Int, col: Int, state: CellState) -> Unit
) {
    val level = gameState.level
    val gridSize = level.size
    val view = LocalView.current
    val currentCellStates = rememberUpdatedState(gameState.cellStates)
    val currentTwoTapMode = rememberUpdatedState(twoTapMode)
    val currentHapticEnabled = rememberUpdatedState(hapticEnabled)

    fun vibrate() {
        if (currentHapticEnabled.value) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val cellPx = size.width.toFloat() / gridSize
                        val col = (offset.x / cellPx).toInt().coerceIn(0, gridSize - 1)
                        val row = (offset.y / cellPx).toInt().coerceIn(0, gridSize - 1)
                        val current = currentCellStates.value[row][col]
                        val next = if (currentTwoTapMode.value) {
                            when (current) {
                                CellState.EMPTY -> CellState.SHADED
                                CellState.SHADED -> CellState.EMPTY
                                CellState.MARKED -> CellState.EMPTY
                            }
                        } else {
                            when (current) {
                                CellState.EMPTY -> CellState.SHADED
                                CellState.SHADED -> CellState.MARKED
                                CellState.MARKED -> CellState.EMPTY
                            }
                        }
                        vibrate()
                        onSetCellState(row, col, next)
                    },
                    onLongPress = { offset ->
                        if (!currentTwoTapMode.value) return@detectTapGestures
                        val cellPx = size.width.toFloat() / gridSize
                        val col = (offset.x / cellPx).toInt().coerceIn(0, gridSize - 1)
                        val row = (offset.y / cellPx).toInt().coerceIn(0, gridSize - 1)
                        vibrate()
                        onSetCellState(row, col, CellState.MARKED)
                    }
                )
            }
    ) {
        // 'size' is DrawScope.size (Size, floats). Use gridSize for loop bounds.
        val cellPx = size.width / gridSize

        // Draw cell fills
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val state = gameState.cellStates[r][c]
                val regionId = level.regionGrid[r][c]
                val regionVal = gameState.validationResult.regionValidations[regionId]
                val cell = Cell(r, c)

                val isError = cell in gameState.validationResult.violating2x2Cells ||
                        regionId in gameState.validationResult.conflictingRegions

                val fillColor = when {
                    state == CellState.SHADED && regionVal?.isValid == true -> {
                        when (regionVal.shapeType) {
                            PolyominoType.L -> COLOR_L
                            PolyominoType.I -> COLOR_I
                            PolyominoType.T -> COLOR_T
                            PolyominoType.S -> COLOR_S
                            null -> COLOR_SHADED
                        }
                    }
                    state == CellState.SHADED -> COLOR_SHADED
                    else -> COLOR_EMPTY
                }

                drawRect(
                    color = fillColor,
                    topLeft = Offset(c * cellPx, r * cellPx),
                    size = Size(cellPx, cellPx)
                )

                // Draw error stripes on top of cell color
                if (state == CellState.SHADED && isError) {
                    val left = c * cellPx
                    val top = r * cellPx
                    val right = (c + 1) * cellPx
                    val bottom = (r + 1) * cellPx
                    clipRect(left, top, right, bottom) {
                        val step = cellPx / 5f
                        var x = left - cellPx
                        while (x < right + cellPx) {
                            drawLine(
                                color = COLOR_ERROR_STRIPE,
                                start = Offset(x, bottom),
                                end = Offset(x + cellPx, top),
                                strokeWidth = cellPx * 0.06f
                            )
                            x += step
                        }
                    }
                }

                // Draw X for MARKED state
                if (state == CellState.MARKED) {
                    val pad = cellPx * 0.28f
                    val strokeW = (cellPx * 0.08f).coerceAtLeast(3f)
                    drawLine(
                        color = Color(0xFF757575),
                        start = Offset(c * cellPx + pad, r * cellPx + pad),
                        end = Offset((c + 1) * cellPx - pad, (r + 1) * cellPx - pad),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = Color(0xFF757575),
                        start = Offset((c + 1) * cellPx - pad, r * cellPx + pad),
                        end = Offset(c * cellPx + pad, (r + 1) * cellPx - pad),
                        strokeWidth = strokeW
                    )
                }
            }
        }

        // Draw internal grid lines (thin within region, thick between regions)
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val regionId = level.regionGrid[r][c]

                // Right border
                if (c < gridSize - 1) {
                    val rightRegion = level.regionGrid[r][c + 1]
                    val isBoundary = rightRegion != regionId
                    drawLine(
                        color = if (isBoundary) COLOR_GRID_LINE_THICK else COLOR_GRID_LINE_THIN,
                        start = Offset((c + 1) * cellPx, r * cellPx),
                        end = Offset((c + 1) * cellPx, (r + 1) * cellPx),
                        strokeWidth = if (isBoundary) 4f else 1.5f
                    )
                }

                // Bottom border
                if (r < gridSize - 1) {
                    val bottomRegion = level.regionGrid[r + 1][c]
                    val isBoundary = bottomRegion != regionId
                    drawLine(
                        color = if (isBoundary) COLOR_GRID_LINE_THICK else COLOR_GRID_LINE_THIN,
                        start = Offset(c * cellPx, (r + 1) * cellPx),
                        end = Offset((c + 1) * cellPx, (r + 1) * cellPx),
                        strokeWidth = if (isBoundary) 4f else 1.5f
                    )
                }
            }
        }

        // Outer border
        drawRect(
            color = COLOR_GRID_LINE_THICK,
            topLeft = Offset(0f, 0f),
            size = Size(gridSize * cellPx, gridSize * cellPx),
            style = Stroke(width = 4f)
        )
    }
}
