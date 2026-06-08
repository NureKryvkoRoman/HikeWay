package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HikeLogDao {
    @Insert
    suspend fun insert(log: HikeLogEntity): Long

    @Query("SELECT * FROM hike_logs ORDER BY finishedAtEpochMillis DESC")
    fun observeAll(): Flow<List<HikeLogEntity>>

    @Query("SELECT * FROM hike_logs ORDER BY finishedAtEpochMillis DESC")
    suspend fun getAll(): List<HikeLogEntity>
}
