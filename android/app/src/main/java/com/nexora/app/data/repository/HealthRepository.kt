package com.nexora.app.data.repository

import com.nexora.app.data.remote.HealthApi
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.remote.model.HealthResponse

/**
 * Repository interface for system health operations.
 */
interface HealthRepository {
    suspend fun getHealthStatus(): NetworkResult<HealthResponse>
}

/**
 * Default implementation of [HealthRepository].
 */
class HealthRepositoryImpl(
    private val healthApi: HealthApi
) : BaseRepository(), HealthRepository {

    override suspend fun getHealthStatus(): NetworkResult<HealthResponse> {
        return safeApiCall { healthApi.checkHealth() }
    }
}
