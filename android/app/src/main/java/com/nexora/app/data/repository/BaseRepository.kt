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
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SerializationError(cause = e))
        } catch (e: IOException) {
            NetworkResult.Error(NetworkError.ConnectivityError(cause = e))
        } catch (e: Exception) {
            NetworkResult.Error(NetworkError.UnknownError(cause = e))
        }
    }

    /**
     * Attempts to parse backend error payload JSON into standard error message string.
     */
    private fun parseErrorDetail(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val errorResponse = ApiClient.defaultJson.decodeFromString<ApiErrorResponse>(errorBody)
            errorResponse.detail ?: errorResponse.message ?: errorResponse.error
        } catch (_: Exception) {
            null
        }
    }
}
