package com.cydoniancitizen.mindora.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.cydoniancitizen.mindora.R

enum class MindoraDestination(
    val route: String,
    @param:StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    HOME(
        route = "home",
        labelResId = R.string.home,
        icon = Icons.Filled.Home,
    ),
    PRACTICE(
        route = "practice",
        labelResId = R.string.practice,
        icon = Icons.Filled.PlayArrow,
    ),
    HISTORY(
        route = "history",
        labelResId = R.string.history,
        icon = Icons.AutoMirrored.Filled.List,
    ),
    SETTINGS(
        route = "settings",
        labelResId = R.string.settings,
        icon = Icons.Filled.Settings,
    ),
}

object FreeMeditationDestination {
    const val route = "practice/free-meditation"
    const val stepIdArgument = "stepId"
    const val linkedRoute = "$route/{$stepIdArgument}"

    fun createLinkedRoute(stepId: String): String = "$route/${Uri.encode(stepId)}"
}

object BreathingExerciseDestination {
    const val route = "practice/breathing"
    const val stepIdArgument = "stepId"
    const val linkedRoute = "$route/{$stepIdArgument}"

    fun createLinkedRoute(stepId: String): String = "$route/${Uri.encode(stepId)}"
}

object GuidedMeditationDestination {
    const val stepIdArgument = "stepId"
    const val route = "practice/guided/{$stepIdArgument}"

    fun createRoute(stepId: String): String = "practice/guided/${Uri.encode(stepId)}"
}

object PathDetailDestination {
    const val pathIdArgument = "pathId"
    const val route = "practice/path/{$pathIdArgument}"

    fun createRoute(pathId: String): String = "practice/path/${Uri.encode(pathId)}"
}
