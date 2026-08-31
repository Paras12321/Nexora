package com.nexora.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnergyUsageDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("usage_kwh") val usageKwh: Double
)

@Serializable
data class BillDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("billing_period_start") val periodStart: String,
    @SerialName("billing_period_end") val periodEnd: String,
    @SerialName("amount") val amount: Double,
    @SerialName("usage_kwh") val usageKwh: Double,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateBillRequest(
    @SerialName("billing_period_start") val periodStart: String,
    @SerialName("billing_period_end") val periodEnd: String,
    @SerialName("amount") val amount: Double,
    @SerialName("usage_kwh") val usageKwh: Double
)

@Serializable
data class AiAnalysisRequest(
    @SerialName("prompt_type") val promptType: String,
    @SerialName("context") val context: Map<String, String>
)

@Serializable
data class AiAnalysisResponse(
    @SerialName("status") val status: String,
    @SerialName("content") val content: String,
    @SerialName("decision") val decision: String? = null,
    @SerialName("requires_approval") val requiresApproval: Boolean = false,
    @SerialName("confidence") val confidence: Double? = null
)
