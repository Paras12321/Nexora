package com.nexora.app.data.repository

import com.nexora.app.data.model.AiAnalysisRequest
import com.nexora.app.data.model.AiAnalysisResponse
import com.nexora.app.data.model.BillContributor
import com.nexora.app.data.model.DecisionApprovalRequest
import com.nexora.app.data.model.DecisionApprovalResponse
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.model.DetailedBillAnalysis
import com.nexora.app.data.model.NaturalLanguageAiRequest
import com.nexora.app.data.model.NaturalLanguageAiResponse
import com.nexora.app.data.remote.AiApiService
import com.nexora.app.data.remote.NetworkResult

interface AiRepository {
    suspend fun analyzeMessage(homeId: Int?, message: String): NetworkResult<NaturalLanguageAiResponse>
    suspend fun getBillAnalysis(
        homeId: Int,
        billAmount: Double,
        averageBill: Double,
        usageKwh: Double,
        billingPeriod: String
    ): NetworkResult<DetailedBillAnalysis>
    suspend fun getEnergyInsights(homeId: Int, usageKwh: Double): NetworkResult<AiAnalysisResponse>
    suspend fun getAutomationRecommendations(homeId: Int): NetworkResult<AiAnalysisResponse>
    suspend fun getDecisionLogs(homeId: Int): NetworkResult<List<DecisionLogDto>>
    suspend fun approveDecision(homeId: Int, logId: Int): NetworkResult<DecisionApprovalResponse>
    suspend fun rejectDecision(homeId: Int, logId: Int): NetworkResult<DecisionApprovalResponse>
}

class AiRepositoryImpl(
    private val apiService: AiApiService
) : BaseRepository(), AiRepository {

    override suspend fun analyzeMessage(
        homeId: Int?,
        message: String
    ): NetworkResult<NaturalLanguageAiResponse> {
        val request = NaturalLanguageAiRequest(message = message, homeId = homeId)
        return safeApiCall { apiService.analyzeMessage(request) }
    }

    override suspend fun getBillAnalysis(
        homeId: Int,
        billAmount: Double,
        averageBill: Double,
        usageKwh: Double,
        billingPeriod: String
    ): NetworkResult<DetailedBillAnalysis> {
        val context = mapOf(
            "home_id" to homeId.toString(),
            "bill_amount" to billAmount.toString(),
            "average_bill" to averageBill.toString(),
            "usage_kwh" to usageKwh.toString(),
            "billing_period" to billingPeriod
        )
        val request = AiAnalysisRequest(promptType = "bill_analysis", context = context)
        val result = safeApiCall { apiService.getAiLegacyAnalysis(request) }

        return result.map { response ->
            val diffPercent = if (averageBill > 0) {
                ((billAmount - averageBill) / averageBill * 100).toInt()
            } else 0

            val whyText = if (diffPercent > 0) {
                "Bill is $diffPercent% higher than historical average due to increased cooling/appliance usage."
            } else if (diffPercent < 0) {
                "Bill is ${-diffPercent}% lower than historical average thanks to optimized energy conservation."
            } else {
                "Bill is consistent with normal historical monthly usage patterns."
            }

            val contributors = listOf(
                BillContributor("HVAC & Cooling", 42.0, "Heating & Air Conditioning"),
                BillContributor("Kitchen Appliances", 28.0, "Refrigerator, Oven & Microwave"),
                BillContributor("Lighting & Electronics", 15.0, "Smart lights, TVs & Routers"),
                BillContributor("Water Heating & Others", 15.0, "Water heater & standby draw")
            )

            DetailedBillAnalysis(
                summary = "Billing Period: $billingPeriod — Total Amount: $$billAmount ($usageKwh kWh)",
                whyHighLow = whyText,
                contributors = contributors,
                recommendation = "Reduce non-essential cooling and high-wattage appliance usage during peak hours (2 PM – 6 PM).",
                timestamp = "Generated at " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date()),
                explanation = response.content
            )
        }
    }

    override suspend fun getEnergyInsights(
        homeId: Int,
        usageKwh: Double
    ): NetworkResult<AiAnalysisResponse> {
        val context = mapOf(
            "home_id" to homeId.toString(),
            "usage_kwh" to usageKwh.toString()
        )
        val request = AiAnalysisRequest(promptType = "energy_explanation", context = context)
        return safeApiCall { apiService.getAiLegacyAnalysis(request) }
    }

    override suspend fun getAutomationRecommendations(
        homeId: Int
    ): NetworkResult<AiAnalysisResponse> {
        val context = mapOf("home_id" to homeId.toString())
        val request = AiAnalysisRequest(promptType = "automation_recommendation", context = context)
        return safeApiCall { apiService.getAiLegacyAnalysis(request) }
    }

    override suspend fun getDecisionLogs(homeId: Int): NetworkResult<List<DecisionLogDto>> {
        return safeApiCall { apiService.getDecisionLogs(homeId) }
    }

    override suspend fun approveDecision(
        homeId: Int,
        logId: Int
    ): NetworkResult<DecisionApprovalResponse> {
        val request = DecisionApprovalRequest(action = "approve")
        return safeApiCall { apiService.approveDecisionLog(homeId, logId, request) }
    }

    override suspend fun rejectDecision(
        homeId: Int,
        logId: Int
    ): NetworkResult<DecisionApprovalResponse> {
        val request = DecisionApprovalRequest(action = "reject")
        return safeApiCall { apiService.approveDecisionLog(homeId, logId, request) }
    }
}
