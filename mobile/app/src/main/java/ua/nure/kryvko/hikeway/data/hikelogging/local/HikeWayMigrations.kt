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
