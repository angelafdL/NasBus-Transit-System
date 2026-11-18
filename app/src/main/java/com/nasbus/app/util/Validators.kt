package com.nasbus.app.util

object Validators {

    private const val USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$"
    private const val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

    fun isValidUsername(username: String): Boolean {
        return Regex(USERNAME_REGEX).matches(username.trim())
    }

    fun isValidEmail(email: String): Boolean {
        return Regex(EMAIL_REGEX).matches(email.trim())
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun doPasswordsMatch(password: String, confirm: String): Boolean {
        return password == confirm
    }
}
