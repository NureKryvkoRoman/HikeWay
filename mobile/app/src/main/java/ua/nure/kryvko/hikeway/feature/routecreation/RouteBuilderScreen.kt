package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.feature.pois.PoiOverviewPopup

@Composable
fun RouteBuilderScreen(
    state: RouteCreationUiState,
    onCrosshairChange: (GeoPoint) -> Unit,
    onCancel: () -> Unit,
    onPlacePoint: () -> Unit,
    onFinish: () -> Unit,
    onPoiClick: (Long) -> Unit,
    onDismissPoi: () -> Unit,
    onRatePoi: (Int) -> Unit,
    onAddPoiToRoute: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        RouteBuilderMap(
            points = state.points,
            crosshairPoint = state.crosshairPoint,
            pointsOfInterest = state.pointsOfInterest,
            onCrosshairChange = onCrosshairChange,
            onPoiClick = onPoiClick,
        )
        Crosshair(modifier = Modifier.align(Alignment.Center))
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Create route", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Placed points: ${state.points.size} | Distance: ${state.distanceKm.formatDistance()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.validationMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(onClick = onPlacePoint) {
                        Text("Place a point")
                    }
                    Button(onClick = onFinish) {
                        Text("Finish")
                    }
                }
            }
        }
        state.selectedPoi?.let { poi ->
            PoiOverviewPopup(
                poi = poi,
                onDismiss = onDismissPoi,
                onRate = onRatePoi,
                onAddToRoute = onAddPoiToRoute,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun Crosshair(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 40.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
