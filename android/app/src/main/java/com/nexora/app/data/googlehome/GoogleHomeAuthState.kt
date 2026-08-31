package com.nexora.app.data.googlehome

sealed class GoogleHomeAuthState {
    data object Unauthenticated : GoogleHomeAuthState()
    data object Authorizing : GoogleHomeAuthState()
    data class Granted(
        val accessToken: String,
        val refreshToken: String? = null,
        val expiresAt: Long = 0
    ) : GoogleHomeAuthState()
    data class Denied(val reason: String) : GoogleHomeAuthState()
    data object Revoked : GoogleHomeAuthState()
}
