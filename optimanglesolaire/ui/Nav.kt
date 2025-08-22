package com.fredz.optimanglesolaire.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fredz.optimanglesolaire.feature.estimation.EstimationViewModel
import com.fredz.optimanglesolaire.feature.home.HomeViewModel
import com.fredz.optimanglesolaire.ui.screens.EstimationScreen
import com.fredz.optimanglesolaire.ui.screens.HomeScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()

    AppShell(
        onHelp = { navController.navigate("help") },
        onSettings = { /* L'icône engrenage ouvrira l'écran d'estimation */
            // On s'assure de ne pas ouvrir la page sur elle-même
            if (navController.currentDestination?.route?.startsWith("estimation") == false) {
                navController.navigate("estimation")
            }
        },
        onLanguageChange = { _ -> /* géré dans AppShell */ }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    // L'action de navigation est maintenant définie ici
                    onGoToEstimation = { lat, lon, tilt, az ->
                        navController.navigate("estimation?lat=$lat&lon=$lon&tilt=$tilt&az=$az")
                    }
                )
            }
            // L'écran d'estimation peut maintenant recevoir des arguments
            composable(
                route = "estimation?lat={lat}&lon={lon}&tilt={tilt}&az={az}",
                arguments = listOf(
                    navArgument("lat") { nullable = true },
                    navArgument("lon") { nullable = true },
                    navArgument("tilt") { nullable = true },
                    navArgument("az") { nullable = true }
                )
            ) {
                val estimationViewModel: EstimationViewModel = viewModel()
                EstimationScreen(
                    viewModel = estimationViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("help") { Text("Aide") }
        }
    }
}