package com.nasbus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey
    val companyId: Int,
    val companyName: String,
    val contactInfo: String,
    val busPrefix: String
)
