package com.cydoniancitizen.mindora.navigation

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
