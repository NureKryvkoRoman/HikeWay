package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.nure.kryvko.hikeway.data.routes.local.RouteDao
import ua.nure.kryvko.hikeway.data.routes.local.RouteEntity

@Database(
    entities = [HikeLogEntity::class, RouteEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class HikeWayDatabase : RoomDatabase() {
    abstract fun hikeLogDao(): HikeLogDao
    abstract fun routeDao(): RouteDao
}
