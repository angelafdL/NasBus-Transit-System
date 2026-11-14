package com.nasbus.app.ui.auth

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.repository.UserRepository
import com.nasbus.app.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val userId: Long) : RegisterUiState()
    data class Error(
        val usernameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val genericError: String? = null
    ) : RegisterUiState()
}

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()

        var hasError = false
        var usernameErr: String? = null
        var emailErr: String? = null
        var passwordErr: String? = null
        var confirmPasswordErr: String? = null

        if (trimmedUsername.isEmpty()) {
            usernameErr = "Username is required"
            hasError = true
        } else if (trimmedUsername.length < 3) {
            usernameErr = "Username must be at least 3 characters"
            hasError = true
        }

        if (trimmedEmail.isEmpty()) {
            emailErr = "Email is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailErr = "Enter a valid email address"
            hasError = true
        }

        if (password.isEmpty()) {
            passwordErr = "Password is required"
            hasError = true
        } else if (password.length < 8) {
            passwordErr = "Password must be at least 8 characters"
            hasError = true
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordErr = "Please confirm your password"
            hasError = true
        } else if (password != confirmPassword) {
            confirmPasswordErr = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            _uiState.value = RegisterUiState.Error(
                usernameError = usernameErr,
                emailError = emailErr,
                passwordError = passwordErr,
                confirmPasswordError = confirmPasswordErr
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = userRepository.register(trimmedUsername, trimmedEmail, password)
            result.onSuccess { user ->
                sessionManager.saveSession(user.userId)
                _uiState.value = RegisterUiState.Success(user.userId)
            }.onFailure { exception ->
                val msg = exception.localizedMessage ?: "Registration failed"
                if (msg.contains("username", ignoreCase = true)) {
                    _uiState.value = RegisterUiState.Error(usernameError = msg)
                } else if (msg.contains("email", ignoreCase = true)) {
                    _uiState.value = RegisterUiState.Error(emailError = msg)
                } else {
                    _uiState.value = RegisterUiState.Error(genericError = msg)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
