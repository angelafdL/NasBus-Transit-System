package com.nasbus.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.model.RouteListItem
import com.nasbus.app.data.repository.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouteRepository(application)

    private val _routes = MutableStateFlow<List<RouteListItem>>(emptyList())
    val routes: StateFlow<List<RouteListItem>> = _routes.asStateFlow()

    init {
        loadRoutes()
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            repository.getAllRouteListItems().collect { list ->
                _routes.value = list
            }
        }
    }
}
