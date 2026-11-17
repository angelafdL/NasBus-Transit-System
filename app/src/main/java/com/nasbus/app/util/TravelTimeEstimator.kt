package com.nasbus.app.util

import com.nasbus.app.data.local.model.RouteTripItem
import com.nasbus.app.data.local.model.ScheduleWithDetails
import java.util.Locale
import kotlin.math.roundToInt

object TravelTimeEstimator {

    const val MAX_TRIPS = 5

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
     * Converts a 24-hour time string "HH:mm" into total minutes since midnight.
     */
    fun parseClockToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val hours = parts[0].trim().toIntOrNull() ?: 0
        val mins = parts[1].trim().toIntOrNull() ?: 0
        return hours * 60 + mins
    }

    /**
     * Formats total minutes since midnight into 12-hour clock format "h:mm AM/PM".
     * If totalMinutes >= 1440, appends "+1 day".
     */
    fun formatClock(totalMinutes: Int): String {
        val isNextDay = totalMinutes >= 1440
        val normalizedMins = totalMinutes % 1440
        val hours24 = normalizedMins / 60
        val mins = normalizedMins % 60

        val amPm = if (hours24 >= 12) "PM" else "AM"
        val hours12 = when {
            hours24 == 0 -> 12
            hours24 > 12 -> hours24 - 12
            else -> hours24
        }
        val formattedMins = String.format(Locale.US, "%02d", mins)
        val clockText = "$hours12:$formattedMins $amPm"

        return if (isNextDay) "$clockText +1 day" else clockText
    }

    /**
     * Formats countdown text given minute difference between departure and current time.
     * "now", "in 25 min", "in 1 hr 10 min", etc.
     */
    fun formatCountdown(diffMinutes: Int): String {
        return when {
            diffMinutes <= 0 -> "now"
            diffMinutes < 60 -> "in $diffMinutes min"
            else -> {
                val hours = diffMinutes / 60
                val mins = diffMinutes % 60
                if (mins > 0) "in $hours hr $mins min" else "in $hours hr"
            }
        }
    }

    /**
     * Estimates time in minutes since midnight at a specific stop given schedule departure,
     * stop distance, total route distance, and route travel time string.
     */
    fun estimateTimeAtStop(
        scheduleDepartureTimeStr: String,
        stopDistanceFromOriginKm: Double,
        routeDistanceKm: Double,
        routeEstimatedTravelTime: String
    ): Int {
        val schDepMins = parseClockToMinutes(scheduleDepartureTimeStr)
        if (routeDistanceKm <= 0.0) return schDepMins

        val routeMidpointMins = parseRouteMidpointMinutes(routeEstimatedTravelTime)
        val elapsedMins = (stopDistanceFromOriginKm / routeDistanceKm) * routeMidpointMins
        val rawTotalMins = schDepMins + elapsedMins
        val roundedTo5 = ((rawTotalMins / 5.0).roundToInt() * 5)

        return roundedTo5
    }

    /**
     * Pure function returning ONLY relevant trips for the current device time (max 5),
     * with their departure, arrival, countdown, status line, and Next bus badge indicator.
     */
    fun getRelevantTripItems(
        schedules: List<ScheduleWithDetails>,
        routeDistanceKm: Double,
        routeEstimatedTravelTime: String,
        routeOriginName: String,
        routeDestinationName: String,
        fromStopName: String,
        toStopName: String,
        fromStopDistanceFromOriginKm: Double,
        toStopDistanceFromOriginKm: Double,
        currentMinutes: Int
    ): List<RouteTripItem> {
        val relevant = schedules.mapNotNull { sch ->
            val depMins = estimateTimeAtStop(
                sch.departureTime,
                fromStopDistanceFromOriginKm,
                routeDistanceKm,
                routeEstimatedTravelTime
            )

            // A trip is relevant when estimated clock time at From stop is >= currentMinutes
            if (depMins < currentMinutes) return@mapNotNull null

            val arrMins = estimateTimeAtStop(
                sch.departureTime,
                toStopDistanceFromOriginKm,
                routeDistanceKm,
                routeEstimatedTravelTime
            )
            val originDepMins = parseClockToMinutes(sch.departureTime)
            val diffMins = depMins - currentMinutes
            val isOnTheWay = originDepMins <= currentMinutes

            RouteTripItem(
                scheduleId = sch.scheduleId,
                busId = sch.busId,
                fromStopName = if (fromStopName.isNotBlank()) fromStopName else routeOriginName,
                toStopName = if (toStopName.isNotBlank()) toStopName else routeDestinationName,
                departsFormatted = formatClock(depMins),
                countdownText = formatCountdown(diffMins),
                arrivesFormatted = formatClock(arrMins),
                originName = routeOriginName,
                originDepartureTimeFormatted = formatClock(originDepMins),
                departsMinutesSinceMidnight = depMins,
                isOnTheWay = isOnTheWay,
                isNextBus = false
            )
        }.take(MAX_TRIPS)

        return relevant.mapIndexed { index, item ->
            item.copy(isNextBus = (index == 0))
        }
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
