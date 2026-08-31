package com.nexora.app.data.remote

import com.nexora.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/logout/")
    suspend fun logout(): Response<DetailResponse>

    @GET("auth/me/")
    suspend fun getCurrentUser(): Response<UserDto>

    @POST("auth/password-reset/")
    suspend fun resetPassword(@Body request: PasswordResetRequest): Response<DetailResponse>
}
