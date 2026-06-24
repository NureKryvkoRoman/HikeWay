package ua.nure.kryvko.hikeway.data.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncCoordinatorTest {
    @Test
    fun followsPaginationAndPersistsLatestCursor() = runTest {
        val transport = FakeSyncTransport(
            listOf(
                response(cursor = "one", hasMore = true),
                response(cursor = "two", hasMore = false),
            )
        )
        val local = FakeSyncLocalDataSource()
        val metadata = FakeSyncMetadataStore()
        val coordinator = SyncCoordinator(transport, local, metadata)

        coordinator.synchronize("user-1")

        assertEquals(listOf(null, "one"), local.requestedCursors)
        assertEquals(listOf("one", "two"), local.applied.map { it.cursor })
        assertEquals("two", metadata.cursor("user-1"))
    }

    @Test
    fun failedExchangeDoesNotAdvanceCursor() = runTest {
        val metadata = FakeSyncMetadataStore(initialCursor = "old")
        val coordinator = SyncCoordinator(
            transport = object : SyncTransport {
                override suspend fun exchange(request: SyncRequestDto): SyncResponseDto {
                    error("offline")
                }
            },
            localDataSource = FakeSyncLocalDataSource(),
            metadataStore = metadata,
        )

        runCatching { coordinator.synchronize("user-1") }

        assertEquals("old", metadata.cursor("user-1"))
    }
}

private class FakeSyncTransport(
    private val responses: List<SyncResponseDto>,
) : SyncTransport {
    private var index = 0

    override suspend fun exchange(request: SyncRequestDto): SyncResponseDto {
        return responses[index++]
    }
}

private class FakeSyncLocalDataSource : SyncLocalDataSource {
    val requestedCursors = mutableListOf<String?>()
    val applied = mutableListOf<SyncResponseDto>()

    override suspend fun createRequest(cursor: String?, deviceId: String): SyncRequestDto {
        requestedCursors += cursor
        return SyncRequestDto(cursor, deviceId, emptyList(), emptyList())
    }

    override suspend fun apply(response: SyncResponseDto) {
        applied += response
    }
}

private class FakeSyncMetadataStore(
    initialCursor: String? = null,
) : SyncMetadataStore {
    private val cursors = mutableMapOf<String, String>()

    init {
        if (initialCursor != null) cursors["user-1"] = initialCursor
    }

    override fun cursor(userId: String): String? = cursors[userId]

    override fun saveCursor(userId: String, cursor: String) {
        cursors[userId] = cursor
    }

    override fun deviceId(): String = "device-1"
}

private fun response(cursor: String, hasMore: Boolean) = SyncResponseDto(
    cursor = cursor,
    hasMore = hasMore,
    accepted = emptyList(),
    conflicts = emptyList(),
    routeChanges = emptyList(),
    hikeChanges = emptyList(),
)
