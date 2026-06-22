package ua.nure.kryvko.hikeway.core.location

import android.content.Context
import ua.nure.kryvko.hikeway.core.location.android.AndroidLocationProvider

fun createLocationProvider(
    context: Context,
    useSimulatedGps: Boolean,
): LocationProvider {
    return if (useSimulatedGps) {
        StubLocationProvider()
    } else {
        AndroidLocationProvider(context)
    }
}

fun selectLocationProvider(
    useSimulatedGps: Boolean,
    simulatedProvider: LocationProvider,
    realProvider: LocationProvider,
): LocationProvider {
    return if (useSimulatedGps) simulatedProvider else realProvider
}
