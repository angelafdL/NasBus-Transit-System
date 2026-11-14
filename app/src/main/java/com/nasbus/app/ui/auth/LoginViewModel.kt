package com.nasbus.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.repository.UserRepository
import com.nasbus.app.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: Long) : LoginUiState()
    data class Error(
        val genericError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null
    ) : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(emailOrUsername: String, password: String) {
        val trimmedIdentifier = emailOrUsername.trim()
        val trimmedPassword = password.trim()

        var hasError = false
        var emailErr: String? = null
        var passwordErr: String? = null

        if (trimmedIdentifier.isEmpty()) {
            emailErr = "Email or username is required"
            hasError = true
        }

        if (trimmedPassword.isEmpty()) {
            passwordErr = "Password is required"
            hasError = true
        } else if (trimmedPassword.length < 8) {
            passwordErr = "Password must be at least 8 characters"
            hasError = true
        }

        if (hasError) {
            _uiState.value = LoginUiState.Error(
                emailError = emailErr,
                passwordError = passwordErr
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = userRepository.login(trimmedIdentifier, trimmedPassword)
            result.onSuccess { user ->
                sessionManager.saveSession(user.userId)
                _uiState.value = LoginUiState.Success(user.userId)
            }.onFailure {
                // Mandatory rule: On login failure show one generic message ("Incorrect email or password")
                _uiState.value = LoginUiState.Error(genericError = "Incorrect email or password")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
