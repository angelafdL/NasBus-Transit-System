package com.nasbus.app.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.local.entity.UserEntity
import com.nasbus.app.data.repository.UserRepository
import com.nasbus.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AccountUiState {
    object Loading : AccountUiState()
    data class Success(val user: UserEntity) : AccountUiState()
    object LoggedOut : AccountUiState()
}

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun loadUserData() {
        val userId = sessionManager.getUserId()
        if (userId == -1L) {
            _uiState.value = AccountUiState.LoggedOut
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUserById(userId).collect { user ->
                if (user != null) {
                    _uiState.value = AccountUiState.Success(user)
                } else {
                    _uiState.value = AccountUiState.LoggedOut
                }
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _uiState.value = AccountUiState.LoggedOut
    }
}
