package com.nasbus.app.data.local.model

import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.StopEntity

data class RouteDetailDataModel(
    val route: RouteEntity,
    val companyName: String,
    val stops: List<StopEntity>
)
