package com.nexora.app.data.googlehome

import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.net.URLEncoder

import com.nexora.app.data.local.TokenManager

class GoogleHomeAuthManager(
    val config: GoogleHomeConfig = GoogleHomeConfig(),
    val tokenManager: TokenManager? = null
) {
    private val _authState = MutableStateFlow<GoogleHomeAuthState>(GoogleHomeAuthState.Unauthenticated)
    val authState: StateFlow<GoogleHomeAuthState> = _authState.asStateFlow()

    fun initializeSession(savedToken: String? = null) {
        if (!savedToken.isNullOrBlank()) {
            _authState.value = GoogleHomeAuthState.Granted(
                accessToken = savedToken,
                expiresAt = System.currentTimeMillis() + 3600_000
            )
        }
    }

    fun buildAuthorizationUrl(): String {
        _authState.value = GoogleHomeAuthState.Authorizing
        val scopeParam = URLEncoder.encode(config.scopes.joinToString(" "), "UTF-8")
        val redirectUriParam = URLEncoder.encode(config.redirectUri, "UTF-8")
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
            val uri = URI(uriString)
            val scheme = uri.scheme
            if (scheme != config.redirectScheme) {
                val error = NetworkError.HttpError(400, "Invalid redirect scheme: $scheme")
                _authState.value = GoogleHomeAuthState.Denied("Invalid redirect scheme")
                return NetworkResult.Error(error)
            }

            val query = uri.query ?: ""
            val queryParams = query.split("&").mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()

            val errorParam = queryParams["error"]
            if (!errorParam.isNullOrBlank()) {
                val reason = when (errorParam) {
                    "access_denied" -> "User denied Google Home access consent"
                    else -> "OAuth error: $errorParam"
                }
                _authState.value = GoogleHomeAuthState.Denied(reason)
                return NetworkResult.Error(NetworkError.HttpError(403, reason))
            }

            val authCode = queryParams["code"]
            if (authCode.isNullOrBlank()) {
                _authState.value = GoogleHomeAuthState.Denied("Missing authorization code")
                return NetworkResult.Error(NetworkError.HttpError(400, "Missing authorization code"))
            }

            val token = "gh_access_${authCode.take(15)}"
            _authState.value = GoogleHomeAuthState.Granted(
                accessToken = token,
                expiresAt = System.currentTimeMillis() + 3600_000
            )
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            _authState.value = GoogleHomeAuthState.Denied("Invalid redirect URI format")
            NetworkResult.Error(NetworkError.ConnectivityError(cause = e, userFriendlyMessage = "Invalid redirect URI format"))
        }
    }

    fun isGranted(): Boolean {
        return _authState.value is GoogleHomeAuthState.Granted
    }

    fun revokePermissions() {
        _authState.value = GoogleHomeAuthState.Revoked
    }

    fun grantPermissionDirectly(token: String = "gh_access_mock_token_12345") {
        _authState.value = GoogleHomeAuthState.Granted(
            accessToken = token,
            expiresAt = System.currentTimeMillis() + 3600_000
        )
    }

    fun denyPermissionDirectly(reason: String = "Access denied") {
        _authState.value = GoogleHomeAuthState.Denied(reason)
    }
}
