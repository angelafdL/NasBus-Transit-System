package com.nasbus.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nasbus.app.data.local.dao.CompanyDao
import com.nasbus.app.data.local.dao.RouteDao
import com.nasbus.app.data.local.dao.ScheduleDao
import com.nasbus.app.data.local.dao.StopDao
import com.nasbus.app.data.local.db.AppDatabase
import com.nasbus.app.data.local.entity.CompanyEntity
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.ScheduleEntity
import com.nasbus.app.data.local.entity.StopEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var routeDao: RouteDao
    private lateinit var stopDao: StopDao
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var companyDao: CompanyDao

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        routeDao = db.routeDao()
        stopDao = db.stopDao()
        scheduleDao = db.scheduleDao()
        companyDao = db.companyDao()

        seedTestData()
    }

    private suspend fun seedTestData() {
        companyDao.insertAll(
            listOf(
                CompanyEntity(companyId = 1, companyName = "DLTB Co.", contactInfo = "123", busPrefix = "DB"),
                CompanyEntity(companyId = 2, companyName = "Batman Express", contactInfo = "456", busPrefix = "BE")
            )
        )

        routeDao.insertAll(
            listOf(
                // Route 1: Nasugbu -> Buendia (Outbound)
                RouteEntity(
                    routeId = 1,
                    companyId = 1,
                    routeName = "Nasugbu to LRT Buendia",
                    originName = "Nasugbu",
                    destinationName = "Buendia",
                    estimatedTravelTime = "3 to 4 hours",
                    pathPolyline = "",
                    distanceKm = 100.0,
                    farePerKm = 2.0,
                    direction = "OUTBOUND",
                    pairedRouteId = 2
                ),
                // Route 2: Buendia -> Nasugbu (Inbound / Reversed)
                RouteEntity(
                    routeId = 2,
                    companyId = 1,
                    routeName = "LRT Buendia to Nasugbu",
                    originName = "Buendia",
                    destinationName = "Nasugbu",
                    estimatedTravelTime = "3 to 4 hours",
                    pathPolyline = "",
                    distanceKm = 100.0,
                    farePerKm = 2.0,
                    direction = "INBOUND",
                    pairedRouteId = 1
                )
            )
        )

        stopDao.insertAll(
            listOf(
                // Route 1 stops
                StopEntity(stopId = 1, routeId = 1, stopName = "Nasugbu", latitude = 14.0, longitude = 120.6, sequenceOrder = 1, distanceFromOriginKm = 0.0),
                StopEntity(stopId = 2, routeId = 1, stopName = "Tagaytay", latitude = 14.1, longitude = 120.9, sequenceOrder = 2, distanceFromOriginKm = 40.0),
                StopEntity(stopId = 3, routeId = 1, stopName = "Buendia", latitude = 14.5, longitude = 120.9, sequenceOrder = 3, distanceFromOriginKm = 100.0),

                // Route 2 stops
                StopEntity(stopId = 4, routeId = 2, stopName = "Buendia", latitude = 14.5, longitude = 120.9, sequenceOrder = 1, distanceFromOriginKm = 0.0),
                StopEntity(stopId = 5, routeId = 2, stopName = "Tagaytay", latitude = 14.1, longitude = 120.9, sequenceOrder = 2, distanceFromOriginKm = 60.0),
                StopEntity(stopId = 6, routeId = 2, stopName = "Nasugbu", latitude = 14.0, longitude = 120.6, sequenceOrder = 3, distanceFromOriginKm = 100.0)
            )
        )

        scheduleDao.insertAll(
            listOf(
                ScheduleEntity(scheduleId = 1, routeId = 1, companyId = 1, busId = "BUS-1", departureTime = "06:00", arrivalTime = "09:30", arrivalNextDay = false, estimatedFare = 200.0),
                ScheduleEntity(scheduleId = 2, routeId = 1, companyId = 1, busId = "BUS-2", departureTime = "22:00", arrivalTime = "01:30", arrivalNextDay = true, estimatedFare = 200.0),
                ScheduleEntity(scheduleId = 3, routeId = 2, companyId = 2, busId = "BUS-3", departureTime = "14:00", arrivalTime = "17:30", arrivalNextDay = false, estimatedFare = 200.0)
            )
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testFindSegmentsFromBeforeTo() = runBlocking {
        val segments = routeDao.findSegments("Nasugbu", "Tagaytay").first()
        assertEquals(1, segments.size)

        val seg = segments.first()
        assertEquals(1, seg.routeId)
        assertEquals("Nasugbu", seg.fromStopName)
        assertEquals("Tagaytay", seg.toStopName)
        assertEquals(40.0, seg.segmentDistanceKm, 0.001)
    }

    @Test
    fun testFindSegmentsFromAfterToReturnsEmpty() = runBlocking {
        val segments = routeDao.findSegments("Tagaytay", "NonExistentStop").first()
        assertEquals(0, segments.size)
    }

    @Test
    fun testFindSegmentsReversedAfterSwapping() = runBlocking {
        val forward = routeDao.findSegments("Nasugbu", "Tagaytay").first()
        assertEquals(1, forward.first().routeId)

        val reversed = routeDao.findSegments("Tagaytay", "Nasugbu").first()
        assertEquals(2, reversed.first().routeId)
    }

    @Test
    fun testFilterSchedulesByCompany() = runBlocking {
        val company1Schedules = scheduleDao.filterSchedules(companyId = 1).first()
        assertEquals(2, company1Schedules.size)

        val company2Schedules = scheduleDao.filterSchedules(companyId = 2).first()
        assertEquals(1, company2Schedules.size)
    }

    @Test
    fun testFilterSchedulesByTimeRangeAndNextDay() = runBlocking {
        // Depart after 05:00 and Arrive before 12:00 PM (720 mins)
        val morningSchedules = scheduleDao.filterSchedules(departAfter = "05:00", arriveBefore = "12:00").first()
        assertEquals(1, morningSchedules.size)
        assertEquals(1, morningSchedules.first().scheduleId)

        // Overnight schedule (departure 22:00, arrival 01:30 next day = 1530 mins)
        val overnightSchedules = scheduleDao.filterSchedules(departAfter = "21:00", arriveBefore = "02:00").first()
        assertTrue(overnightSchedules.any { it.scheduleId == 2 && it.arrivalNextDay })
    }
}
