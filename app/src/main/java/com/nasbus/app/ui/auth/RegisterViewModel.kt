package com.nasbus.app.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nasbus.app.data.repository.UserRepository
import com.nasbus.app.util.SessionManager
import com.nasbus.app.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    data class Loading(val submitId: Long) : RegisterUiState()
    data class Success(val submitId: Long, val userId: Long) : RegisterUiState()
    data class Error(
        val submitId: Long,
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

    private var submitCounter: Long = 0

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val submitId = ++submitCounter

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
        } else if (!Validators.isValidUsername(trimmedUsername)) {
            usernameErr = "3 to 20 characters, letters, numbers, and underscores only"
            hasError = true
        }

        if (trimmedEmail.isEmpty()) {
            emailErr = "Email is required"
            hasError = true
        } else if (!Validators.isValidEmail(trimmedEmail)) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }

        if (password.isEmpty()) {
            passwordErr = "Password is required"
            hasError = true
        } else if (!Validators.isValidPassword(password)) {
            passwordErr = "Password must be at least 8 characters"
            hasError = true
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordErr = "Please confirm your password"
            hasError = true
        } else if (!Validators.doPasswordsMatch(password, confirmPassword)) {
            confirmPasswordErr = "Passwords do not match"
            hasError = true
        }

        val isValid = !hasError
        Log.d("Auth", "register tapped, valid=$isValid")

        if (hasError) {
            _uiState.value = RegisterUiState.Error(
                submitId = submitId,
                usernameError = usernameErr,
                emailError = emailErr,
                passwordError = passwordErr,
                confirmPasswordError = confirmPasswordErr
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading(submitId)
            try {
                val result = userRepository.register(trimmedUsername, trimmedEmail, password)
                result.onSuccess { user ->
                    sessionManager.saveSession(user.userId)
                    _uiState.value = RegisterUiState.Success(submitId, user.userId)
                }.onFailure { exception ->
                    val msg = exception.localizedMessage ?: "Registration failed"
                    if (msg.contains("username", ignoreCase = true)) {
                        _uiState.value = RegisterUiState.Error(submitId = submitId, usernameError = msg)
                    } else if (msg.contains("email", ignoreCase = true)) {
                        _uiState.value = RegisterUiState.Error(submitId = submitId, emailError = msg)
                    } else {
                        _uiState.value = RegisterUiState.Error(submitId = submitId, genericError = msg)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(submitId = submitId, genericError = e.localizedMessage ?: "Registration error")
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
