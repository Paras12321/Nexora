package com.nexora.app.data.repository

import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException

/**
 * Base Repository class providing generic exception handling and Retrofit response wrapping.
 */
abstract class BaseRepository {

    /**
     * Executes an API call safely, capturing network/deserialization exceptions and mapping
     * HTTP error codes and bodies into structured [NetworkResult].
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): NetworkResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else if (response.code() == 204) {
                    @Suppress("UNCHECKED_CAST")
                    NetworkResult.Success(Unit as T)
                } else {
                    NetworkResult.Error(
                        NetworkError.SerializationError(
                            userFriendlyMessage = "Empty response body received from server."
                        )
                    )
                }
            } else {
                val code = response.code()
                val rawErrorBody = response.errorBody()?.string()
                val parsedDetail = parseErrorDetail(rawErrorBody)
                NetworkResult.Error(
                    NetworkError.HttpError(
                        statusCode = code,
                        serverMessage = parsedDetail,
                        rawErrorBody = rawErrorBody
                    )
                )
            }
        } catch (e: IOException) {
            println("DEBUG_EXCEPTION: IOException: ${e.message}")
            return NetworkResult.Error(
                NetworkError.ConnectivityError(
                    cause = e,
                    userFriendlyMessage = "Unable to connect to server. Please check if backend server is running."
                )
            )
        } catch (e: SerializationException) {
            println("DEBUG_EXCEPTION: SerializationException: ${e.message}")
            return NetworkResult.Error(
                NetworkError.SerializationError(
                    cause = e,
                    userFriendlyMessage = "Failed to parse server response."
                )
            )
        } catch (e: Throwable) {
            println("DEBUG_EXCEPTION: ${e.javaClass.name}: ${e.message}")
            return NetworkResult.Error(
                NetworkError.UnknownError(
                    cause = e,
                    userFriendlyMessage = e.message ?: "An unexpected error occurred."
                )
            )
        }
    }

    /**
     * Attempts to parse backend error payload JSON into standard error message string.
     * Supports DRF, FastAPI, Express, Spring, Laravel, and custom backend error formats.
     */
    private fun parseErrorDetail(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        val trimmed = errorBody.trim()
        return try {
            val jsonElement = ApiClient.defaultJson.parseToJsonElement(trimmed)
            val parsed = parseJsonErrorElement(jsonElement)
            if (!parsed.isNullOrBlank()) {
                parsed
            } else if (!trimmed.startsWith("<") && !trimmed.startsWith("<!DOCTYPE", ignoreCase = true)) {
                trimmed
            } else null
        } catch (_: Exception) {
            // Fallback for non-JSON plain text error bodies (excluding HTML pages)
            if (!trimmed.startsWith("<") && !trimmed.startsWith("<!DOCTYPE", ignoreCase = true)) {
                trimmed
            } else null
        }
    }

    private fun parseJsonErrorElement(jsonElement: kotlinx.serialization.json.JsonElement): String? {
        return when (jsonElement) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (jsonElement.isString && jsonElement.content.isNotBlank()) {
                    jsonElement.content
                } else null
            }
            is kotlinx.serialization.json.JsonObject -> {
                // Check FastAPI / Pydantic validation error schema: {"loc": [...], "msg": "..."}
                if (jsonElement.containsKey("loc") && jsonElement.containsKey("msg")) {
                    val locArray = (jsonElement["loc"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull {
                        (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                    }
                    val msg = (jsonElement["msg"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    if (!msg.isNullOrBlank()) {
                        val field = locArray?.lastOrNull { it != "body" && it != "query" && it != "path" }
                        return if (!field.isNullOrBlank()) {
                            "${formatFieldName(field)}: $msg"
                        } else {
                            msg
                        }
                    }
                }

                // Check Spring Boot field validation error schema: {"field": "...", "defaultMessage": "..."}
                if (jsonElement.containsKey("field") && (jsonElement.containsKey("defaultMessage") || jsonElement.containsKey("message"))) {
                    val field = (jsonElement["field"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    val msg = (jsonElement["defaultMessage"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                        ?: (jsonElement["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    if (!field.isNullOrBlank() && !msg.isNullOrBlank()) {
                        return "${formatFieldName(field)}: $msg"
                    }
                }

                // 1. Check known top-level error keys
                val directKeys = listOf("detail", "details", "message", "messages", "error", "errors", "non_field_errors", "msg", "reason", "error_description")
                for (key in directKeys) {
                    val value = jsonElement[key] ?: continue
                    val extracted = parseJsonErrorElement(value)
                    if (!extracted.isNullOrBlank()) return extracted
                }

                // 2. Parse field-specific validation error dictionaries (e.g. {"email": ["Already in use"], "password": ["Too short"]})
                val fieldErrors = mutableListOf<String>()
                for ((key, value) in jsonElement) {
                    if (key in directKeys) continue
                    val fieldMsg = when (value) {
                        is kotlinx.serialization.json.JsonPrimitive -> {
                            if (value.isString && value.content.isNotBlank()) value.content else null
                        }
                        is kotlinx.serialization.json.JsonArray -> {
                            val msgs = value.mapNotNull { parseJsonErrorElement(it) }
                            if (msgs.isNotEmpty()) msgs.joinToString(", ") else null
                        }
                        is kotlinx.serialization.json.JsonObject -> {
                            parseJsonErrorElement(value)
                        }
                    }
                    if (!fieldMsg.isNullOrBlank()) {
                        val fieldName = formatFieldName(key)
                        fieldErrors.add("$fieldName: $fieldMsg")
                    }
                }
                if (fieldErrors.isNotEmpty()) {
                    return fieldErrors.joinToString("\n")
                }
                null
            }
            is kotlinx.serialization.json.JsonArray -> {
                val msgs = jsonElement.mapNotNull { parseJsonErrorElement(it) }.filter { it.isNotBlank() }
                if (msgs.isNotEmpty()) msgs.joinToString(", ") else null
            }
        }
    }

    private fun formatFieldName(key: String): String {
        return key.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
