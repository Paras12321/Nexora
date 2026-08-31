package com.nexora.app.data.repository

import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.remote.AuthApiService
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.model.*

class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) : BaseRepository(), AuthRepository {

    override suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse> {
        return safeApiCall { apiService.register(request) }.onSuccess { 
            tokenManager.saveToken(it.token)
        }
    }

    override suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> {
        return safeApiCall { apiService.login(request) }.onSuccess {
            tokenManager.saveToken(it.token)
        }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        return safeApiCall { apiService.logout() }.map { 
            tokenManager.clearToken()
            Unit
        }
    }

    override suspend fun resetPassword(email: String): NetworkResult<DetailResponse> {
        return safeApiCall { apiService.resetPassword(PasswordResetRequest(email)) }
    }

    override fun isLoggedIn(): Boolean = tokenManager.getToken() != null
}
