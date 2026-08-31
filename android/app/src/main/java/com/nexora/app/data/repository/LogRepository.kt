package com.nexora.app.data.repository

import com.nexora.app.data.model.ActivityLogDto
import com.nexora.app.data.model.ApproveDecisionRequest
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.remote.LogApiService
import com.nexora.app.data.remote.NetworkResult

interface LogRepository {
    suspend fun getActivityLogs(homeId: Int): NetworkResult<List<ActivityLogDto>>
    suspend fun getDecisionLogs(homeId: Int): NetworkResult<List<DecisionLogDto>>
    suspend fun approveDecision(homeId: Int, logId: Int, approve: Boolean): NetworkResult<String>
}

class LogRepositoryImpl(
    private val apiService: LogApiService
) : BaseRepository(), LogRepository {
    override suspend fun getActivityLogs(homeId: Int): NetworkResult<List<ActivityLogDto>> {
        return safeApiCall { apiService.getActivityLogs(homeId) }
    }

    override suspend fun getDecisionLogs(homeId: Int): NetworkResult<List<DecisionLogDto>> {
        return safeApiCall { apiService.getDecisionLogs(homeId) }
    }

    override suspend fun approveDecision(homeId: Int, logId: Int, approve: Boolean): NetworkResult<String> {
        val action = if (approve) "approve" else "reject"
        val result = safeApiCall { apiService.approveDecision(homeId, logId, ApproveDecisionRequest(action)) }
        return result.map { it.detail }
    }
}
