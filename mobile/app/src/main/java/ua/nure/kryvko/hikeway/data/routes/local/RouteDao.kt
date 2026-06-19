package ua.nure.kryvko.hikeway.data.routes.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RouteDao {
    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Query("SELECT * FROM routes ORDER BY id DESC")
    suspend fun getAll(): List<RouteEntity>
}
