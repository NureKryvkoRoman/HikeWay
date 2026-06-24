package ua.nure.kryvko.hikeway.data.services.backend

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import ua.nure.kryvko.hikeway.data.sync.SyncRequestDto
import ua.nure.kryvko.hikeway.data.sync.SyncResponseDto
import ua.nure.kryvko.hikeway.data.sync.RouteChangeDto

interface SyncService {
    @POST("sync")
    suspend fun sync(@Body request: SyncRequestDto): SyncResponseDto

    @POST("routes/{clientId}/publish")
    suspend fun publishRoute(@Path("clientId") clientId: String): RouteChangeDto

    @POST("routes/{clientId}/unpublish")
    suspend fun unpublishRoute(@Path("clientId") clientId: String): RouteChangeDto
}
