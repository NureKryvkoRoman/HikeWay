package ua.nure.kryvko.hikeway.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SyncTransport {
    suspend fun exchange(request: SyncRequestDto): SyncResponseDto
}

interface SyncLocalDataSource {
    suspend fun createRequest(cursor: String?, deviceId: String): SyncRequestDto
    suspend fun apply(response: SyncResponseDto)
}

interface SyncMetadataStore {
    fun cursor(userId: String): String?
    fun saveCursor(userId: String, cursor: String)
    fun deviceId(): String
}

class SyncCoordinator(
    private val transport: SyncTransport,
    private val localDataSource: SyncLocalDataSource,
    private val metadataStore: SyncMetadataStore,
) {
    suspend fun synchronize(userId: String) = processMutex.withLock {
        synchronizeLocked(userId)
    }

    private suspend fun synchronizeLocked(userId: String) {
        var nextCursor = metadataStore.cursor(userId)
        do {
            val request = localDataSource.createRequest(nextCursor, metadataStore.deviceId())
            val response = transport.exchange(request)
            localDataSource.apply(response)
            metadataStore.saveCursor(userId, response.cursor)
            nextCursor = response.cursor
        } while (response.hasMore)
    }

    private companion object {
        val processMutex = Mutex()
    }
}
