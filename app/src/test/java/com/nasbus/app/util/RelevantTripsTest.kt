package com.nasbus.app.util

import com.nasbus.app.data.local.model.ScheduleWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelevantTripsTest {

    private val testSchedules = listOf(
        createSchedule(1, "03:00", "06:30", "DB-001"),
        createSchedule(2, "04:00", "07:30", "DB-002"),
        createSchedule(3, "05:00", "08:30", "DB-003"),
        createSchedule(4, "06:00", "09:30", "DB-004"),
        createSchedule(5, "06:30", "10:00", "DB-005"), // Reaches Tagaytay at ~8:05 AM (485 mins)
        createSchedule(6, "07:00", "10:30", "DB-006"), // Reaches Tagaytay at ~8:35 AM (515 mins)
        createSchedule(7, "08:00", "11:30", "DB-007"),
        createSchedule(8, "09:00", "12:30", "DB-008")
    )

    @Test
    fun testRelevantTripsAt810AM() {
        // At 8:10 AM (490 mins):
        // 06:30 departure reaches From stop at 485 mins (8:05 AM) -> < 490 mins -> EXCLUDED
        // 07:00 departure reaches From stop at 515 mins (8:35 AM) -> >= 490 mins -> RELEVANT (First!)
        val relevant = TravelTimeEstimator.getRelevantTripItems(
            schedules = testSchedules,
            routeDistanceKm = 104.8,
            routeEstimatedTravelTime = "3 to 4 hours",
            routeOriginName = "Nasugbu",
            routeDestinationName = "Buendia",
            fromStopName = "Tagaytay",
            toStopName = "Buendia",
            fromStopDistanceFromOriginKm = 47.0,
            toStopDistanceFromOriginKm = 104.8,
            currentMinutes = 490 // 8:10 AM
        )

        assertTrue(relevant.isNotEmpty())
        val firstTrip = relevant.first()
        assertEquals("DB-006", firstTrip.busId)
        assertTrue(firstTrip.isNextBus)
        assertEquals("8:35 AM", firstTrip.departsFormatted)
        assertEquals("in 25 min", firstTrip.countdownText)

        // Ensure 06:30 departure (DB-005) is excluded
        assertTrue(relevant.none { it.busId == "DB-005" })
    }

    @Test
    fun testRelevantTripsAt1155PM() {
        // At 11:55 PM (1435 mins), no more schedules today
        val relevant = TravelTimeEstimator.getRelevantTripItems(
            schedules = testSchedules,
            routeDistanceKm = 104.8,
            routeEstimatedTravelTime = "3 to 4 hours",
            routeOriginName = "Nasugbu",
            routeDestinationName = "Buendia",
            fromStopName = "Nasugbu",
            toStopName = "Buendia",
            fromStopDistanceFromOriginKm = 0.0,
            toStopDistanceFromOriginKm = 104.8,
            currentMinutes = 1435 // 11:55 PM
        )

        assertTrue(relevant.isEmpty())
    }

    @Test
    fun testRelevantTripsAt200AM() {
        // At 2:00 AM (120 mins), first trip is 03:00 AM departure
        val relevant = TravelTimeEstimator.getRelevantTripItems(
            schedules = testSchedules,
            routeDistanceKm = 104.8,
            routeEstimatedTravelTime = "3 to 4 hours",
            routeOriginName = "Nasugbu",
            routeDestinationName = "Buendia",
            fromStopName = "Nasugbu",
            toStopName = "Buendia",
            fromStopDistanceFromOriginKm = 0.0,
            toStopDistanceFromOriginKm = 104.8,
            currentMinutes = 120 // 2:00 AM
        )

        assertTrue(relevant.isNotEmpty())
        assertEquals("DB-001", relevant.first().busId)
        assertEquals("3:00 AM", relevant.first().departsFormatted)
    }

    @Test
    fun testMaxFiveTripsReturned() {
        val relevant = TravelTimeEstimator.getRelevantTripItems(
            schedules = testSchedules,
            routeDistanceKm = 104.8,
            routeEstimatedTravelTime = "3 to 4 hours",
            routeOriginName = "Nasugbu",
            routeDestinationName = "Buendia",
            fromStopName = "Nasugbu",
            toStopName = "Buendia",
            fromStopDistanceFromOriginKm = 0.0,
            toStopDistanceFromOriginKm = 104.8,
            currentMinutes = 0 // 12:00 AM midnight
        )

        assertTrue(relevant.size <= 5)
        assertEquals(5, relevant.size)
    }

    private fun createSchedule(
        id: Int,
        dep: String,
        arr: String,
        busId: String,
        nextDay: Boolean = false
    ): ScheduleWithDetails {
        return ScheduleWithDetails(
            scheduleId = id,
            routeId = 1,
            routeName = "Nasugbu to Buendia",
            companyName = "DLTB Co.",
            busId = busId,
            originName = "Nasugbu",
            destinationName = "Buendia",
            departureTime = dep,
            arrivalTime = arr,
            arrivalNextDay = nextDay,
            estimatedFare = 220.0,
            distanceKm = 104.8,
            farePerKm = 2.10
        )
    }
}
