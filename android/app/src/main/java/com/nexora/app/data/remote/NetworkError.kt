package com.nexora.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard backend error response format.
 */
@Serializable
data class ApiErrorResponse(
    @SerialName("detail") val detail: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("status") val status: String? = null
)

/**
 * Network error hierarchy representing different categories of failures.
 */
sealed class NetworkError : Throwable() {

    /**
     * HTTP error with status code and optional server message or raw response body.
     */
    data class HttpError(
        val statusCode: Int,
        val serverMessage: String?,
        val rawErrorBody: String? = null
    ) : NetworkError() {
        override val message: String
            get() = serverMessage ?: "HTTP Error $statusCode"
    }

    /**
     * Network/IO connection failures (e.g. offline, timeout, unknown host).
     */
    data class ConnectivityError(
        override val cause: Throwable? = null,
        val userFriendlyMessage: String = "Network connection unavailable. Please check your internet connection."
    ) : NetworkError() {
        override val message: String get() = userFriendlyMessage
    }

    /**
     * Serialization / Deserialization issues (e.g. malformed JSON, mismatched fields).
     */
    data class SerializationError(
        override val cause: Throwable? = null,
        val userFriendlyMessage: String = "Failed to parse server response."
    ) : NetworkError() {
        override val message: String get() = userFriendlyMessage
    }

    /**
     * Catch-all for unexpected failures.
     */
    data class UnknownError(
        override val cause: Throwable? = null,
        val userFriendlyMessage: String = "An unexpected error occurred."
    ) : NetworkError() {
        override val message: String get() = userFriendlyMessage
    }
}
