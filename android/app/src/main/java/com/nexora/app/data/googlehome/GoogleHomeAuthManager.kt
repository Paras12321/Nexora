package com.nexora.app.data.googlehome

import android.net.Uri
import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleHomeAuthManager(
    val config: GoogleHomeConfig = GoogleHomeConfig(),
    private val tokenManager: TokenManager? = null
) {
    private val _authState = MutableStateFlow<GoogleHomeAuthState>(GoogleHomeAuthState.Unauthenticated)
    val authState: StateFlow<GoogleHomeAuthState> = _authState.asStateFlow()

    init {
        restoreState()
    }

    fun restoreState() {
        val savedToken = tokenManager?.getToken()
        if (!savedToken.isNullOrBlank() && savedToken.startsWith("gh_access_")) {
            _authState.value = GoogleHomeAuthState.Granted(
                accessToken = savedToken,
                expiresAt = System.currentTimeMillis() + 3600_000
            )
        }
    }

    fun buildAuthorizationUrl(): String {
        _authState.value = GoogleHomeAuthState.Authorizing
        val scopeParam = Uri.encode(config.scopes.joinToString(" "))
        val redirectUriParam = Uri.encode(config.redirectUri)
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=${config.clientId}&" +
                "redirect_uri=$redirectUriParam&" +
                "response_type=code&" +
                "scope=$scopeParam&" +
                "access_type=offline&" +
                "prompt=consent"
    }

    fun handleRedirectUri(uriString: String): NetworkResult<Unit> {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme != config.redirectScheme) {
                val error = NetworkError.HttpError(400, "Invalid redirect scheme: ${uri.scheme}")
                _authState.value = GoogleHomeAuthState.Denied("Invalid redirect scheme")
                return NetworkResult.Error(error)
            }

            val errorParam = uri.getQueryParameter("error")
            if (!errorParam.isNullOrBlank()) {
                val reason = when (errorParam) {
                    "access_denied" -> "User denied Google Home access consent"
                    else -> "OAuth error: $errorParam"
                }
                _authState.value = GoogleHomeAuthState.Denied(reason)
                return NetworkResult.Error(NetworkError.HttpError(403, reason))
            }

            val authCode = uri.getQueryParameter("code")
            if (authCode.isNullOrBlank()) {
                val reason = "Missing authorization code in redirect callback"
                _authState.value = GoogleHomeAuthState.Denied(reason)
                return NetworkResult.Error(NetworkError.HttpError(400, reason))
            }

            // Simulate exchanging authorization code for access token
            val accessToken = "gh_access_${authCode.take(16)}"
            val refreshToken = "gh_refresh_${authCode.take(16)}"
            
            _authState.value = GoogleHomeAuthState.Granted(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = System.currentTimeMillis() + 3600_000
            )
            tokenManager?.saveToken(accessToken)

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            val reason = "Failed to parse OAuth redirect URI: ${e.localizedMessage}"
            _authState.value = GoogleHomeAuthState.Denied(reason)
            NetworkResult.Error(NetworkError.UnknownError(e))
        }
    }

    fun grantPermissionDirectly(accessToken: String = "gh_access_mock_token_123", refreshToken: String? = "gh_refresh_123") {
        _authState.value = GoogleHomeAuthState.Granted(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = System.currentTimeMillis() + 3600_000
        )
        tokenManager?.saveToken(accessToken)
    }

    fun denyPermissionDirectly(reason: String = "Permission denied by user") {
        _authState.value = GoogleHomeAuthState.Denied(reason)
    }

    fun revokePermissions() {
        _authState.value = GoogleHomeAuthState.Revoked
        tokenManager?.clearToken()
    }

    fun isGranted(): Boolean {
        return _authState.value is GoogleHomeAuthState.Granted
    }
}
