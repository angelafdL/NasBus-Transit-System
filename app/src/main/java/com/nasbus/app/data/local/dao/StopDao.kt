package com.nasbus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nasbus.app.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<StopEntity>)

    @Query("SELECT * FROM stops WHERE routeId = :routeId ORDER BY sequenceOrder ASC")
    fun getStopsForRoute(routeId: Int): Flow<List<StopEntity>>

    @Query("SELECT * FROM stops WHERE routeId = :routeId ORDER BY sequenceOrder ASC")
    suspend fun getStopsForRouteList(routeId: Int): List<StopEntity>

    @Query("SELECT DISTINCT stopName FROM stops ORDER BY stopName ASC")
    fun getDistinctStopNames(): Flow<List<String>>
}
