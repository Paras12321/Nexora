package com.nexora.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned by GET /api/health/
 */
@Serializable
data class HealthResponse(
    @SerialName("status") val status: String,
    @SerialName("service") val service: String
)
