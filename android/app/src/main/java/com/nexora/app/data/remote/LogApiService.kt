package com.nexora.app.data.remote

import com.nexora.app.data.model.ActivityLogDto
import com.nexora.app.data.model.ApproveDecisionRequest
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.model.DetailResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LogApiService {
    @GET("homes/{home_id}/activity-logs/")
    suspend fun getActivityLogs(@Path("home_id") homeId: Int): Response<List<ActivityLogDto>>

    @GET("homes/{home_id}/decision-logs/")
    suspend fun getDecisionLogs(@Path("home_id") homeId: Int): Response<List<DecisionLogDto>>

    @POST("homes/{home_id}/decision-logs/{log_id}/approve/")
    suspend fun approveDecision(
        @Path("home_id") homeId: Int,
        @Path("log_id") logId: Int,
        @Body request: ApproveDecisionRequest
    ): Response<DetailResponse>
}
