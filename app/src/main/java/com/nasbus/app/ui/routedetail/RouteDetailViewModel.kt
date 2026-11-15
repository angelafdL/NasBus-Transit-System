package com.nasbus.app.ui.routedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.model.RouteDetailDataModel
import com.nasbus.app.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TripFilter {
    UPCOMING,
    ALL,
    PREVIOUS
}

sealed class RouteDetailUiState {
    object Loading : RouteDetailUiState()
    data class Success(
        val data: RouteDetailDataModel,
        val fromStopName: String,
        val toStopName: String
    ) : RouteDetailUiState()
    data class Error(val message: String) : RouteDetailUiState()
}

class RouteDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouteRepository(application)

    private val _uiState = MutableStateFlow<RouteDetailUiState>(RouteDetailUiState.Loading)
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    var selectedTripFilter: TripFilter = TripFilter.UPCOMING

    fun loadRouteDetail(routeId: Int, fromStopName: String, toStopName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = RouteDetailUiState.Loading
            try {
                repository.getRouteDetailData(routeId).collect { detailData ->
                    if (detailData != null) {
                        _uiState.value = RouteDetailUiState.Success(
                            data = detailData,
                            fromStopName = fromStopName,
                            toStopName = toStopName
                        )
                    } else {
                        _uiState.value = RouteDetailUiState.Error("Route details not found for ID $routeId")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RouteDetailUiState.Error(e.localizedMessage ?: "Failed to load route details")
            }
        }
    }
}
