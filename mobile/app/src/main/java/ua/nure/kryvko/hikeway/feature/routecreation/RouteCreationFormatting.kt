package ua.nure.kryvko.hikeway.feature.routecreation

import java.util.Locale
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Terrain

fun Difficulty.label() = name.lowercase().replaceFirstChar(Char::uppercase)

fun Terrain.label() = name.lowercase().replaceFirstChar(Char::uppercase)

fun Double.formatDistance(): String {
    return "%.2f km".format(Locale.US, this)
}
