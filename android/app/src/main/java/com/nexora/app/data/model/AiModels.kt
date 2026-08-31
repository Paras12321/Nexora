package com.nexora.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NaturalLanguageAiRequest(
    @SerialName("message") val message: String,
    @SerialName("home_id") val homeId: Int? = null
)

@Serializable
data class NaturalLanguageAiResponse(
    @SerialName("message") val message: String = "",
    @SerialName("intent") val intent: String = "informational",
    @SerialName("confidence") val confidence: Double? = null,
    @SerialName("entities") val entities: List<AiEntityDto> = emptyList(),
    @SerialName("proposed_actions") val proposedActions: List<ProposedActionDto> = emptyList(),
    @SerialName("policy_status") val policyStatus: String = "informational",
    @SerialName("requires_confirmation") val requiresConfirmation: Boolean = false,
    @SerialName("provider") val provider: String? = null,
    @SerialName("decision_log_id") val decisionLogId: Int? = null
)

@Serializable
data class AiEntityDto(
    @SerialName("type") val type: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("id") val id: Int? = null
)

@Serializable
data class ProposedActionDto(
    @SerialName("action_type") val actionType: String = "",
    @SerialName("device_id") val deviceId: Int? = null,
    @SerialName("room_id") val roomId: Int? = null,
    @SerialName("reason") val reason: String = ""
)


@Serializable
data class DecisionApprovalRequest(
    @SerialName("action") val action: String // "approve" or "reject"
)

@Serializable
data class DecisionApprovalResponse(
    @SerialName("detail") val detail: String = ""
)

data class BillContributor(
    val category: String,
    val percentage: Double,
    val description: String
)

data class DetailedBillAnalysis(
    val summary: String,
    val whyHighLow: String,
    val contributors: List<BillContributor>,
    val recommendation: String,
    val timestamp: String,
    val explanation: String
)
