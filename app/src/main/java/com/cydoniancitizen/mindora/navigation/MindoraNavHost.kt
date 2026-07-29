package com.cydoniancitizen.mindora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cydoniancitizen.mindora.feature.breathing.BreathingExerciseScreen
import com.cydoniancitizen.mindora.feature.history.HistoryScreen
import com.cydoniancitizen.mindora.feature.home.HomeScreen
import com.cydoniancitizen.mindora.feature.freemeditation.FreeMeditationScreen
import com.cydoniancitizen.mindora.feature.practice.PracticeScreen
import com.cydoniancitizen.mindora.feature.settings.SettingsScreen

@Composable
fun MindoraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MindoraDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(MindoraDestination.HOME.route) {
            HomeScreen()
        }
        composable(MindoraDestination.PRACTICE.route) {
            PracticeScreen(
                onFreeMeditationClick = {
                    navController.navigate(FreeMeditationDestination.route)
                },
                onBreathingExerciseClick = {
                    navController.navigate(BreathingExerciseDestination.route)
                },
            )
        }
        composable(FreeMeditationDestination.route) {
            FreeMeditationScreen(onNavigateBack = navController::navigateUp)
        }
        composable(BreathingExerciseDestination.route) {
            BreathingExerciseScreen(onNavigateBack = navController::navigateUp)
        }
        composable(MindoraDestination.HISTORY.route) {
            HistoryScreen()
        }
        composable(MindoraDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
