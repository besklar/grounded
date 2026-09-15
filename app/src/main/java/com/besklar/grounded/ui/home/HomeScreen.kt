package com.besklar.grounded.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.besklar.grounded.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onModeSelected: (HomeMode) -> Unit,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    onMapEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = state.refreshStatus !is RefreshStatus.Refreshing) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SituationSummaryCard(state)
            Spacer(Modifier.height(16.dp))
            ModeSelector(state.mode, onModeSelected)
            Spacer(Modifier.height(12.dp))
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.isInitialLoading -> LoadingState()
                    state.isFullScreenFailure -> FailureState(onRefresh)
                    state.snapshot?.earthquakes?.isEmpty() == true -> EmptyState()
                    else -> saveableStateHolder.SaveableStateProvider(state.mode.name) {
                        if (state.mode == HomeMode.MAP) {
                            EarthquakeMap(
                                earthquakes = state.snapshot?.earthquakes.orEmpty(),
                                selectedEventId = state.selectedEventId,
                                onEventSelected = onMapEventSelected,
                                onOpenDetails = onOpenDetails,
                            )
                        } else {
                            EarthquakeList(
                                earthquakes = state.snapshot?.earthquakes.orEmpty(),
                                refreshing = state.refreshStatus is RefreshStatus.Refreshing,
                                refreshFailed = state.refreshStatus is RefreshStatus.Failed,
                                newEventIds = state.newEventIds,
                                onRefresh = onRefresh,
                                onEventSelected = onEventSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SituationSummaryCard(state: HomeUiState) {
    val calculator = SituationSummaryCalculator()
    val summary = calculator.calculate(state.snapshot, state.refreshStatus is RefreshStatus.Failed)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text =
            when (summary) {
                is SituationSummary.GlobalActivity -> stringResource(R.string.recent_activity)
                is SituationSummary.SavedActivity -> stringResource(R.string.showing_saved_activity)
                SituationSummary.NoActivity -> stringResource(R.string.no_recent_activity)
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text =
            when (summary) {
                is SituationSummary.GlobalActivity ->
                    pluralStringResource(R.plurals.events_past_day, summary.eventCount, summary.eventCount)
                is SituationSummary.SavedActivity ->
                    pluralStringResource(R.plurals.saved_events_count, summary.eventCount, summary.eventCount)
                SituationSummary.NoActivity -> stringResource(R.string.no_results_explanation)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.refreshStatus is RefreshStatus.Refreshing && state.snapshot != null) {
            Text(stringResource(R.string.refreshing), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ModeSelector(selected: HomeMode, onSelected: (HomeMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HomeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, HomeMode.entries.size),
                label = { Text(stringResource(if (mode == HomeMode.MAP) R.string.map else R.string.list)) },
            )
        }
    }
}

@Composable
private fun LoadingState() {
    val description = stringResource(R.string.loading_earthquakes)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        CircularProgressIndicator()
        Text(description)
    }
}

@Composable
private fun FailureState(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.could_not_load), style = MaterialTheme.typography.titleLarge)
        androidx.compose.material3.Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EmptyState() {
    Text(stringResource(R.string.no_results_explanation), style = MaterialTheme.typography.bodyLarge)
}
