package com.cydoniancitizen.mindora.app

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cydoniancitizen.mindora.core.reminder.ReminderNotificationPublisher
import com.cydoniancitizen.mindora.navigation.MindoraDestination
import com.cydoniancitizen.mindora.navigation.MindoraNavHost
import com.cydoniancitizen.mindora.navigation.MindoraNavigationBar
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MindoraApp(
    intentFlow: StateFlow<Intent?> = MutableStateFlow(null),
) {
    MindoraTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = shouldShowBottomBar(currentRoute)

        val intent by intentFlow.collectAsStateWithLifecycle()
        LaunchedEffect(intent) {
            val currentIntent = intent ?: return@LaunchedEffect
            if (currentIntent.action == ReminderNotificationPublisher.ACTION_REMINDER_NOTIFICATION) {
                navController.navigate(MindoraDestination.PRACTICE.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    MindoraNavigationBar(navController = navController)
                }
            },
        ) { innerPadding ->
            MindoraNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

internal fun shouldShowBottomBar(route: String?): Boolean =
    MindoraDestination.entries.any { it.route == route }
