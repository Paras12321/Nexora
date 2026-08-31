package com.nexora.app.data.repository

import com.nexora.app.data.model.*
import com.nexora.app.data.remote.EnergyApiService
import com.nexora.app.data.remote.NetworkResult

interface EnergyRepository {
    suspend fun getEnergyUsage(homeId: Int): NetworkResult<List<EnergyUsageDto>>
    suspend fun getBills(homeId: Int): NetworkResult<List<BillDto>>
    suspend fun submitBill(homeId: Int, request: CreateBillRequest): NetworkResult<BillDto>
    suspend fun getBillAnalysis(homeId: Int, bill: BillDto): NetworkResult<AiAnalysisResponse>
}

class EnergyRepositoryImpl(
    private val apiService: EnergyApiService
) : BaseRepository(), EnergyRepository {

    override suspend fun getEnergyUsage(homeId: Int): NetworkResult<List<EnergyUsageDto>> {
        return safeApiCall { apiService.getEnergyUsage(homeId) }
    }

    override suspend fun getBills(homeId: Int): NetworkResult<List<BillDto>> {
        return safeApiCall { apiService.getBills(homeId) }
    }

    override suspend fun submitBill(homeId: Int, request: CreateBillRequest): NetworkResult<BillDto> {
        return safeApiCall { apiService.submitBill(homeId, request) }
    }

    override suspend fun getBillAnalysis(homeId: Int, bill: BillDto): NetworkResult<AiAnalysisResponse> {
        val context = mapOf(
            "home_id" to homeId.toString(),
            "bill_amount" to bill.amount.toString(),
            "usage_kwh" to bill.usageKwh.toString(),
            "billing_period" to "${bill.periodStart} to ${bill.periodEnd}"
        )
        val request = AiAnalysisRequest(promptType = "bill_analysis", context = context)
        return safeApiCall { apiService.getAiAnalysis(request) }
    }
}
