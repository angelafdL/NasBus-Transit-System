package com.nasbus.app.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.entity.CompanyEntity
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.model.ScheduleWithDetails
import com.nasbus.app.data.repository.RouteRepository
import com.nasbus.app.util.TravelTimeEstimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScheduleUiState {
    object Loading : ScheduleUiState()
    data class Success(
        val list: List<ScheduleWithDetails>,
        val totalCount: Int
    ) : ScheduleUiState()
    data class ValidationError(val message: String) : ScheduleUiState()
    data class Error(val message: String) : ScheduleUiState()
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouteRepository(application)

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Filter choices state
    var selectedCompany: CompanyEntity? = null
        private set
    var selectedFrom: String? = null
        private set
    var selectedTo: String? = null
        private set
    var departAfter24h: String? = null
        private set
    var departAfter12h: String? = null
        private set
    var arriveBefore24h: String? = null
        private set
    var arriveBefore12h: String? = null
        private set
    var isFilterExpanded: Boolean = true

    private val _allCompanies = MutableStateFlow<List<CompanyEntity>>(emptyList())
    val allCompanies: StateFlow<List<CompanyEntity>> = _allCompanies.asStateFlow()

    private val _allRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val allRoutes: StateFlow<List<RouteEntity>> = _allRoutes.asStateFlow()

    private val _availableFromStops = MutableStateFlow<List<String>>(emptyList())
    val availableFromStops: StateFlow<List<String>> = _availableFromStops.asStateFlow()

    private val _availableToStops = MutableStateFlow<List<String>>(emptyList())
    val availableToStops: StateFlow<List<String>> = _availableToStops.asStateFlow()

    init {
        loadMetadataAndObserve()
    }

    private fun loadMetadataAndObserve() {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                repository.getAllCompanies().collect { companies ->
                    _allCompanies.value = companies
                }
            }
            launch {
                repository.getAllRoutes().collect { routes ->
                    _allRoutes.value = routes
                    updateAvailableTerminals()
                }
            }
        }
        applyFilters()
    }

    private fun updateAvailableTerminals() {
        val routes = _allRoutes.value
        val companyId = selectedCompany?.companyId

        val filteredRoutes = if (companyId != null) {
            routes.filter { it.companyId == companyId }
        } else {
            routes
        }

        val origins = filteredRoutes.map { it.originName }.distinct().sorted()
        val destinations = filteredRoutes.map { it.destinationName }.distinct().sorted()

        _availableFromStops.value = listOf("Any") + origins
        _availableToStops.value = listOf("Any") + destinations

        if (selectedFrom != null && selectedFrom != "Any" && !origins.contains(selectedFrom)) {
            selectedFrom = null
        }
        if (selectedTo != null && selectedTo != "Any" && !destinations.contains(selectedTo)) {
            selectedTo = null
        }
    }

    fun onCompanySelected(companyName: String) {
        selectedCompany = if (companyName == "All companies" || companyName.isBlank()) {
            null
        } else {
            _allCompanies.value.find { it.companyName.equals(companyName, ignoreCase = true) }
        }
        updateAvailableTerminals()
        applyFilters()
    }

    fun onFromSelected(from: String) {
        selectedFrom = if (from == "Any" || from.isBlank()) null else from
        applyFilters()
    }

    fun onToSelected(to: String) {
        selectedTo = if (to == "Any" || to.isBlank()) null else to
        applyFilters()
    }

    fun swapTerminals() {
        val temp = selectedFrom
        selectedFrom = selectedTo
        selectedTo = temp
        applyFilters()
    }

    fun setDepartAfter(hourOfDay: Int, minute: Int) {
        val mins = hourOfDay * 60 + minute
        departAfter24h = String.format("%02d:%02d", hourOfDay, minute)
        departAfter12h = TravelTimeEstimator.formatClock(mins)
        applyFilters()
    }

    fun clearDepartAfter() {
        departAfter24h = null
        departAfter12h = null
        applyFilters()
    }

    fun setArriveBefore(hourOfDay: Int, minute: Int) {
        val mins = hourOfDay * 60 + minute
        arriveBefore24h = String.format("%02d:%02d", hourOfDay, minute)
        arriveBefore12h = TravelTimeEstimator.formatClock(mins)
        applyFilters()
    }

    fun clearArriveBefore() {
        arriveBefore24h = null
        arriveBefore12h = null
        applyFilters()
    }

    fun clearAllFilters() {
        selectedCompany = null
        selectedFrom = null
        selectedTo = null
        departAfter24h = null
        departAfter12h = null
        arriveBefore24h = null
        arriveBefore12h = null
        updateAvailableTerminals()
        applyFilters()
    }

    fun applyFilters() {
        // Validate time range if both set
        if (departAfter24h != null && arriveBefore24h != null) {
            val depMins = TravelTimeEstimator.parseClockToMinutes(departAfter24h!!)
            val arrMins = TravelTimeEstimator.parseClockToMinutes(arriveBefore24h!!)
            if (depMins > arrMins) {
                _uiState.value = ScheduleUiState.ValidationError("Depart after must be earlier than Arrive before")
                return
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ScheduleUiState.Loading
            try {
                repository.filterSchedules(
                    companyId = selectedCompany?.companyId,
                    origin = selectedFrom,
                    destination = selectedTo,
                    departAfter = departAfter24h,
                    arriveBefore = arriveBefore24h
                ).collect { list ->
                    _uiState.value = ScheduleUiState.Success(list, list.size)
                }
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.localizedMessage ?: "Failed to load schedules")
            }
        }
    }
}
