package com.besklar.grounded.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.besklar.grounded.ui.detail.DetailScreen
import com.besklar.grounded.ui.detail.DetailViewModel
import com.besklar.grounded.ui.home.HomeEffect
import com.besklar.grounded.ui.home.HomeScreen
import com.besklar.grounded.ui.home.HomeViewModel

@Composable
fun GroundedApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            val haptics = LocalHapticFeedback.current
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    if (effect is HomeEffect.NewEarthquakes) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
            val context = LocalContext.current
            val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        viewModel.loadLocation()
                    } else {
                        val activity = context as? Activity
                        val permanently =
                            activity?.let {
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                    it,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            } ?: false
                        viewModel.recordLocationDenial(permanently)
                    }
                }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.loadLocation()
                }
            }
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
                onRequestLocation = {
                    if ((state.locationContext as? com.besklar.grounded.location.LocationContext.Denied)?.permanently == true) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                onFiltersChanged = viewModel::updateFilters,
                onResetFilters = viewModel::resetFilters,
            )
        }
        composable(
            route = "detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) {
            val viewModel: DetailViewModel = hiltViewModel()
            val detailState = viewModel.uiState.collectAsStateWithLifecycle().value
            DetailScreen(
                earthquake = detailState.earthquake,
                relativeLocation = detailState.relativeLocation,
                isLoading = !detailState.isResolved,
                onBack = navController::navigateUp,
            )
        }
    }
}
