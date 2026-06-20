package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `distanceKm` REAL NOT NULL,
                `estimatedTimeMinutes` INTEGER NOT NULL,
                `difficulty` TEXT NOT NULL,
                `elevationGainMeters` INTEGER NOT NULL,
                `terrain` TEXT NOT NULL,
                `geometryGeoJson` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `hike_logs`")
        db.execSQL("DROP TABLE IF EXISTS `routes`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hike_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerUserId` TEXT NOT NULL,
                `routeId` INTEGER NOT NULL,
                `routeName` TEXT NOT NULL,
                `startedAtEpochMillis` INTEGER NOT NULL,
                `finishedAtEpochMillis` INTEGER NOT NULL,
                `activeDurationMillis` INTEGER NOT NULL,
                `wallClockDurationMillis` INTEGER NOT NULL,
                `totalDistanceKm` REAL NOT NULL,
                `pathGeoJson` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerUserId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `distanceKm` REAL NOT NULL,
                `estimatedTimeMinutes` INTEGER NOT NULL,
                `difficulty` TEXT NOT NULL,
                `elevationGainMeters` INTEGER NOT NULL,
                `terrain` TEXT NOT NULL,
                `geometryGeoJson` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
