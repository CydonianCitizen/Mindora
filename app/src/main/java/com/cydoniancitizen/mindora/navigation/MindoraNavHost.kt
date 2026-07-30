package com.cydoniancitizen.mindora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cydoniancitizen.mindora.feature.breathing.BreathingExerciseScreen
import com.cydoniancitizen.mindora.feature.guidedmeditation.GuidedMeditationScreen
import com.cydoniancitizen.mindora.feature.history.HistoryScreen
import com.cydoniancitizen.mindora.feature.home.HomeScreen
import com.cydoniancitizen.mindora.feature.freemeditation.FreeMeditationScreen
import com.cydoniancitizen.mindora.feature.pathdetail.PathDetailScreen
import com.cydoniancitizen.mindora.feature.pathdetail.PathStepType
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
                onPathClick = { pathId ->
                    navController.navigate(PathDetailDestination.createRoute(pathId))
                },
            )
        }
        composable(
            route = PathDetailDestination.route,
            arguments = listOf(
                navArgument(PathDetailDestination.pathIdArgument) {
                    type = NavType.StringType
                },
            ),
        ) {
            PathDetailScreen(
                onNavigateBack = navController::navigateUp,
                onStepClick = { type, stepId ->
                    val route = when (type) {
                        PathStepType.GUIDED_MEDITATION ->
                            GuidedMeditationDestination.createRoute(stepId)

                        PathStepType.FREE_MEDITATION ->
                            FreeMeditationDestination.createLinkedRoute(stepId)

                        PathStepType.BREATHING_EXERCISE ->
                            BreathingExerciseDestination.createLinkedRoute(stepId)
                    }
                    navController.navigate(route)
                },
            )
        }
        composable(FreeMeditationDestination.route) {
            FreeMeditationScreen(onNavigateBack = navController::navigateUp)
        }
        composable(BreathingExerciseDestination.route) {
            BreathingExerciseScreen(onNavigateBack = navController::navigateUp)
        }
        composable(
            route = FreeMeditationDestination.linkedRoute,
            arguments = listOf(
                navArgument(FreeMeditationDestination.stepIdArgument) {
                    type = NavType.StringType
                },
            ),
        ) {
            FreeMeditationScreen(onNavigateBack = navController::navigateUp)
        }
        composable(
            route = BreathingExerciseDestination.linkedRoute,
            arguments = listOf(
                navArgument(BreathingExerciseDestination.stepIdArgument) {
                    type = NavType.StringType
                },
            ),
        ) {
            BreathingExerciseScreen(onNavigateBack = navController::navigateUp)
        }
        composable(
            route = GuidedMeditationDestination.route,
            arguments = listOf(
                navArgument(GuidedMeditationDestination.stepIdArgument) {
                    type = NavType.StringType
                },
            ),
        ) {
            GuidedMeditationScreen(onNavigateBack = navController::navigateUp)
        }
        composable(MindoraDestination.HISTORY.route) {
            HistoryScreen()
        }
        composable(MindoraDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
