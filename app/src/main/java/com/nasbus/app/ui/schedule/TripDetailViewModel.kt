package com.nasbus.app.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.model.TripDetailDataModel
import com.nasbus.app.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TripDetailUiState {
    object Loading : TripDetailUiState()
    data class Success(val data: TripDetailDataModel) : TripDetailUiState()
    data class Error(val message: String) : TripDetailUiState()
}

class TripDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouteRepository(application)

    private val _uiState = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    fun loadTripDetail(scheduleId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = TripDetailUiState.Loading
            try {
                repository.getTripDetailData(scheduleId).collect { data ->
                    if (data != null) {
                        _uiState.value = TripDetailUiState.Success(data)
                    } else {
                        _uiState.value = TripDetailUiState.Error("Trip schedule not found.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TripDetailUiState.Error(e.localizedMessage ?: "Failed to load trip detail.")
            }
        }
    }
}
