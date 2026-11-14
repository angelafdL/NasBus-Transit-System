package com.nasbus.app.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, NO_USER_ID)
    }

    fun isLoggedIn(): Boolean {
        return getUserId() != NO_USER_ID
    }

    fun clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "nasbus_session_prefs"
        private const val KEY_USER_ID = "logged_in_user_id"
        const val NO_USER_ID = -1L
    }
}
