package ua.nure.kryvko.hikeway.data.routes.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RouteDao {
    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Query("SELECT * FROM routes WHERE ownerUserId = :ownerUserId AND deleted = 0 ORDER BY id DESC")
    suspend fun getAll(ownerUserId: String): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE ownerUserId = :ownerUserId AND syncState != 'SYNCED'")
    suspend fun getPending(ownerUserId: String): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE ownerUserId = :ownerUserId AND clientId = :clientId LIMIT 1")
    suspend fun findByClientId(ownerUserId: String, clientId: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE ownerUserId = :ownerUserId AND id = :id LIMIT 1")
    suspend fun findById(ownerUserId: String, id: Long): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)
}
