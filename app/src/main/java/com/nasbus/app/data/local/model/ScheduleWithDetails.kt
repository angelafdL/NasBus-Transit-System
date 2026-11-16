package com.nasbus.app.data.local.model

data class ScheduleWithDetails(
    val scheduleId: Int,
    val routeId: Int,
    val routeName: String,
    val companyName: String,
    val busId: String,
    val originName: String,
    val destinationName: String,
    val departureTime: String,
    val arrivalTime: String,
    val arrivalNextDay: Boolean,
    val estimatedFare: Double,
    val distanceKm: Double = 0.0,
    val farePerKm: Double = 0.0
)
