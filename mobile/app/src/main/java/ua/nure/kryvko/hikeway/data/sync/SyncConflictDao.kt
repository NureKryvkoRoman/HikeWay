package ua.nure.kryvko.hikeway.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conflict: SyncConflictEntity)

    @Query(
        "DELETE FROM sync_conflicts WHERE ownerUserId = :ownerUserId " +
            "AND resourceType = :resourceType AND clientId = :clientId"
    )
    suspend fun delete(ownerUserId: String, resourceType: String, clientId: String)

    @Query("SELECT * FROM sync_conflicts WHERE ownerUserId = :ownerUserId ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(ownerUserId: String): List<SyncConflictEntity>
}
