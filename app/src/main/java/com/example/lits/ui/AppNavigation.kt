package com.example.lits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lits.LitsApp
import com.example.lits.ProgressViewModel
import com.example.lits.SettingsViewModel
import com.example.lits.logic.LevelRepository

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val progressViewModel: ProgressViewModel = viewModel()
    val levelRepository = (LocalContext.current.applicationContext as LitsApp).levelRepository

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onSizeSelected = { size -> navController.navigate("levels/$size") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable(
            route = "levels/{gridSize}",
            arguments = listOf(navArgument("gridSize") { type = NavType.IntType })
        ) { backStackEntry ->
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 5
            val completedLevels by progressViewModel.completedLevels(gridSize)
                .collectAsState(initial = emptySet())
            val startedLevels by progressViewModel.startedLevels(gridSize)
                .collectAsState(initial = emptySet())
            val completionTimes by progressViewModel.completionTimes(gridSize)
                .collectAsState(initial = emptyMap())
            LevelSelectScreen(
                gridSize = gridSize,
                levelCount = levelRepository.getLevelCount(gridSize),
                completedLevels = completedLevels,
                startedLevels = startedLevels,
                completionTimes = completionTimes,
                onLevelSelected = { index -> navController.navigate("game/$gridSize/$index") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "game/{gridSize}/{levelIndex}",
            arguments = listOf(
                navArgument("gridSize") { type = NavType.IntType },
                navArgument("levelIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 5
            val levelIndex = backStackEntry.arguments?.getInt("levelIndex") ?: 0
            val hapticEnabled by settingsViewModel.hapticEnabled.collectAsState()
            val twoTapMode by settingsViewModel.twoTapMode.collectAsState()
            val zenMode by settingsViewModel.zenMode.collectAsState()
            GameScreen(
                hapticEnabled = hapticEnabled,
                twoTapMode = twoTapMode,
                zenMode = zenMode,
                onBack = { navController.popBackStack() },
                onLevelSolved = { progressViewModel.markCompleted(gridSize, levelIndex) }
            )
        }
        composable("settings") {
            val hapticEnabled by settingsViewModel.hapticEnabled.collectAsState()
            val twoTapMode by settingsViewModel.twoTapMode.collectAsState()
            val zenMode by settingsViewModel.zenMode.collectAsState()
            SettingsScreen(
                hapticEnabled = hapticEnabled,
                onHapticToggle = settingsViewModel::setHapticEnabled,
                twoTapMode = twoTapMode,
                onTwoTapModeToggle = settingsViewModel::setTwoTapMode,
                zenMode = zenMode,
                onZenModeToggle = settingsViewModel::setZenMode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
