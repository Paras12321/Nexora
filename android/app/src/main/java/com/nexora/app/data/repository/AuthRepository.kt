package com.nexora.app.data.repository

import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.model.*

interface AuthRepository {
    suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse>
    suspend fun login(request: LoginRequest): NetworkResult<AuthResponse>
    suspend fun logout(): NetworkResult<Unit>
    suspend fun resetPassword(email: String): NetworkResult<DetailResponse>
    fun isLoggedIn(): Boolean
}
