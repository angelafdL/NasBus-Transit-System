package com.nasbus.app.data.local.model

import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.StopEntity

data class TripDetailDataModel(
    val schedule: ScheduleWithDetails,
    val route: RouteEntity,
    val stops: List<StopEntity>
)
