package com.cydoniancitizen.mindora.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cydoniancitizen.mindora.navigation.MindoraDestination
import com.cydoniancitizen.mindora.navigation.MindoraNavHost
import com.cydoniancitizen.mindora.navigation.MindoraNavigationBar
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme

@Composable
fun MindoraApp() {
    MindoraTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute == null ||
            MindoraDestination.entries.any { it.route == currentRoute }

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
