package ua.nure.kryvko.hikeway.feature.pois

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

@Composable
fun RatingRow(
    poi: PointOfInterest,
    onRate: (Int) -> Unit,
    onRemoveRating: (() -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingStars(
            rating = poi.userRating ?: poi.averageRating.toInt().coerceIn(0, 5),
            onRate = onRate,
        )
        Text(
            "Avg ${"%.1f".format(Locale.US, poi.averageRating)} (${poi.ratingCount})",
            style = MaterialTheme.typography.bodySmall,
        )
        if (poi.userRating != null && onRemoveRating != null) {
            TextButton(onClick = onRemoveRating) {
                Text("Clear")
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
