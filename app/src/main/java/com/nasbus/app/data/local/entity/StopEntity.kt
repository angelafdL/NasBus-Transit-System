package com.nasbus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["routeId"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("routeId"),
        Index("stopName")
    ]
)
data class StopEntity(
    @PrimaryKey
    val stopId: Int,
    val routeId: Int,
    val stopName: String,
    val latitude: Double,
    val longitude: Double,
    val sequenceOrder: Int,
    val distanceFromOriginKm: Double
)
