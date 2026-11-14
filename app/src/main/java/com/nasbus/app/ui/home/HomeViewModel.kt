package com.nasbus.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.model.SegmentResult
import com.nasbus.app.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Results(val list: List<SegmentResult>) : HomeUiState()
    object Empty : HomeUiState()
    data class Error(
        val message: String,
        val fromError: String? = null,
        val toError: String? = null
    ) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouteRepository(application)

    private val _distinctStops = MutableStateFlow<List<String>>(emptyList())
    val distinctStops: StateFlow<List<String>> = _distinctStops.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var fromText: String = ""
        private set

    var toText: String = ""
        private set

    private var searchJob: Job? = null

    init {
        loadStops()
    }

    private fun loadStops() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getDistinctStops().collect { stops ->
                _distinctStops.value = stops
            }
        }
    }

    fun onFromTextChanged(text: String) {
        fromText = text
        scheduleSearchDebounced()
    }

    fun onToTextChanged(text: String) {
        toText = text
        scheduleSearchDebounced()
    }

    fun swapStops() {
        val temp = fromText
        fromText = toText
        toText = temp
        scheduleSearchDebounced()
    }

    private fun scheduleSearchDebounced() {
        searchJob?.cancel()

        val from = fromText.trim()
        val to = toText.trim()

        if (from.isEmpty() || to.isEmpty()) {
            _uiState.value = HomeUiState.Idle
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300) // 300 ms debounce

            val availableStops = _distinctStops.value
            val fromValid = availableStops.any { it.equals(from, ignoreCase = true) }
            val toValid = availableStops.any { it.equals(to, ignoreCase = true) }

            if (!fromValid) {
                _uiState.value = HomeUiState.Error(
                    message = "Please select a valid stop",
                    fromError = "Please select a valid stop"
                )
                return@launch
            }

            if (!toValid) {
                _uiState.value = HomeUiState.Error(
                    message = "Please select a valid stop",
                    toError = "Please select a valid stop"
                )
                return@launch
            }

            if (from.equals(to, ignoreCase = true)) {
                _uiState.value = HomeUiState.Error(
                    message = "Origin and destination cannot be the same",
                    toError = "Origin and destination cannot be the same"
                )
                return@launch
            }

            _uiState.value = HomeUiState.Loading

            // Match case-insensitive exact stop name from distinct list
            val matchedFrom = availableStops.first { it.equals(from, ignoreCase = true) }
            val matchedTo = availableStops.first { it.equals(to, ignoreCase = true) }

            repository.findSegments(matchedFrom, matchedTo).collectLatest { results ->
                val sorted = results.sortedWith(
                    compareBy<SegmentResult> { it.companyName }.thenBy { it.segmentFare }
                )

                if (sorted.isEmpty()) {
                    _uiState.value = HomeUiState.Empty
                } else {
                    _uiState.value = HomeUiState.Results(sorted)
                }
            }
        }
    }
}
