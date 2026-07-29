package com.cydoniancitizen.mindora.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.cydoniancitizen.mindora.navigation.MindoraNavHost
import com.cydoniancitizen.mindora.navigation.MindoraNavigationBar
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme

@Composable
fun MindoraApp() {
    MindoraTheme {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                MindoraNavigationBar(navController = navController)
            },
        ) { innerPadding ->
            MindoraNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
