package com.besklar.grounded.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.besklar.grounded.ui.detail.DetailScreen
import com.besklar.grounded.ui.detail.DetailViewModel
import com.besklar.grounded.ui.home.HomeScreen
import com.besklar.grounded.ui.home.HomeViewModel

@Composable
fun GroundedApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            val openDetails: (String) -> Unit = { id ->
                viewModel.selectEvent(id)
                navController.navigate("detail/$id")
            }
            HomeScreen(
                state = state,
                onModeSelected = viewModel::selectMode,
                onRefresh = viewModel::refresh,
                onEventSelected = openDetails,
                onMapEventSelected = viewModel::selectEvent,
                onOpenDetails = openDetails,
            )
        }
        composable(
            route = "detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) {
            val viewModel: DetailViewModel = hiltViewModel()
            val earthquake = viewModel.earthquake.collectAsStateWithLifecycle().value
            DetailScreen(earthquake = earthquake, onBack = navController::navigateUp)
        }
    }
}
