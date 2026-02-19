package com.example.purchaseregister.utils

import android.content.Context
import android.content.SharedPreferences

object SessionPrefs {
    private const val PREFS_NAME = "session_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(context: Context, email: String, userName: String? = null) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            if (userName != null) {
                putString(KEY_USER_NAME, userName)
            }
        }.apply()
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun getCurrentUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_NAME, null)
    }
}