package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog

class HikeLogDaoTest {
    private lateinit var database: HikeWayDatabase
    private lateinit var dao: HikeLogDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            HikeWayDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.hikeLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndReadsCompletedHike() = runBlocking {
        val repository = RoomHikeLogRepository(dao)
        val id = repository.save(
            HikeLog(
                routeId = 1,
                routeName = "High Castle Loop",
                startedAtEpochMillis = 1_000,
                finishedAtEpochMillis = 5_000,
                activeDurationMillis = 3_000,
                wallClockDurationMillis = 4_000,
                totalDistanceKm = 0.42,
                path = listOf(
                    GeoPoint(longitude = 24.0316, latitude = 49.8429),
                    GeoPoint(longitude = 24.0394, latitude = 49.8461),
                ),
            )
        )

        val saved = dao.getAll().single()
        assertEquals(id, saved.id)
        assertEquals("High Castle Loop", saved.routeName)
        assertEquals(
            """{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}""",
            saved.pathGeoJson,
        )
    }
}
