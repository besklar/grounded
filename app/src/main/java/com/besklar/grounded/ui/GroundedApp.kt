package com.besklar.grounded.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.besklar.grounded.ui.home.HomeScreen
import com.besklar.grounded.ui.home.HomeViewModel

@Composable
fun GroundedApp() {
    val viewModel: HomeViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    HomeScreen(
        state = state,
        onModeSelected = viewModel::selectMode,
        onRefresh = viewModel::refresh,
        onEventSelected = viewModel::selectEvent,
    )
}
