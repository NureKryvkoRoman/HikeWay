package ua.nure.kryvko.hikeway.data.sync

import ua.nure.kryvko.hikeway.data.services.backend.SyncService

class RetrofitSyncTransport(
    private val service: SyncService,
) : SyncTransport {
    override suspend fun exchange(request: SyncRequestDto): SyncResponseDto {
        return service.sync(request)
    }
}
