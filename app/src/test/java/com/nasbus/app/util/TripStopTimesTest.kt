package com.nasbus.app.util

import com.nasbus.app.data.local.entity.StopEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStopTimesTest {

    private val stops = listOf(
        StopEntity(stopId = 1, routeId = 1, stopName = "Terminal Origin", latitude = 14.0, longitude = 120.6, sequenceOrder = 1, distanceFromOriginKm = 0.0),
        StopEntity(stopId = 2, routeId = 1, stopName = "Intermediate Stop 1", latitude = 14.1, longitude = 120.7, sequenceOrder = 2, distanceFromOriginKm = 25.0),
        StopEntity(stopId = 3, routeId = 1, stopName = "Intermediate Stop 2", latitude = 14.2, longitude = 120.8, sequenceOrder = 3, distanceFromOriginKm = 60.0),
        StopEntity(stopId = 4, routeId = 1, stopName = "Terminal Destination", latitude = 14.3, longitude = 120.9, sequenceOrder = 4, distanceFromOriginKm = 100.0)
    )

    @Test
    fun testFirstStopExactDeparture() {
        val depTime = "06:00"
        val firstStopMins = TravelTimeEstimator.estimateTimeAtStop(depTime, stops[0].distanceFromOriginKm, 100.0, "3 to 4 hours")
        val depClock = TravelTimeEstimator.formatClock(firstStopMins)
        assertEquals("6:00 AM", depClock)
    }

    @Test
    fun testLastStopEstimatedArrival() {
        val depTime = "22:00" // 10:00 PM (1320 mins)
        // Midpoint of 3 to 4 hours = 210 mins. 1320 + 210 = 1530 mins = 1:30 AM +1 day
        val lastStopMins = TravelTimeEstimator.estimateTimeAtStop(depTime, stops.last().distanceFromOriginKm, 100.0, "3 to 4 hours")
        val arrClock = TravelTimeEstimator.formatClock(lastStopMins)

        assertTrue(arrClock.contains("+1 day"))
        assertEquals("1:30 AM +1 day", arrClock)
    }

    @Test
    fun testTimesMonotonicallyNonDecreasing() {
        val depTime = "08:00"
        val calculatedMins = stops.map { stop ->
            TravelTimeEstimator.estimateTimeAtStop(depTime, stop.distanceFromOriginKm, 100.0, "3 to 4 hours")
        }

        for (i in 0 until calculatedMins.size - 1) {
            assertTrue("Time at stop $i should be <= time at stop ${i + 1}", calculatedMins[i] <= calculatedMins[i + 1])
        }
    }
}
