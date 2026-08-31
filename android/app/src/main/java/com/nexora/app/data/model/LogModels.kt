package com.nexora.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityLogDto(
    @SerialName("id") val id: Int,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("actor") val actor: String,
    @SerialName("action") val action: String,
    @SerialName("home_context") val homeContext: String? = null,
    @SerialName("room_context") val roomContext: String? = null,
    @SerialName("device_context") val deviceContext: String? = null,
    @SerialName("result") val result: String
)

@Serializable
data class DecisionLogDto(
    @SerialName("id") val id: Int,
    @SerialName("timestamp") val timestamp: String = "",
    @SerialName("source") val source: String = "system",
    @SerialName("reason") val reason: String = "",
    @SerialName("decision") val decision: String = "",
    @SerialName("proposed_action") val proposedAction: String = "",
    @SerialName("actual_action") val actualAction: String? = null,
    @SerialName("result") val result: String? = null,
    @SerialName("status") val status: String? = "pending_approval",
    @SerialName("room") val roomId: Int? = null,
    @SerialName("device") val deviceId: Int? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null
)

@Serializable
data class ApproveDecisionRequest(
    @SerialName("action") val action: String // approve|reject
)
