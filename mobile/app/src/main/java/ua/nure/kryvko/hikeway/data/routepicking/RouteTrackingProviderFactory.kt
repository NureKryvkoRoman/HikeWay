package ua.nure.kryvko.hikeway.data.routepicking

import android.content.Context
import ua.nure.kryvko.hikeway.data.routepicking.android.AndroidRouteTrackingProvider
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider

fun createRouteTrackingProvider(
    context: Context,
    useSimulatedGps: Boolean,
): RouteTrackingProvider {
    return if (useSimulatedGps) {
        StubRouteTrackingProvider()
    } else {
        AndroidRouteTrackingProvider(context)
    }
}

fun selectRouteTrackingProvider(
    useSimulatedGps: Boolean,
    simulatedProvider: RouteTrackingProvider,
    realProvider: RouteTrackingProvider,
): RouteTrackingProvider {
    return if (useSimulatedGps) simulatedProvider else realProvider
}
