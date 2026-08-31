package com.nexora.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresenceEventDto(
    @SerialName("id") val id: Int,
    @SerialName("state") val state: String, // home, away, unknown
    @SerialName("source") val source: String,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class CreatePresenceRequest(
    @SerialName("state") val state: String,
    @SerialName("source") val source: String = "user"
)

@Serializable
data class SecurityEventDto(
    @SerialName("id") val id: Int,
    @SerialName("mode") val mode: String, // disarmed, armed_home, armed_away
    @SerialName("source") val source: String,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class ChangeSecurityModeRequest(
    @SerialName("mode") val mode: String,
    @SerialName("source") val source: String = "user"
)
