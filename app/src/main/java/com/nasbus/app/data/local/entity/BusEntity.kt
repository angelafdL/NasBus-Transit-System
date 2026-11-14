package com.nasbus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buses",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["companyId"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("companyId")
    ]
)
data class BusEntity(
    @PrimaryKey
    val busId: String,
    val companyId: Int,
    val busNumber: Int
)
