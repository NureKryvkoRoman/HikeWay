package ua.nure.kryvko.hikeway.core.location

import ua.nure.kryvko.hikeway.core.model.GeoPoint

interface LocationProvider {
    suspend fun getCurrentLocation(): GeoPoint
}

class StubLocationProvider(
    private val location: GeoPoint = GeoPoint(longitude = 24.0316, latitude = 49.8429),
) : LocationProvider {
    override suspend fun getCurrentLocation(): GeoPoint = location
}
