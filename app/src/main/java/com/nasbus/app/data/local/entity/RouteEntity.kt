package com.nasbus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["companyId"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("companyId"),
        Index("originName"),
        Index("destinationName")
    ]
)
data class RouteEntity(
    @PrimaryKey
    val routeId: Int,
    val companyId: Int,
    val routeName: String,
    val originName: String,
    val destinationName: String,
    val estimatedTravelTime: String,
    val pathPolyline: String,
    val distanceKm: Double,
    val farePerKm: Double = 2.45,
    val direction: String,
    val pairedRouteId: Int
)
