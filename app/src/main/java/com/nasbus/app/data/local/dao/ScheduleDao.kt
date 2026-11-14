package com.nasbus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nasbus.app.data.local.entity.ScheduleEntity
import com.nasbus.app.data.local.model.ScheduleWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduleEntity>)

    @Query("""
        SELECT 
            sch.scheduleId AS scheduleId,
            sch.routeId AS routeId,
            r.routeName AS routeName,
            c.companyName AS companyName,
            sch.busId AS busId,
            r.originName AS originName,
            r.destinationName AS destinationName,
            sch.departureTime AS departureTime,
            sch.arrivalTime AS arrivalTime,
            sch.arrivalNextDay AS arrivalNextDay,
            sch.estimatedFare AS estimatedFare
        FROM schedules sch
        INNER JOIN routes r ON sch.routeId = r.routeId
        INNER JOIN companies c ON sch.companyId = c.companyId
        WHERE (:companyId IS NULL OR sch.companyId = :companyId)
          AND (
              :origin IS NULL 
              OR r.originName = :origin 
              OR EXISTS (SELECT 1 FROM stops s1 WHERE s1.routeId = sch.routeId AND s1.stopName = :origin)
          )
          AND (
              :destination IS NULL 
              OR r.destinationName = :destination 
              OR EXISTS (SELECT 1 FROM stops s2 WHERE s2.routeId = sch.routeId AND s2.stopName = :destination)
          )
          AND (
              :departAfter IS NULL 
              OR (CAST(substr(sch.departureTime, 1, 2) AS INTEGER) * 60 + CAST(substr(sch.departureTime, 4, 2) AS INTEGER)) >= (CAST(substr(:departAfter, 1, 2) AS INTEGER) * 60 + CAST(substr(:departAfter, 4, 2) AS INTEGER))
          )
          AND (
              :arriveBefore IS NULL 
              OR (CAST(substr(sch.arrivalTime, 1, 2) AS INTEGER) * 60 + CAST(substr(sch.arrivalTime, 4, 2) AS INTEGER) + CASE WHEN sch.arrivalNextDay = 1 THEN 1440 ELSE 0 END) <= (CAST(substr(:arriveBefore, 1, 2) AS INTEGER) * 60 + CAST(substr(:arriveBefore, 4, 2) AS INTEGER))
          )
        ORDER BY (CAST(substr(sch.departureTime, 1, 2) AS INTEGER) * 60 + CAST(substr(sch.departureTime, 4, 2) AS INTEGER)) ASC
    """)
    fun filterSchedules(
        companyId: Int? = null,
        origin: String? = null,
        destination: String? = null,
        departAfter: String? = null,
        arriveBefore: String? = null
    ): Flow<List<ScheduleWithDetails>>
}
