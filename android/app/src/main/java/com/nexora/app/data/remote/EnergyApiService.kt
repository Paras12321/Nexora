package com.nexora.app.data.remote

import com.nexora.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface EnergyApiService {
    @GET("homes/{id}/energy/")
    suspend fun getEnergyUsage(@Path("id") homeId: Int): Response<List<EnergyUsageDto>>

    @GET("homes/{id}/bills/")
    suspend fun getBills(@Path("id") homeId: Int): Response<List<BillDto>>

    @POST("homes/{id}/bills/")
    suspend fun submitBill(@Path("id") homeId: Int, @Body request: CreateBillRequest): Response<BillDto>

    @POST("ai/legacy/analyze/")
    suspend fun getAiAnalysis(@Body request: AiAnalysisRequest): Response<AiAnalysisResponse>
}
