package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.nure.kryvko.hikeway.data.routes.local.RouteDao
import ua.nure.kryvko.hikeway.data.routes.local.RouteEntity
import ua.nure.kryvko.hikeway.data.sync.SyncConflictDao
import ua.nure.kryvko.hikeway.data.sync.SyncConflictEntity

@Database(
    entities = [HikeLogEntity::class, RouteEntity::class, SyncConflictEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class HikeWayDatabase : RoomDatabase() {
    abstract fun hikeLogDao(): HikeLogDao
    abstract fun routeDao(): RouteDao
    abstract fun syncConflictDao(): SyncConflictDao
}
