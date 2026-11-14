package com.nasbus.app.util

import kotlin.math.roundToInt

object TravelTimeEstimator {

    /**
     * Parses route estimated travel time strings like "3 to 4 hours", "1 to 2 hours", "4 to 5 hours"
     * into a midpoint in minutes.
     */
    fun parseRouteMidpointMinutes(estimatedTravelTime: String): Double {
        val rangeRegex = Regex("""(\d+(?:\.\d+)?)\s*to\s*(\d+(?:\.\d+)?)\s*hours?""", RegexOption.IGNORE_CASE)
        val rangeMatch = rangeRegex.find(estimatedTravelTime)
        if (rangeMatch != null) {
            val minHours = rangeMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            val maxHours = rangeMatch.groupValues[2].toDoubleOrNull() ?: 2.0
            return ((minHours + maxHours) / 2.0) * 60.0
        }

        val singleRegex = Regex("""(\d+(?:\.\d+)?)\s*hours?""", RegexOption.IGNORE_CASE)
        val singleMatch = singleRegex.find(estimatedTravelTime)
        if (singleMatch != null) {
            val hours = singleMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            return hours * 60.0
        }

        return 180.0 // Default 3 hours if unparseable
    }

    /**
     * Estimates segment travel time in minutes based on distance ratio,
     * rounded to the nearest 5 minutes.
     */
    fun estimateSegmentMinutes(
        segmentDistanceKm: Double,
        routeDistanceKm: Double,
        routeEstimatedTravelTime: String
    ): Int {
        if (routeDistanceKm <= 0.0) return 0
        val routeMidpointMinutes = parseRouteMidpointMinutes(routeEstimatedTravelTime)
        val rawSegmentMinutes = (segmentDistanceKm / routeDistanceKm) * routeMidpointMinutes
        val roundedTo5 = ((rawSegmentMinutes / 5.0).roundToInt() * 5)
        return roundedTo5.coerceAtLeast(5)
    }

    /**
     * Formats estimated segment minutes into strings like "About 1 hr 5 min" or "About 45 min".
     */
    fun formatEstimatedSegmentTime(segmentMinutes: Int): String {
        if (segmentMinutes < 60) {
            return "About $segmentMinutes min"
        }
        val hours = segmentMinutes / 60
        val mins = segmentMinutes % 60
        return if (mins > 0) {
            "About $hours hr $mins min"
        } else {
            "About $hours hr"
        }
    }
}
