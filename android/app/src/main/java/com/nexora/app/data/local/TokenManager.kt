package com.nexora.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Interface for token storage abstraction.
 */
interface TokenManager {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}

/**
 * Android SharedPreferences backed TokenManager for persistent token storage.
 */
class SharedPreferencesTokenManager(context: Context) : TokenManager {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "nexora_auth_prefs"
        private const val KEY_TOKEN = "auth_token"
    }
}

/**
 * In-Memory TokenManager useful for unit testing or temporary sessions.
 */
class InMemoryTokenManager(initialToken: String? = null) : TokenManager {
    private var token: String? = initialToken

    override fun getToken(): String? = token

    override fun saveToken(token: String) {
        this.token = token
    }

    override fun clearToken() {
        this.token = null
    }
}
