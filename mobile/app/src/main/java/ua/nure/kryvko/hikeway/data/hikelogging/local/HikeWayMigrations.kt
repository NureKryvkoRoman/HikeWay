package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addSyncColumns(db, "routes", includeRouteReference = false)
        addSyncColumns(db, "hike_logs", includeRouteReference = true)
        assignClientIds(db, "routes")
        assignClientIds(db, "hike_logs")
        db.execSQL(
            """
            UPDATE `hike_logs`
            SET `routeClientId` = (
                SELECT `routes`.`clientId`
                FROM `routes`
                WHERE `routes`.`id` = `hike_logs`.`routeId`
                  AND `routes`.`ownerUserId` = `hike_logs`.`ownerUserId`
            ),
            `routeServerId` = (
                SELECT `routes`.`serverId`
                FROM `routes`
                WHERE `routes`.`id` = `hike_logs`.`routeId`
                  AND `routes`.`ownerUserId` = `hike_logs`.`ownerUserId`
            )
            WHERE EXISTS (
                SELECT 1 FROM `routes`
                WHERE `routes`.`id` = `hike_logs`.`routeId`
                  AND `routes`.`ownerUserId` = `hike_logs`.`ownerUserId`
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_routes_ownerUserId_clientId` " +
                "ON `routes` (`ownerUserId`, `clientId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_hike_logs_ownerUserId_clientId` " +
                "ON `hike_logs` (`ownerUserId`, `clientId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_conflicts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerUserId` TEXT NOT NULL,
                `resourceType` TEXT NOT NULL,
                `clientId` TEXT NOT NULL,
                `submittedBaseVersion` INTEGER NOT NULL,
                `serverVersion` INTEGER NOT NULL,
                `serverRecordJson` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_sync_conflicts_ownerUserId_resourceType_clientId` " +
                "ON `sync_conflicts` (`ownerUserId`, `resourceType`, `clientId`)"
        )
    }
}

private fun addSyncColumns(
    db: SupportSQLiteDatabase,
    table: String,
    includeRouteReference: Boolean,
) {
    db.execSQL("ALTER TABLE `$table` ADD COLUMN `clientId` TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE `$table` ADD COLUMN `serverId` INTEGER")
    db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncVersion` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAtEpochMillis` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'PENDING'")
    db.execSQL("ALTER TABLE `$table` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
    if (includeRouteReference) {
        db.execSQL("ALTER TABLE `$table` ADD COLUMN `routeClientId` TEXT")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN `routeServerId` INTEGER")
    }
}

private fun assignClientIds(db: SupportSQLiteDatabase, table: String) {
    db.query("SELECT id FROM `$table` WHERE clientId = ''").use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow("id")
        while (cursor.moveToNext()) {
            db.execSQL(
                "UPDATE `$table` SET clientId = ? WHERE id = ?",
                arrayOf<Any>(UUID.randomUUID().toString(), cursor.getLong(idIndex)),
            )
        }
    }
}
