package com.nexora.app.data.remote

import com.nexora.app.data.model.AiAnalysisRequest
import com.nexora.app.data.model.AiAnalysisResponse
import com.nexora.app.data.model.DecisionApprovalRequest
import com.nexora.app.data.model.DecisionApprovalResponse
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.model.NaturalLanguageAiRequest
import com.nexora.app.data.model.NaturalLanguageAiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AiApiService {

    @POST("ai/analyze/")
    suspend fun analyzeMessage(
        @Body request: NaturalLanguageAiRequest
    ): Response<NaturalLanguageAiResponse>

    @POST("ai/legacy/analyze/")
    suspend fun getAiLegacyAnalysis(
        @Body request: AiAnalysisRequest
    ): Response<AiAnalysisResponse>

    @GET("homes/{homeId}/decision-logs/")
    suspend fun getDecisionLogs(
        @Path("homeId") homeId: Int
    ): Response<List<DecisionLogDto>>

    @POST("homes/{homeId}/decision-logs/{logId}/approve/")
    suspend fun approveDecisionLog(
        @Path("homeId") homeId: Int,
        @Path("logId") logId: Int,
        @Body request: DecisionApprovalRequest
    ): Response<DecisionApprovalResponse>
}
