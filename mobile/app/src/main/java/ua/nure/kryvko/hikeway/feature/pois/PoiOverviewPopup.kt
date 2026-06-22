package ua.nure.kryvko.hikeway.feature.pois

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import ua.nure.kryvko.hikeway.R
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

@Composable
fun PoiOverviewPopup(
    poi: PointOfInterest,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onAddToRoute: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Point of interest", style = MaterialTheme.typography.labelLarge)
            Text(poi.name, style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(poi.photoResIds.ifEmpty { listOf(R.drawable.poi_photo_placeholder) }) { photoResId ->
                    Image(
                        painter = painterResource(photoResId),
                        contentDescription = "${poi.name} photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 160.dp, height = 90.dp),
                    )
                }
            }
            Text(poi.description, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(
                    rating = poi.userRating ?: poi.averageRating.toInt().coerceIn(1, 5),
                    onRate = onRate,
                )
                Text(
                    "Avg ${"%.1f".format(Locale.US, poi.averageRating)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                onAddToRoute?.let { addToRoute ->
                    Button(onClick = addToRoute) {
                        Text("Add to route")
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingStars(
    rating: Int,
    onRate: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Rate $star stars",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onRate(star) },
            )
        }
    }
}
