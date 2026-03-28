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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lits.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lits.GameViewModel
import com.example.lits.logic.Cell
import com.example.lits.logic.CellState
import com.example.lits.logic.GameState
import com.example.lits.logic.PolyominoType


@Composable
fun GameScreen(
    hapticEnabled: Boolean = true,
    twoTapMode: Boolean = false,
    zenMode: Boolean = false,
    onBack: () -> Unit = {},
    onLevelSolved: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    // Pause timer when app goes to background, resume when it comes back
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseTimer()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeTimer()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(gameState.validationResult.isSolved) {
        if (gameState.validationResult.isSolved) onLevelSolved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        // Header pinned to the top, matching other screens
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), modifier = Modifier.size(28.dp))
            }
            Text(
                text = stringResource(R.string.game_title, gameState.level.size, viewModel.levelIndex + 1),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Game content centered in the remaining space
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!zenMode && gameState.validationResult.isSolved) {
                Surface(
                    color = ColorSolved,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.solved_label),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Hint + Reset on the left, timer on the right (timer hidden in zen mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { /* TODO: hint */ },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) { Text(stringResource(R.string.hint_button), fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = { viewModel.resetGame() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) { Text(stringResource(R.string.reset_button), fontSize = 13.sp) }
                }
                if (!zenMode) {
                    Text(
                        text = elapsedSeconds.formatTime(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = if (gameState.validationResult.isSolved)
                            ColorSolved else MaterialTheme.colorScheme.onSurface
                    )
                }
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

            if (!zenMode) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(gameState = gameState)
                Spacer(modifier = Modifier.height(8.dp))
                Legend()
            }
        }   // inner Column
    }       // outer Column
}

private fun Long.formatTime(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun StatusRow(gameState: GameState) {
    val result = gameState.validationResult
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(label = stringResource(R.string.status_connected), ok = result.isConnected)
        StatusChip(label = stringResource(R.string.status_no_2x2), ok = result.violating2x2Cells.isEmpty())
        StatusChip(label = stringResource(R.string.status_no_twins), ok = result.conflictingRegions.isEmpty())
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    Surface(
        color = if (ok) ColorStatusOkBackground else ColorStatusErrorBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = if (ok) ColorStatusOkText else ColorStatusErrorText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem(stringResource(R.string.shape_l), ColorShapeL)
        LegendItem(stringResource(R.string.shape_i), ColorShapeI)
        LegendItem(stringResource(R.string.shape_t), ColorShapeT)
        LegendItem(stringResource(R.string.shape_s), ColorShapeS)
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
                            PolyominoType.L -> ColorShapeL
                            PolyominoType.I -> ColorShapeI
                            PolyominoType.T -> ColorShapeT
                            PolyominoType.S -> ColorShapeS
                            null -> ColorCellShaded
                        }
                    }
                    state == CellState.SHADED -> ColorCellShaded
                    else -> ColorCellEmpty
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
                                color = ColorErrorStripe,
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
                        color = ColorCellMark,
                        start = Offset(c * cellPx + pad, r * cellPx + pad),
                        end = Offset((c + 1) * cellPx - pad, (r + 1) * cellPx - pad),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = ColorCellMark,
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
                        color = if (isBoundary) ColorGridLineThick else ColorGridLineThin,
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
                        color = if (isBoundary) ColorGridLineThick else ColorGridLineThin,
                        start = Offset(c * cellPx, (r + 1) * cellPx),
                        end = Offset((c + 1) * cellPx, (r + 1) * cellPx),
                        strokeWidth = if (isBoundary) 4f else 1.5f
                    )
                }
            }
        }

        // Outer border
        drawRect(
            color = ColorGridLineThick,
            topLeft = Offset(0f, 0f),
            size = Size(gridSize * cellPx, gridSize * cellPx),
            style = Stroke(width = 4f)
        )
    }
}
