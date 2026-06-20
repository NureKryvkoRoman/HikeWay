package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog

class HikeLogDaoTest {
    private lateinit var database: HikeWayDatabase
    private lateinit var dao: HikeLogDao
    private lateinit var currentUserProvider: MutableCurrentUserProvider

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            HikeWayDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.hikeLogDao()
        currentUserProvider = MutableCurrentUserProvider()
        currentUserProvider.setCurrentUserId("user-1")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndReadsCompletedHike() = runBlocking {
        val repository = RoomHikeLogRepository(dao, currentUserProvider)
        val id = repository.save(
            hikeLog(routeName = "High Castle Loop")
        )

        val saved = dao.getAll("user-1").single()
        assertEquals(id, saved.id)
        assertEquals("user-1", saved.ownerUserId)
        assertEquals("High Castle Loop", saved.routeName)
        assertEquals(
            """{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}""",
            saved.pathGeoJson,
        )
    }

    @Test
    fun readsOnlyCurrentUsersCompletedHikes() = runBlocking {
        val repository = RoomHikeLogRepository(dao, currentUserProvider)
        repository.save(hikeLog(routeName = "User one hike"))

        currentUserProvider.setCurrentUserId("user-2")
        repository.save(hikeLog(routeName = "User two hike"))

        assertEquals("User one hike", dao.getAll("user-1").single().routeName)
        assertEquals("User two hike", dao.getAll("user-2").single().routeName)
    }

    private fun hikeLog(routeName: String) = HikeLog(
        routeId = 1,
        routeName = routeName,
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
}
