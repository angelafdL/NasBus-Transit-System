package com.nasbus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["routeId"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["companyId"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BusEntity::class,
            parentColumns = ["busId"],
            childColumns = ["busId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("routeId"),
        Index("companyId"),
        Index("busId"),
        Index("departureTime")
    ]
)
data class ScheduleEntity(
    @PrimaryKey
    val scheduleId: Int,
    val routeId: Int,
    val companyId: Int,
    val busId: String,
    val departureTime: String,
    val arrivalTime: String,
    val arrivalNextDay: Boolean,
    val estimatedFare: Double
)
