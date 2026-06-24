package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HikeWayMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HikeWayDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesFromOneToTwoAndCreatesRoutesTable() {
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).close()
    }

    @Test
    fun migratesFromTwoToThreeAndDeletesGlobalRows() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO hike_logs (
                    routeId,
                    routeName,
                    startedAtEpochMillis,
                    finishedAtEpochMillis,
                    activeDurationMillis,
                    wallClockDurationMillis,
                    totalDistanceKm,
                    pathGeoJson
                ) VALUES (
                    1,
                    'Old hike',
                    1000,
                    2000,
                    1000,
                    1000,
                    1.0,
                    '{"type":"LineString","coordinates":[[24.0,49.0],[24.1,49.1]]}'
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO routes (
                    name,
                    description,
                    distanceKm,
                    estimatedTimeMinutes,
                    difficulty,
                    elevationGainMeters,
                    terrain,
                    geometryGeoJson
                ) VALUES (
                    'Old route',
                    'Global route',
                    1.0,
                    15,
                    'EASY',
                    0,
                    'FOREST',
                    '{"type":"LineString","coordinates":[[24.0,49.0],[24.1,49.1]]}'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).apply {
            query("SELECT COUNT(*) FROM hike_logs").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            query("SELECT COUNT(*) FROM routes").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    @Test
    fun migratesFromThreeToFourAndMarksExistingRowsForSync() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO routes (
                    ownerUserId, name, description, distanceKm, estimatedTimeMinutes,
                    difficulty, elevationGainMeters, terrain, geometryGeoJson
                ) VALUES (
                    'user-1', 'Local route', 'Description', 1.0, 15,
                    'EASY', 0, 'FOREST',
                    '{"type":"LineString","coordinates":[[24.0,49.0],[24.1,49.1]]}'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).apply {
            query("SELECT clientId, syncVersion, syncState, deleted FROM routes").use { cursor ->
                cursor.moveToFirst()
                assertEquals(false, cursor.getString(0).isBlank())
                assertEquals(0, cursor.getLong(1))
                assertEquals("PENDING", cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DB = "hikeway-migration-test"
    }
}
