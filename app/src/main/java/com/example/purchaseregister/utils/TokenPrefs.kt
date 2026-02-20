package com.example.purchaseregister.utils

import android.content.Context
import android.content.SharedPreferences

object TokenPrefs {
    private const val PREFS_NAME = "token_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun clearToken(context: Context) {
        getPrefs(context).edit().remove(KEY_AUTH_TOKEN).apply()
    }
}