package com.example.lits.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val COLOR_COMPLETED = Color(0xFF4CAF50)
private val COLOR_STARTED   = Color(0xFFFF9800)
private val COLOR_INCOMPLETE = Color(0xFFEEEEEE)

@Composable
fun LevelSelectScreen(
    gridSize: Int,
    levelCount: Int,
    completedLevels: Set<Int>,
    startedLevels: Set<Int>,
    completionTimes: Map<Int, Long>,
    onLevelSelected: (levelIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
            }
            Text(
                text = "${gridSize}×${gridSize}  —  Levels",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(levelCount) { index ->
                LevelCell(
                    number = index + 1,
                    completed = index in completedLevels,
                    started = index in startedLevels,
                    completionTime = completionTimes[index],
                    onClick = { onLevelSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun LevelCell(
    number: Int,
    completed: Boolean,
    started: Boolean,
    completionTime: Long?,
    onClick: () -> Unit
) {
    val containerColor = when {
        completed -> COLOR_COMPLETED
        started   -> COLOR_STARTED
        else      -> COLOR_INCOMPLETE
    }
    val contentColor = if (completed || started) Color.White else Color(0xFF424242)
    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$number",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                if (completed && completionTime != null) {
                    Text(
                        text = completionTime.formatTime(),
                        fontSize = 9.sp,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun Long.formatTime(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
