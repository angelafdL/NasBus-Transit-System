package com.nasbus.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TravelTimeEstimatorTest {

    @Test
    fun testParseRouteMidpointMinutes() {
        assertEquals(210.0, TravelTimeEstimator.parseRouteMidpointMinutes("3 to 4 hours"), 0.001)
        assertEquals(90.0, TravelTimeEstimator.parseRouteMidpointMinutes("1 to 2 hours"), 0.001)
        assertEquals(270.0, TravelTimeEstimator.parseRouteMidpointMinutes("4 to 5 hours"), 0.001)
    }

    @Test
    fun testEstimateTimeAtStop() {
        // Departure 03:00 (180 mins), stop at 47.0 km on a 104.8 km route with travel time "3 to 4 hours" (210 mins)
        // Elapsed = (47.0 / 104.8) * 210 = 94.1788 mins -> Total = 274.1788 mins -> Rounded to nearest 5 = 275 mins
        val totalMins = TravelTimeEstimator.estimateTimeAtStop("03:00", 47.0, 104.8, "3 to 4 hours")
        assertEquals(275, totalMins)

        val formattedClock = TravelTimeEstimator.formatClock(totalMins)
        assertEquals("4:35 AM", formattedClock)
    }

    @Test
    fun testPlusOneDayRule() {
        // 1445 minutes = 24 hours 5 mins -> 12:05 AM +1 day
        val formatted = TravelTimeEstimator.formatClock(1445)
        assertEquals("12:05 AM +1 day", formatted)

        val midnightFormatted = TravelTimeEstimator.formatClock(1440)
        assertEquals("12:00 AM +1 day", midnightFormatted)
    }

    @Test
    fun test12HourFormatting() {
        assertEquals("12:00 AM", TravelTimeEstimator.formatClock(0))
        assertEquals("6:00 AM", TravelTimeEstimator.formatClock(360))
        assertEquals("12:00 PM", TravelTimeEstimator.formatClock(720))
        assertEquals("11:55 PM", TravelTimeEstimator.formatClock(1435))
    }

    @Test
    fun testRoundingToNearestFiveMinutes() {
        val mins1 = TravelTimeEstimator.estimateSegmentMinutes(12.5, 104.8, "3 to 4 hours")
        // (12.5 / 104.8) * 210 = 25.0477 -> rounded to nearest 5 = 25
        assertEquals(25, mins1)
    }
}
