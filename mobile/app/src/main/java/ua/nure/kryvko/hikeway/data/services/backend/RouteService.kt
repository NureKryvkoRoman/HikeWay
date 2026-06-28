package ua.nure.kryvko.hikeway.data.services.backend

import retrofit2.http.GET
import retrofit2.http.Query
import ua.nure.kryvko.hikeway.data.routes.remote.RouteSummaryDto
import ua.nure.kryvko.hikeway.data.routes.remote.PageResponseDto

interface RouteService {
    @GET("routes")
    suspend fun search(
        @Query("minDistanceKm") minDistanceKm: Double? = null,
        @Query("maxDistanceKm") maxDistanceKm: Double? = null,
        @Query("minEstimatedTimeMinutes") minEstimatedTimeMinutes: Int? = null,
        @Query("maxEstimatedTimeMinutes") maxEstimatedTimeMinutes: Int? = null,
        @Query("difficulties") difficulties: List<String>? = null,
        @Query("terrains") terrains: List<String>? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("maxProximityKm") maxProximityKm: Double? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PageResponseDto<RouteSummaryDto>
}
