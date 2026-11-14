package com.nasbus.app.data.repository

import android.content.Context
import com.nasbus.app.data.local.db.AppDatabase
import com.nasbus.app.data.local.entity.CompanyEntity
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.StopEntity
import com.nasbus.app.data.local.model.RouteDetailDataModel
import com.nasbus.app.data.local.model.RouteListItem
import com.nasbus.app.data.local.model.SegmentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class RouteRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val routeDao = db.routeDao()
    private val stopDao = db.stopDao()
    private val companyDao = db.companyDao()

    fun getRouteById(routeId: Int): Flow<RouteEntity?> = flow {
        emit(routeDao.getRouteWithDetails(routeId))
    }.flowOn(Dispatchers.IO)

    fun getRouteDetailData(routeId: Int): Flow<RouteDetailDataModel?> = flow {
        val route = routeDao.getRouteWithDetails(routeId)
        if (route != null) {
            val company = companyDao.getCompanyById(route.companyId)
            val companyName = company?.companyName ?: "Bus Company"
            val stops = stopDao.getStopsForRouteList(routeId)
            emit(RouteDetailDataModel(route, companyName, stops))
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun getStopsForRoute(routeId: Int): Flow<List<StopEntity>> {
        return stopDao.getStopsForRoute(routeId)
    }

    fun getAllRouteListItems(): Flow<List<RouteListItem>> {
        return routeDao.getAllRouteListItems()
    }

    fun findSegments(from: String, to: String): Flow<List<SegmentResult>> {
        return routeDao.findSegments(from, to)
    }

    fun searchRoutes(query: String): Flow<List<RouteEntity>> {
        return routeDao.searchByText(query)
    }

    fun getAllCompanies(): Flow<List<CompanyEntity>> {
        return companyDao.getAll()
    }

    fun getDistinctStops(): Flow<List<String>> {
        return stopDao.getDistinctStopNames()
    }
}
