package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HikeLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HikeWayDatabase : RoomDatabase() {
    abstract fun hikeLogDao(): HikeLogDao
}
