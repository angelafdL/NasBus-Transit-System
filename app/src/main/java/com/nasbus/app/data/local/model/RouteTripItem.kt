package com.nasbus.app.data.local.model

data class RouteTripItem(
    val scheduleId: Int,
    val busId: String,
    val fromStopName: String,
    val toStopName: String,
    val departsFormatted: String,
    val arrivesFormatted: String,
    val originName: String,
    val originDepartureTimeFormatted: String,
    val departsMinutesSinceMidnight: Int,
    val isNextBus: Boolean = false,
    val isPastToday: Boolean = false
)
