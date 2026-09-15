package com.besklar.grounded.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.besklar.grounded.R
import com.besklar.grounded.location.RelativeLocation
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.ui.home.EarthquakeFormatter
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    earthquake: Earthquake?,
    relativeLocation: RelativeLocation?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.earthquake_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (earthquake == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
        } else {
            DetailContent(earthquake, relativeLocation, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailContent(
    earthquake: Earthquake,
    relativeLocation: RelativeLocation?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val now = remember { Instant.now() }
    val zoneId = ZoneId.systemDefault()
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    val occurred =
        remember(earthquake.occurredAt, locale, zoneId) {
            DateFormat
                .getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, locale)
                .apply { timeZone = java.util.TimeZone.getTimeZone(zoneId) }
                .format(Date.from(earthquake.occurredAt))
        }
    val shareText = remember(earthquake, locale, now, zoneId) { buildShareText(earthquake, locale, now, zoneId) }
    val safeUrl = remember(earthquake.detailUrl) { safeUsgsUrl(earthquake.detailUrl) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("M ${EarthquakeFormatter.magnitude(earthquake, locale)}", style = MaterialTheme.typography.displayMedium)
        Text(EarthquakeFormatter.place(earthquake, locale), style = MaterialTheme.typography.headlineSmall)
        Text(EarthquakeFormatter.relativeTime(earthquake.occurredAt, now, locale, zoneId))
        HorizontalDivider()
        DetailRow(stringResource(R.string.occurred), occurred)
        EarthquakeFormatter.depth(earthquake, locale)?.let { DetailRow(stringResource(R.string.depth), it) }
        relativeLocation?.let {
            DetailRow(stringResource(R.string.distance_from_you), EarthquakeFormatter.relativeLocation(it, locale))
        }
        earthquake.coordinates?.let {
            DetailRow(stringResource(R.string.coordinates), "${number.format(it.latitude)}, ${number.format(it.longitude)}")
        }
        earthquake.magnitudeType?.let { DetailRow(stringResource(R.string.magnitude_type), it.uppercase(locale)) }
        earthquake.feltReports?.let { DetailRow(stringResource(R.string.felt_reports), number.format(it)) }
        earthquake.significance?.let { DetailRow(stringResource(R.string.usgs_significance), number.format(it)) }
        earthquake.alert?.let { DetailRow(stringResource(R.string.alert_level), it.replaceFirstChar(Char::uppercase)) }
        earthquake.tsunami?.let { DetailRow(stringResource(R.string.tsunami), if (it) "Flagged" else "Not flagged") }
        earthquake.reviewStatus?.let { DetailRow(stringResource(R.string.review_status), it.replaceFirstChar(Char::uppercase)) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_earthquake)))
                },
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text(stringResource(R.string.share))
            }
            if (safeUrl != null) {
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, safeUrl.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // The official URL remains visible in share text; no unsafe fallback is attempted.
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                    Text(stringResource(R.string.usgs_source))
                }
            }
        }
        Text(stringResource(R.string.usgs_attribution), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
