package com.nexora.app.data.remote

import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.session.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that appends the DRF token authentication header to requests.
 * Header format: Authorization: Token <token_value>
 */
class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val sessionRepository: SessionRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Do not override if Authorization header is explicitly provided
        if (originalRequest.header("Authorization") != null) {
            val response = chain.proceed(originalRequest)
            if (response.code == 401) {
                sessionRepository.onSessionExpired()
            }
            return response
        }

        val token = tokenManager.getToken()
        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Token $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)
        if (response.code == 401) {
            sessionRepository.onSessionExpired()
        }
        return response
    }
}
