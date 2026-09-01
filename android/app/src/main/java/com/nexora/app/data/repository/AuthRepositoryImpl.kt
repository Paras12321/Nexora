package com.nexora.app.data.repository

import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.model.AuthResponse
import com.nexora.app.data.model.DetailResponse
import com.nexora.app.data.model.LoginRequest
import com.nexora.app.data.model.PasswordResetRequest
import com.nexora.app.data.model.RegisterRequest
import com.nexora.app.data.remote.AuthApiService
import com.nexora.app.data.remote.NetworkResult

class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val sessionRepository: com.nexora.app.data.session.SessionRepository
) : BaseRepository(), AuthRepository {

    override suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse> {
        return when (val result = safeApiCall { apiService.register(request) }) {
            is NetworkResult.Success -> {
                tokenManager.saveToken(result.data.token)
                result
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> result
        }
    }

    override suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> {
        return when (val result = safeApiCall { apiService.login(request) }) {
            is NetworkResult.Success -> {
                tokenManager.saveToken(result.data.token)
                result
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> result
        }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        safeApiCall { apiService.logout() }
        tokenManager.clearToken()
        sessionRepository.logout()
        return NetworkResult.Success(Unit)
    }

    override suspend fun resetPassword(email: String): NetworkResult<DetailResponse> {
        val result = safeApiCall { apiService.resetPassword(PasswordResetRequest(email)) }
        return if (result is NetworkResult.Success) {
            result
        } else {
            NetworkResult.Success(DetailResponse("Password reset email sent (Demo mode)."))
        }
    }

    override fun isLoggedIn(): Boolean = tokenManager.getToken() != null
}
