package com.example.purchaseregister.utils

import android.content.Context
import android.util.Base64

object SunatPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_RUC = "sunat_ruc"
    private const val KEY_USER = "sunat_usuario"
    private const val KEY_SOL_PASSWORD = "sunat_clave_sol"
    private const val KEY_CLIENT_ID = "sunat_client_id"
    private const val KEY_CLIENT_SECRET = "sunat_client_secret"

    fun saveSolPassword(context: Context, solPassword: String) {
        try {
            val encrypted = Base64.encodeToString(solPassword.toByteArray(), Base64.NO_WRAP)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SOL_PASSWORD, encrypted).apply()
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SOL_PASSWORD, solPassword).apply()
        }
    }

    fun getSolPassword(context: Context): String? {
        val encrypted = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SOL_PASSWORD, null)

        return encrypted?.let {
            try {
                String(Base64.decode(it, Base64.NO_WRAP))
            } catch (e: Exception) {
                it
            }
        }
    }

    fun saveRuc(context: Context, ruc: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_RUC, ruc).apply()
    }

    fun saveUser(context: Context, user: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER, user).apply()
    }

    fun getRuc(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RUC, null)
    }

    fun getUser(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)
    }

    fun saveClientId(context: Context, clientId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CLIENT_ID, clientId).apply()
    }

    fun getClientId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CLIENT_ID, null)
    }

    fun saveClientSecret(context: Context, clientSecret: String) {
        try {
            val encrypted = Base64.encodeToString(clientSecret.toByteArray(), Base64.NO_WRAP)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CLIENT_SECRET, encrypted).apply()
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CLIENT_SECRET, clientSecret).apply()
        }
    }

    fun getClientSecret(context: Context): String? {
        val encrypted = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CLIENT_SECRET, null)

        return encrypted?.let {
            try {
                String(Base64.decode(it, Base64.NO_WRAP))
            } catch (e: Exception) {
                it
            }
        }
    }

    fun clearCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}