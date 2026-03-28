package com.example.lits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lits.SettingsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // SettingsViewModel is created here so it's shared across all destinations
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onLevelSelected = { size -> navController.navigate("game/$size") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable(
            route = "game/{gridSize}",
            arguments = listOf(navArgument("gridSize") { type = NavType.IntType })
        ) {
            val hapticEnabled by settingsViewModel.hapticEnabled.collectAsState()
            val twoTapMode by settingsViewModel.twoTapMode.collectAsState()
            GameScreen(
                hapticEnabled = hapticEnabled,
                twoTapMode = twoTapMode,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            val hapticEnabled by settingsViewModel.hapticEnabled.collectAsState()
            val twoTapMode by settingsViewModel.twoTapMode.collectAsState()
            SettingsScreen(
                hapticEnabled = hapticEnabled,
                onHapticToggle = settingsViewModel::setHapticEnabled,
                twoTapMode = twoTapMode,
                onTwoTapModeToggle = settingsViewModel::setTwoTapMode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
