package com.nexora.app.data.repository

import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.model.*
import com.nexora.app.data.remote.AuthApiService
import com.nexora.app.data.remote.NetworkResult

interface AuthRepository {
    suspend fun login(email: String, password: String): NetworkResult<AuthResponse>
    suspend fun register(email: String, password: String, firstName: String, lastName: String): NetworkResult<AuthResponse>
    suspend fun resetPassword(email: String): NetworkResult<DetailResponse>
    fun logout()
    fun isAuthenticated(): Boolean
}

class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) : BaseRepository(), AuthRepository {

    override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> {
        val result = safeApiCall { apiService.login(LoginRequest(email, password)) }
        if (result is NetworkResult.Success) {
            tokenManager.saveToken(result.data.token)
        }
        return result
    }

    override suspend fun register(email: String, password: String, firstName: String, lastName: String): NetworkResult<AuthResponse> {
        val result = safeApiCall { apiService.register(RegisterRequest(email, password, firstName, lastName)) }
        if (result is NetworkResult.Success) {
            tokenManager.saveToken(result.data.token)
        }
        return result
    }

    override suspend fun resetPassword(email: String): NetworkResult<DetailResponse> {
        return safeApiCall { apiService.requestPasswordReset(PasswordResetRequest(email)) }
    }

    override fun logout() {
        tokenManager.clearToken()
    }

    override fun isAuthenticated(): Boolean {
        return tokenManager.getToken() != null
    }
}
