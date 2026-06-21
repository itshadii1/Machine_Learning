package com.namaaztracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.namaaztracker.presentation.screens.home.HomeScreen
import com.namaaztracker.presentation.screens.tracker.TrackerScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_TRACKER = "tracker/{namaazType}"
private const val ARG_NAMAAZ_TYPE = "namaazType"

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onNamaazSelected = { namaazType ->
                    navController.navigate("tracker/${namaazType.name}")
                },
            )
        }

        composable(
            route = ROUTE_TRACKER,
            arguments = listOf(
                navArgument(ARG_NAMAAZ_TYPE) { type = NavType.StringType },
            ),
        ) {
            TrackerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
