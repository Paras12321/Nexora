package com.nexora.app.data.repository

import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.ApiErrorResponse
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
                        serverMessage = parsedDetail ?: "Server error ($code)",
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
     * Supports both global detail messages and field-specific validation error dictionaries from DRF.
     */
    private fun parseErrorDetail(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val jsonElement = ApiClient.defaultJson.parseToJsonElement(errorBody)
            if (jsonElement is kotlinx.serialization.json.JsonObject) {
                // 1. Check direct detail / message / error keys first
                jsonElement["detail"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) return it.content }
                jsonElement["message"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) return it.content }
                jsonElement["error"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) return it.content }

                // 2. Parse field validation errors (e.g. {"password": ["Error..."], "email": ["..."]})
                val fieldErrors = mutableListOf<String>()
                for ((key, value) in jsonElement) {
                    if (value is kotlinx.serialization.json.JsonArray) {
                        val msgs = value.filterIsInstance<kotlinx.serialization.json.JsonPrimitive>().map { it.content }
                        if (msgs.isNotEmpty()) {
                            val fieldName = key.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            fieldErrors.add("$fieldName: ${msgs.joinToString(", ")}")
                        }
                    } else if (value is kotlinx.serialization.json.JsonPrimitive && value.isString) {
                        val fieldName = key.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        fieldErrors.add("$fieldName: ${value.content}")
                    }
                }
                if (fieldErrors.isNotEmpty()) {
                    return fieldErrors.joinToString("\n")
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
