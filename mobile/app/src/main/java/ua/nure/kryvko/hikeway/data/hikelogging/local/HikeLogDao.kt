package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HikeLogDao {
    @Insert
    suspend fun insert(log: HikeLogEntity): Long

    @Query(
        "SELECT * FROM hike_logs WHERE ownerUserId = :ownerUserId AND deleted = 0 " +
            "ORDER BY finishedAtEpochMillis DESC"
    )
    fun observeAll(ownerUserId: String): Flow<List<HikeLogEntity>>

    @Query(
        "SELECT * FROM hike_logs WHERE ownerUserId = :ownerUserId AND deleted = 0 " +
            "ORDER BY finishedAtEpochMillis DESC"
    )
    suspend fun getAll(ownerUserId: String): List<HikeLogEntity>

    @Query("SELECT * FROM hike_logs WHERE ownerUserId = :ownerUserId AND syncState != 'SYNCED'")
    suspend fun getPending(ownerUserId: String): List<HikeLogEntity>

    @Query("SELECT * FROM hike_logs WHERE ownerUserId = :ownerUserId AND clientId = :clientId LIMIT 1")
    suspend fun findByClientId(ownerUserId: String, clientId: String): HikeLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: HikeLogEntity): Long

    @Update
    suspend fun update(log: HikeLogEntity)
}
