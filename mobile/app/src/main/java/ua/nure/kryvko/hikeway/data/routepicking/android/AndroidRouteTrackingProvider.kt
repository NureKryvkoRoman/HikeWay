package ua.nure.kryvko.hikeway.data.routepicking.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.routepicking.RouteProgress
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.bearingDegrees
import ua.nure.kryvko.hikeway.domain.routes.distanceKm

class AndroidRouteTrackingProvider(
    context: Context,
    private val minTimeMillis: Long = 1_000L,
    private val minDistanceMeters: Float = 2f,
) : RouteTrackingProvider {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun positions(route: Route, startIndex: Int): Flow<RouteProgress> = callbackFlow {
        if (!appContext.hasLocationPermission()) {
            close(SecurityException("Location permission is required for GPS tracking."))
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toRouteProgress(route, startIndex))
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            close(IllegalStateException("No location provider is enabled."))
            return@callbackFlow
        }

        locationManager.getLastKnownLocation(provider)?.let { location ->
            trySend(location.toRouteProgress(route, startIndex))
        }
        locationManager.requestLocationUpdates(
            provider,
            minTimeMillis,
            minDistanceMeters,
            listener,
            Looper.getMainLooper(),
        )

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun Location.toRouteProgress(route: Route, startIndex: Int): RouteProgress {
        val position = GeoPoint(longitude = longitude, latitude = latitude)
        val points = route.geometry.points
        val nearestIndex = points.nearestIndexTo(position, startIndex)
        val nextPoint = points.getOrNull(nearestIndex + 1) ?: points.getOrNull(nearestIndex) ?: position
        return RouteProgress(
            position = position,
            bearingDegrees = if (hasBearing()) bearing.toDouble() else bearingDegrees(position, nextPoint),
            pointIndex = nearestIndex,
        )
    }
}

private fun Context.hasLocationPermission(): Boolean {
    return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun List<GeoPoint>.nearestIndexTo(position: GeoPoint, startIndex: Int): Int {
    if (isEmpty()) return 0
    val firstIndex = startIndex.coerceIn(indices)
    return indices.drop(firstIndex).minBy { index -> distanceKm(position, this[index]) }
}
