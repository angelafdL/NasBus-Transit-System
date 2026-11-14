package com.nasbus.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.StopEntity
import com.nasbus.app.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val isLoading: Boolean = true,
    val route: RouteEntity? = null,
    val companyName: String = "",
    val stops: List<StopEntity> = emptyList(),
    val fromStopName: String? = null,
    val toStopName: String? = null,
    val error: String? = null
)

class RouteMapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouteRepository(application)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadRouteData(routeId: Int, fromStopName: String? = null, toStopName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                fromStopName = fromStopName,
                toStopName = toStopName
            )
            try {
                repository.getRouteDetailData(routeId).collect { detailData ->
                    if (detailData != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            route = detailData.route,
                            companyName = detailData.companyName,
                            stops = detailData.stops,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Route not found for ID $routeId"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Error loading route map"
                )
            }
        }
    }
}
