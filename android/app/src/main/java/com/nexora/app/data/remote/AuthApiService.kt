package com.nexora.app.data.remote

import com.nexora.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/password-reset/")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<DetailResponse>
}
