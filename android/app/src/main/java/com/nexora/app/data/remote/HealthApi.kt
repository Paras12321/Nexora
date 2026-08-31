package com.nexora.app.data.remote

import com.nexora.app.data.remote.model.HealthResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit API interface for backend availability / health check.
 */
interface HealthApi {

    @GET("health/")
    suspend fun checkHealth(): Response<HealthResponse>
}
