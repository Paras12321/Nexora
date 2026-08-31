package com.nexora.app.data.session

import com.nexora.app.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionRepository(private val tokenManager: TokenManager) {
    private val _isSessionExpired = MutableStateFlow(false)
    val isSessionExpired: StateFlow<Boolean> = _isSessionExpired.asStateFlow()

    fun onSessionExpired() {
        tokenManager.clearToken()
        _isSessionExpired.value = true
    }

    fun resetSessionExpired() {
        _isSessionExpired.value = false
    }

    fun logout() {
        tokenManager.clearToken()
        _isSessionExpired.value = false
    }
}
