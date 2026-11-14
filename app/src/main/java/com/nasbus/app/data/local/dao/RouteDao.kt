package com.nasbus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.model.RouteListItem
import com.nasbus.app.data.local.model.SegmentResult
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Query("SELECT * FROM routes WHERE routeId = :routeId LIMIT 1")
    suspend fun getRouteWithDetails(routeId: Int): RouteEntity?

    @Query("SELECT * FROM routes WHERE companyId = :companyId ORDER BY routeId ASC")
    fun getByCompany(companyId: Int): Flow<List<RouteEntity>>

    @Query("""
        SELECT 
            r.routeId AS routeId,
            r.routeName AS routeName,
            c.companyName AS companyName,
            r.originName AS originName,
            r.destinationName AS destinationName
        FROM routes r
        INNER JOIN companies c ON r.companyId = c.companyId
        ORDER BY r.routeId ASC
    """)
    fun getAllRouteListItems(): Flow<List<RouteListItem>>

    @Query("""
        SELECT * FROM routes 
        WHERE routeName LIKE '%' || :query || '%' 
           OR originName LIKE '%' || :query || '%' 
           OR destinationName LIKE '%' || :query || '%'
        ORDER BY routeId ASC
    """)
    fun searchByText(query: String): Flow<List<RouteEntity>>

    @Query("""
        SELECT 
            r.routeId AS routeId,
            r.routeName AS routeName,
            c.companyName AS companyName,
            s1.stopName AS fromStopName,
            s2.stopName AS toStopName,
            (s2.distanceFromOriginKm - s1.distanceFromOriginKm) AS segmentDistanceKm,
            ((s2.distanceFromOriginKm - s1.distanceFromOriginKm) * r.farePerKm) AS segmentFare,
            r.estimatedTravelTime AS estimatedTravelTime
        FROM routes r
        INNER JOIN companies c ON r.companyId = c.companyId
        INNER JOIN stops s1 ON r.routeId = s1.routeId
        INNER JOIN stops s2 ON r.routeId = s2.routeId
        WHERE s1.stopName = :from 
          AND s2.stopName = :to 
          AND s1.sequenceOrder < s2.sequenceOrder
        ORDER BY r.routeId ASC
    """)
    fun findSegments(from: String, to: String): Flow<List<SegmentResult>>
}
