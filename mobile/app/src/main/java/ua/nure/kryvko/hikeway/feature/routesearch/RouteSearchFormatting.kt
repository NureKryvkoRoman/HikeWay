package ua.nure.kryvko.hikeway.feature.routesearch

import java.util.Locale
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus

fun Difficulty.label() = name.lowercase().replaceFirstChar(Char::uppercase)

fun Terrain.label() = name.lowercase().replaceFirstChar(Char::uppercase)

fun RoutePickingStatus.label() = name.lowercase().replaceFirstChar(Char::uppercase)

fun Long.formatDuration(): String {
    val totalSeconds = this / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
}

fun Double.formatDistance(): String {
    return "%.2f km".format(Locale.US, this)
}
