package com.nasbus.app.ui.schedule

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.entity.CompanyEntity
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.model.ScheduleWithDetails
import com.nasbus.app.data.repository.RouteRepository
import com.nasbus.app.util.TravelTimeEstimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleFilterState(
    val selectedCompany: CompanyEntity? = null,
    val selectedFrom: String? = null,
    val selectedTo: String? = null,
    val departAfter24h: String? = null,
    val departAfter12h: String? = null,
    val arriveBefore24h: String? = null,
    val arriveBefore12h: String? = null,
    val isFilterExpanded: Boolean = true
)

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

    private val _filterState = MutableStateFlow(ScheduleFilterState())
    val filterState: StateFlow<ScheduleFilterState> = _filterState.asStateFlow()

    private val _allCompanies = MutableStateFlow<List<CompanyEntity>>(emptyList())
    val allCompanies: StateFlow<List<CompanyEntity>> = _allCompanies.asStateFlow()

    private val _allRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val allRoutes: StateFlow<List<RouteEntity>> = _allRoutes.asStateFlow()

    private val _availableFromStops = MutableStateFlow<List<String>>(emptyList())
    val availableFromStops: StateFlow<List<String>> = _availableFromStops.asStateFlow()

    private val _availableToStops = MutableStateFlow<List<String>>(emptyList())
    val availableToStops: StateFlow<List<String>> = _availableToStops.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleUiState> = filterState
        .flatMapLatest { state ->
            if (state.departAfter24h != null && state.arriveBefore24h != null) {
                val depMins = TravelTimeEstimator.parseClockToMinutes(state.departAfter24h)
                val arrMins = TravelTimeEstimator.parseClockToMinutes(state.arriveBefore24h)
                if (depMins > arrMins) {
                    return@flatMapLatest flowOf(ScheduleUiState.ValidationError("Depart after must be earlier than Arrive before"))
                }
            }
            flow {
                emit(ScheduleUiState.Loading)
                repository.filterSchedules(
                    companyId = state.selectedCompany?.companyId,
                    origin = state.selectedFrom,
                    destination = state.selectedTo,
                    departAfter = state.departAfter24h,
                    arriveBefore = state.arriveBefore24h
                ).collect { list ->
                    emit(ScheduleUiState.Success(list, list.size))
                }
            }.catch { e ->
                emit(ScheduleUiState.Error(e.localizedMessage ?: "Failed to load schedules"))
            }.flowOn(Dispatchers.IO)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScheduleUiState.Loading
        )

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
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
    }

    private fun updateAvailableTerminals() {
        val routes = _allRoutes.value
        val companyId = _filterState.value.selectedCompany?.companyId

        val filteredRoutes = if (companyId != null) {
            routes.filter { it.companyId == companyId }
        } else {
            routes
        }

        // Distinct UNION of originName and destinationName so every terminal is available in both dropdowns
        val allTerminals = filteredRoutes.flatMap { listOf(it.originName, it.destinationName) }
            .distinct()
            .sorted()

        _availableFromStops.value = listOf("Any") + allTerminals
        _availableToStops.value = listOf("Any") + allTerminals

        val currentFrom = _filterState.value.selectedFrom
        val currentTo = _filterState.value.selectedTo

        val newFrom = if (currentFrom != null && currentFrom != "Any" && !allTerminals.contains(currentFrom)) null else currentFrom
        val newTo = if (currentTo != null && currentTo != "Any" && !allTerminals.contains(currentTo)) null else currentTo

        if (newFrom != currentFrom || newTo != currentTo) {
            _filterState.value = _filterState.value.copy(
                selectedFrom = newFrom,
                selectedTo = newTo
            )
        }
    }

    fun onCompanySelected(companyName: String) {
        val selected = if (companyName == "All companies" || companyName.isBlank()) {
            null
        } else {
            _allCompanies.value.find { it.companyName.equals(companyName, ignoreCase = true) }
        }
        _filterState.value = _filterState.value.copy(selectedCompany = selected)
        updateAvailableTerminals()
    }

    fun onFromSelected(from: String) {
        val newFrom = if (from == "Any" || from.isBlank()) null else from
        _filterState.value = _filterState.value.copy(selectedFrom = newFrom)
    }

    fun onToSelected(to: String) {
        val newTo = if (to == "Any" || to.isBlank()) null else to
        _filterState.value = _filterState.value.copy(selectedTo = newTo)
    }

    fun swapFromTo() {
        val currentFrom = _filterState.value.selectedFrom
        val currentTo = _filterState.value.selectedTo

        _filterState.value = _filterState.value.copy(
            selectedFrom = currentTo,
            selectedTo = currentFrom
        )
        updateAvailableTerminals()

        Log.d("Schedule", "swap state updated at ${SystemClock.elapsedRealtime()}")
        Log.d("Schedule", "swap: from=${_filterState.value.selectedFrom} to=${_filterState.value.selectedTo} company=${_filterState.value.selectedCompany?.companyName}")
    }

    fun setDepartAfter(hourOfDay: Int, minute: Int) {
        val mins = hourOfDay * 60 + minute
        val h24 = String.format("%02d:%02d", hourOfDay, minute)
        val h12 = TravelTimeEstimator.formatClock(mins)
        _filterState.value = _filterState.value.copy(
            departAfter24h = h24,
            departAfter12h = h12
        )
    }

    fun clearDepartAfter() {
        _filterState.value = _filterState.value.copy(
            departAfter24h = null,
            departAfter12h = null
        )
    }

    fun setArriveBefore(hourOfDay: Int, minute: Int) {
        val mins = hourOfDay * 60 + minute
        val h24 = String.format("%02d:%02d", hourOfDay, minute)
        val h12 = TravelTimeEstimator.formatClock(mins)
        _filterState.value = _filterState.value.copy(
            arriveBefore24h = h24,
            arriveBefore12h = h12
        )
    }

    fun clearArriveBefore() {
        _filterState.value = _filterState.value.copy(
            arriveBefore24h = null,
            arriveBefore12h = null
        )
    }

    fun toggleFilterExpanded() {
        _filterState.value = _filterState.value.copy(
            isFilterExpanded = !_filterState.value.isFilterExpanded
        )
    }

    fun clearAllFilters() {
        _filterState.value = ScheduleFilterState(
            isFilterExpanded = _filterState.value.isFilterExpanded
        )
        updateAvailableTerminals()
    }
}
