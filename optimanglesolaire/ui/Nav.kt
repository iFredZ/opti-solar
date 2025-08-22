package com.fredz.optimanglesolaire.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fredz.optimanglesolaire.feature.estimation.EstimationViewModel
import com.fredz.optimanglesolaire.feature.home.HomeViewModel
import com.fredz.optimanglesolaire.ui.screens.EstimationScreen
import com.fredz.optimanglesolaire.ui.screens.HelpScreen
import com.fredz.optimanglesolaire.ui.screens.HomeScreen
import com.fredz.optimanglesolaire.ui.screens.SettingsScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()
    // On ajoute un observateur pour connaître la page actuelle
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AppShell(
        onHelp = {
            // Si on est déjà sur l'aide, on revient en arrière
            if (currentRoute == "help") {
                navController.popBackStack()
            } else {
                // Sinon, on y navigue
                navController.navigate("help") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        onSettings = {
            // Si on est déjà sur les réglages, on revient en arrière
            if (currentRoute == "settings") {
                navController.popBackStack()
            } else {
                // Sinon, on y navigue
                navController.navigate("settings") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
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
                    onGoToEstimation = { lat, lon, tilt, az ->
                        navController.navigate("estimation?lat=$lat&lon=$lon&tilt=$tilt&az=$az")
                    }
                )
            }
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
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("help") {
                HelpScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}