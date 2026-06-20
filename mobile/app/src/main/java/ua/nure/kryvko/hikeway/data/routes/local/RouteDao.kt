package ua.nure.kryvko.hikeway.data.routes.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RouteDao {
    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Query("SELECT * FROM routes WHERE ownerUserId = :ownerUserId ORDER BY id DESC")
    suspend fun getAll(ownerUserId: String): List<RouteEntity>
}
