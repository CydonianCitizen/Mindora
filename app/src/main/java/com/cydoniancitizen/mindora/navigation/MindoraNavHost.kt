package com.cydoniancitizen.mindora.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cydoniancitizen.mindora.feature.pathdetail.PathStepType
import com.cydoniancitizen.mindora.feature.breathing.BreathingExerciseScreen
import com.cydoniancitizen.mindora.feature.freemeditation.FreeMeditationScreen
import com.cydoniancitizen.mindora.feature.guidedmeditation.GuidedMeditationScreen
import com.cydoniancitizen.mindora.feature.history.HistoryScreen
import com.cydoniancitizen.mindora.feature.library.LibraryScreen
import com.cydoniancitizen.mindora.feature.library.MeditationDetailScreen
import com.cydoniancitizen.mindora.feature.pathdetail.PathDetailScreen
import com.cydoniancitizen.mindora.feature.practice.PracticeScreen
import com.cydoniancitizen.mindora.feature.settings.SettingsScreen
import com.cydoniancitizen.mindora.feature.whitenoise.WhiteNoiseScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MindoraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = MindoraDestination.PRACTICE.route,
                enterTransition = {
                    if (isPeerSwitch()) fadeThroughEnter() else sharedAxisEnter(forward = true)
                },
                exitTransition = {
                    if (isPeerSwitch()) fadeThroughExit() else sharedAxisExit(forward = true)
                },
                popEnterTransition = {
                    if (isPeerSwitch()) fadeThroughEnter() else sharedAxisEnter(forward = false)
                },
                popExitTransition = {
                    if (isPeerSwitch()) fadeThroughExit() else sharedAxisExit(forward = false)
                },
            ) {
                composable(MindoraDestination.PRACTICE.route) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        PracticeScreen(
                            onFreeMeditationClick = {
                                navController.navigate(FreeMeditationDestination.route)
                            },
                            onWhiteNoiseClick = {
                                navController.navigate(WhiteNoiseDestination.route)
                            },
                            onBreathingExerciseClick = {
                                navController.navigate(BreathingExerciseDestination.route)
                            },
                            onPathClick = { pathId ->
                                navController.navigate(PathDetailDestination.createRoute(pathId))
                            },
                            onLibraryClick = {
                                navController.navigate(LibraryDestination.route)
                            },
                        )
                    }
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
                composable(
                    route = FreeMeditationDestination.route,
                    enterTransition = { containerEnter() },
                    exitTransition = { containerExit() },
                    popEnterTransition = { containerEnter() },
                    popExitTransition = { containerExit() },
                ) {
                    ContainerTransformHost(ContainerKeys.FREE_MEDITATION) {
                        FreeMeditationScreen(onNavigateBack = navController::navigateUp)
                    }
                }
                composable(
                    route = BreathingExerciseDestination.route,
                    enterTransition = { containerEnter() },
                    exitTransition = { containerExit() },
                    popEnterTransition = { containerEnter() },
                    popExitTransition = { containerExit() },
                ) {
                    ContainerTransformHost(ContainerKeys.BREATHING_EXERCISE) {
                        BreathingExerciseScreen(onNavigateBack = navController::navigateUp)
                    }
                }
                composable(
                    route = WhiteNoiseDestination.route,
                    enterTransition = { containerEnter() },
                    exitTransition = { containerExit() },
                    popEnterTransition = { containerEnter() },
                    popExitTransition = { containerExit() },
                ) {
                    ContainerTransformHost(ContainerKeys.WHITE_NOISE) {
                        WhiteNoiseScreen(onNavigateBack = navController::navigateUp)
                    }
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
                composable(
                    route = LibraryDestination.route,
                    enterTransition = { containerEnter() },
                    exitTransition = { containerExit() },
                    popEnterTransition = { containerEnter() },
                    popExitTransition = { containerExit() },
                ) {
                    ContainerTransformHost(ContainerKeys.LIBRARY) {
                        LibraryScreen(
                            onNavigateBack = navController::navigateUp,
                            onMeditationClick = { meditationId ->
                                navController.navigate(
                                    MeditationDetailDestination.createRoute(meditationId),
                                )
                            },
                        )
                    }
                }
                composable(
                    route = MeditationDetailDestination.route,
                    arguments = listOf(
                        navArgument(MeditationDetailDestination.meditationIdArgument) {
                            type = NavType.StringType
                        },
                    ),
                ) {
                    MeditationDetailScreen(
                        onNavigateBack = navController::navigateUp,
                        // No approved audio is bundled yet, so a library practice runs on the
                        // timer the free meditation screen already provides.
                        onStartPractice = { meditationId ->
                            navController.navigate(
                                FreeMeditationDestination.createLinkedRoute(meditationId),
                            )
                        },
                    )
                }
                composable(MindoraDestination.HISTORY.route) {
                    HistoryScreen(
                        onNavigateToPractice = {
                            navController.navigate(MindoraDestination.PRACTICE.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(MindoraDestination.SETTINGS.route) {
                    SettingsScreen()
                }
            }
        }
    }
}

/**
 * Wraps a destination in the bounds that grew out of the card the user tapped.
 *
 * The opaque surface matters: without it the morphing container would be see-through and the
 * screen underneath would read through the growing card.
 */
@Composable
private fun AnimatedVisibilityScope.ContainerTransformHost(
    key: String,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedContainer(key)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
    }
}
