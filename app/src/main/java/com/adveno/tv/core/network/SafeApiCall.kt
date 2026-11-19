package com.adveno.tv.core.network

import retrofit2.Response
import java.io.IOException

/**
 * Executes a Retrofit API call and encapsulates the outcome in a [NetworkResult].
 *
 * Responsibilities:
 * - Parses HTTP 200..299 success bodies.
 * - Handles HTTP 204 No Content (logout) and HTTP 202 Accepted (sync acknowledgment) without payload.
 * - Deserializes backend error responses (both domain errors and FastAPI validation details).
 * - Identifies credential revocation errors (`SCREEN_NOT_FOUND`, `SCREEN_DISCONNECTED`, `INVALID_SCREEN_CREDENTIAL`).
 * - Catches I/O exceptions, timeouts, and network unavailability.
 */
suspend inline fun <T> safeApiCall(
    crossinline apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = apiCall()
        val body = response.body()

        if (response.isSuccessful) {
            when {
                body != null -> NetworkResult.Success(body)
                response.code() == 204 || response.code() == 202 -> {
                    @Suppress("UNCHECKED_CAST")
                    NetworkResult.Success(Unit as T)
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    NetworkResult.Success(null as T)
                }
            }
        } else {
            val errorBodyString = response.errorBody()?.string()
            val parsedError = errorBodyString?.let {
                try {
                    ApiClient.json.decodeFromString<ApiErrorResponse>(it)
                } catch (_: Exception) {
                    null
                }
            }

            val appErrorCode = parsedError?.appError?.code
            val appErrorMessage = parsedError?.appError?.message
                ?: "Request failed with HTTP status ${response.code()}"

            if (appErrorCode == "SCREEN_NOT_FOUND" ||
                appErrorCode == "SCREEN_DISCONNECTED" ||
                appErrorCode == "INVALID_SCREEN_CREDENTIAL"
            ) {
                NetworkResult.Error(
                    NetworkError.DeviceRevoked(
                        errorCode = appErrorCode,
                        message = appErrorMessage
                    )
                )
            } else {
                NetworkResult.Error(
                    NetworkError.HttpError(
                        httpCode = response.code(),
                        message = appErrorMessage,
                        errorCode = appErrorCode,
                        errorResponse = parsedError
                    )
                )
            }
        }
    } catch (e: IOException) {
        NetworkResult.Error(NetworkError.NetworkFailure(e))
    } catch (e: Exception) {
        NetworkResult.Error(
            NetworkError.Unknown(e.localizedMessage ?: "An unexpected error occurred during API request")
        )
    }
}

