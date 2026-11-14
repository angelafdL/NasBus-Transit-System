package com.nasbus.app.data.local.model

data class SegmentResult(
    val routeId: Int,
    val routeName: String,
    val companyName: String,
    val fromStopName: String,
    val toStopName: String,
    val segmentDistanceKm: Double,
    val routeDistanceKm: Double = 0.0,
    val segmentFare: Double,
    val estimatedTravelTime: String
)
